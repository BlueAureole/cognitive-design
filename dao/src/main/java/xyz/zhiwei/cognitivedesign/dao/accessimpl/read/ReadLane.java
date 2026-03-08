package xyz.zhiwei.cognitivedesign.dao.accessimpl.read;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.zhiwei.cognitivedesign.dao.Dao;
import xyz.zhiwei.cognitivedesign.dao.accessimpl.CustomReadThreadPool;
import xyz.zhiwei.cognitivedesign.dao.accessimpl.DaoBeanCache;
import xyz.zhiwei.cognitivedesign.dao.accessimpl.TimeOutConfig;
import xyz.zhiwei.cognitivedesign.morphism.Principle;
import xyz.zhiwei.cognitivedesign.morphism.principle.source.container.PrincipleSource;
import xyz.zhiwei.cognitivedesign.morphism.principle.source.container.PrincipleSourceLane;
import xyz.zhiwei.cognitivedesign.morphism.principle.source.qualifier.PrincipleQualifier;
import xyz.zhiwei.cognitivedesign.morphism.principle.source.qualifier.PrincipleQualifiers;
import xyz.zhiwei.cognitivedesign.morphism.principle.source.qualifier.QualifiersLane;


/**
 * 本原集存取实现
 */
public class ReadLane {
    private static final Logger log = LoggerFactory.getLogger(ReadAccess.class);
    private Integer TIMEOUT=TimeOutConfig.TIME_OUT_READ_UNIT;


    private DaoBeanCache daoBeanCache;
    private Executor daoReadExecutor;
    
    
    public ReadLane(DaoBeanCache daoBeanCache,Executor daoReadExecutor) {
    	this.daoBeanCache=daoBeanCache;
    	this.daoReadExecutor=daoReadExecutor;
    }

    
    
	/*
	 * ================================================= 泳道 ===============================================================
	 */
    
    /**
     * 读取一个泳道的数据
     * 一个泳道分为若干批次
     * @param qualifiersLane
     * @param laneIndex 泳道序号
     * @return 
     */
    public PrincipleSourceLane queryLane(QualifiersLane qualifiersLane, int laneIndex){

    	PrincipleSourceLane relatedCollectionList=new PrincipleSourceLane();
		if(null ==qualifiersLane || qualifiersLane.isEmpty()) {
			return relatedCollectionList;
		}

        log.info("============= 第{}号泳道 查询开始 =============", laneIndex);
        
		for (int i = 0; i < qualifiersLane.size(); i++) {
		    Function<List<PrincipleSource>, PrincipleQualifiers> qualifierFunction = qualifiersLane.get(i);
			PrincipleQualifiers qualifierGroup=qualifierFunction.apply(relatedCollectionList);
			ReadAccessLog.qualifiers(laneIndex, i, qualifierGroup);
			PrincipleSource relatedCollection=queryBatch(qualifierGroup);
			ReadAccessLog.source(laneIndex, i, relatedCollection);
			relatedCollectionList.add(relatedCollection);
		}
		
        log.info("============= 第{}号泳道 查询结束 =============", laneIndex);
		return relatedCollectionList;
    }
    

	/*
	 * ================================================= 批次 ===============================================================
	 */

	
	/**
	 * 查询一个批次
	 * @param <P>
	 * @param qualifierGroup
	 * @return  结果容器List必不为null，至少返回空列表[]。
	 *   
	 */
    private PrincipleSource queryBatch(PrincipleQualifiers qualifierGroup) {

        if (null == qualifierGroup || qualifierGroup.isEmpty() || !qualifierGroup.stream().anyMatch(Objects::nonNull)) {
            return new PrincipleSource();
        }

        int size = qualifierGroup.size();
        // 原子数组存储结果（保证线程安全+索引对应）
        AtomicReferenceArray<List<? extends Principle<?>>> resultArray = new AtomicReferenceArray<>(size);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            int index = i; 
            PrincipleQualifier<?> qualifier = qualifierGroup.get(index);
            
            Executor executorToUse = this.daoReadExecutor;
            if (qualifier != null) {
                 Dao<?> dao = daoBeanCache.getDaoBeanByPrincipleClass(qualifier.getPrincipleClazz());
                 if (dao instanceof CustomReadThreadPool) {
                     Executor customExecutor = ((CustomReadThreadPool) dao).getReadExecutor();
                     if (customExecutor != null) {
                         executorToUse = customExecutor;
                     }
                 }
            }
            
            CompletableFuture<Void> future = CompletableFuture.supplyAsync(
                () -> queryUnit(qualifierGroup.get(index), index), 
                executorToUse
            ).thenAccept(list -> resultArray.set(index, list));
            
            futures.add(future);
        }

        // 等待所有任务完成（含超时控制）
        try {
            // 超时仅终止未完成任务，已完成任务结果保留
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).get(TIMEOUT, TimeUnit.SECONDS);
        } catch (Exception e) {
            // 超时/中断时：仅取消未完成任务，已完成任务结果保留
            log.warn("查询任务超时/被中断，取消未完成任务", e);
            for (int i = 0; i < futures.size(); i++) {
                CompletableFuture<Void> future = futures.get(i);
                // 仅取消未完成的任务，并为其设置空结果
                if (!future.isDone()) {
                    future.cancel(true);
                    resultArray.set(i, new ArrayList<>()); // 超时/中断任务兜底空列表
                }
            }
        }

        // 按索引顺序组装结果（保证与入参顺序一致）
        PrincipleSource result = new PrincipleSource();
        for (int i = 0; i < size; i++) {
            // 兜底：防止极端情况（如任务未执行）数组值为null
            List<? extends Principle<?>> list = Optional.ofNullable(resultArray.get(i)).orElse(new ArrayList<>());
            result.add(list);
        }

        return result;
    }

	/*
	 * ================================================= 单元 ===============================================================
	 */

    /**
     * 执行单个查询任务
     * @param qualifier 单个查询条件
     * @return 查询结果列表，发生异常时返回空列表
     */
    private <P extends Principle<?>> List<P> queryUnit(PrincipleQualifier<P> qualifier, int index) {
        try {
            List<P> list;
            
            // 空值兜底逻辑（原有逻辑保留）
            if (null == qualifier) {
                list = new ArrayList<>();
            } else {
                Dao<P> relatedDao = daoBeanCache.getDaoBeanByPrincipleClass(qualifier.getPrincipleClazz());
                list = relatedDao.subCollection(qualifier);
                // DAO返回null时兜底为空列表
                list = Optional.ofNullable(list).orElse(new ArrayList<>());
            }
            return list;
        } catch (Exception e) {
            // 核心：异常时仅记录日志，返回空结果，不影响其他任务
            log.error("第{}个查询任务执行异常，已设置空结果", index, e);
            return new ArrayList<>(); // 异常兜底空列表
        }
    }
    
}
