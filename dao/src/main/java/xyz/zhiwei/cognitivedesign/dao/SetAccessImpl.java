package xyz.zhiwei.cognitivedesign.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import xyz.zhiwei.cognitivedesign.dao.utils.TransactionGroupExecutorManager;
import xyz.zhiwei.cognitivedesign.morphism.Principle;
import xyz.zhiwei.cognitivedesign.morphism.support.image.PrincipleImage;
import xyz.zhiwei.cognitivedesign.morphism.support.image.PrincipleImageResponse;
import xyz.zhiwei.cognitivedesign.morphism.support.image.PrincipleImagery;
import xyz.zhiwei.cognitivedesign.morphism.support.image.TransactionGroupList;
import xyz.zhiwei.cognitivedesign.morphism.support.qualifier.PrincipleQualifiers;
import xyz.zhiwei.cognitivedesign.morphism.support.source.PrincipleSource;
import xyz.zhiwei.cognitivedesign.service.SetAccessInterface;


/**
 * 本原集存取实现
 */
public class SetAccessImpl implements SetAccessInterface{
    private static final Logger log = LoggerFactory.getLogger(SetAccessImpl.class);
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SetAccessSupport setAccessSupport; 
    
    @Autowired
    private PlatformTransactionManager transactionManager;

    // 事务定义（默认配置：只读=false，隔离级别=DEFAULT，传播行为=REQUIRED）
    private final DefaultTransactionDefinition defaultTxDef = new DefaultTransactionDefinition();
    
	/*
	 * =================================================接口方法区===============================================================
	 */
    
    /**
     * 读取指定数据集
     * 根据子集描述符组的定义函数列表，返回子集描述符组列表所描述的数据集
     * @param qualifierFunctionList
     * @return List<RelatedCollection>
     *    结果容器List必不为null，至少返回空列表[]。
     */
    @Override
    public List<PrincipleSource> queryRelatedCollectionList(
    		List<Function<List<PrincipleSource>,PrincipleQualifiers>> qualifierFunctionList){

		List<PrincipleQualifiers> qualifierGroupList=new ArrayList<>();
		List<PrincipleSource> relatedCollectionList=new ArrayList<>();
		
		if(null!=qualifierFunctionList && !qualifierFunctionList.isEmpty()) {
			for (int i = 0; i < qualifierFunctionList.size(); i++) {
			    Function<List<PrincipleSource>, PrincipleQualifiers> qualifierFunction = qualifierFunctionList.get(i);
				PrincipleQualifiers qualifierGroup=qualifierFunction.apply(relatedCollectionList);
				qualifierGroupList.add(qualifierGroup);
				PrincipleSource relatedCollection=setAccessSupport.queryRelatedCollection(qualifierGroup);
				relatedCollectionList.add(relatedCollection);
			}
		}
		//查询完成后，统一打印查询结果
		log.info("PrincipleSourceQualifiersList is: {}",toJson(qualifierGroupList));
		log.info("PrincipleSourceList is: {}",toJson(relatedCollectionList));
		return relatedCollectionList;
    }
    
    
    
    
    
	/**
	 * 存储相关数据集
	 * 根据拆解函数列表，返回所有拆解目标的保存结果
	 * @param <P>
	 * @param deconstructFunctionList 拆解函数列表，每个函数负责将上一个函数的结果拆解为一个数据集
     * @param transactionGroupSupplier 事务组提供者，用于提供当前拆解函数所在的事务组
	 * @return
	 */
	@Override
	public List<PrincipleImageResponse> saveRelatedSegmentsList(
			List<Function<List<PrincipleImageResponse>, PrincipleImage>> deconstructFunctionList,Supplier<TransactionGroupList> transactionGroupSupplier) {
		
		List<PrincipleImageResponse> saveResponseList=new ArrayList<>();
	    if (null == deconstructFunctionList || deconstructFunctionList.isEmpty()) {
	        return saveResponseList;
	    }
		

	    // 1. 获取事务组配置
	    TransactionGroupList transactionGroupList = transactionGroupSupplier.get();
	    int groupCount = transactionGroupList.size();

	    // 2. 为每个事务组创建：独立事务 + 专属线程池
	    Map<Integer, TransactionStatus> transactionMap = new HashMap<>();
	    Map<Integer, ThreadPoolTaskExecutor> groupExecutorMap = new HashMap<>(); // 事务组-线程池映射
	    Map<Integer, Boolean> groupResultMap = new HashMap<>();
    	TransactionGroupExecutorManager groupExecutorManager=new TransactionGroupExecutorManager();
	    
	    for (int i = 0; i < groupCount; i++) {
	        // 2.1 创建事务组专属线程池
	        ThreadPoolTaskExecutor groupExecutor = groupExecutorManager.getOrCreateGroupExecutor(i);
	        groupExecutorMap.put(i, groupExecutor);
	        final int inx=i;
	        // 2.2 在专属线程中创建独立事务（保证事务上下文隔离）
	        CountDownLatch latch = new CountDownLatch(1);
	        AtomicReference<TransactionStatus> txStatusRef = new AtomicReference<>();
	        groupExecutor.submit(() -> {
	            try {
	                // 该线程中创建的事务，仅归属该线程（独立于其他事务组）
	                TransactionStatus txStatus = transactionManager.getTransaction(defaultTxDef);
	                txStatusRef.set(txStatus);
	                log.info("事务组{}在专属线程{}中创建独立事务：{}", 
	                		inx, Thread.currentThread().threadId(), txStatus);
	            } finally {
	                latch.countDown();
	            }
	        });
	        
	        // 等待事务创建完成
	        try {
				latch.await(10, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        transactionMap.put(i, txStatusRef.get());
	        groupResultMap.put(i, true);
	    }
	    
		
		
	    try {
	        // 3. 循环处理数据（依赖前一次结果）
	        for (int i = 0; i < deconstructFunctionList.size(); i++) {
	            Function<List<PrincipleImageResponse>, PrincipleImage> deconstructFunction = deconstructFunctionList.get(i);
	            PrincipleImage relatedSegments = deconstructFunction.apply(saveResponseList);
	            printPrincipleImage(relatedSegments);

	            // 4. 构建事务组映射（数据项→事务组）
	            Map<Integer, Integer> transactionGroupMap = transactionObjIdentityHashes(transactionGroupList);

	            // 5. 执行存储：根据数据项所属事务组，找到专属线程池执行
	            PrincipleImageResponse saveResponse = setAccessSupport.saveRelatedSegments(
	                    relatedSegments, 
	                    transactionGroupMap, 
	                    transactionMap, 
	                    groupExecutorMap); // 传递线程池映射

	            // 6. 检查结果，标记失败的事务组
	            checkSingleBatchResult(saveResponse, transactionGroupMap, relatedSegments, groupResultMap);

	            log.info("PrincipleImageResponseList {} is: {}", i, toJson(saveResponse));
	            saveResponseList.add(saveResponse);
	        }

	        // 7. 按事务组提交/回滚，并关闭线程池
	        commitOrRollbackAndShutdown(transactionMap, groupResultMap, groupExecutorMap,groupExecutorManager);

	        // 兜底关闭所有线程池（防止泄漏）
	        groupExecutorManager.shutdownAll();
	        
	        return saveResponseList;

	    } catch (Exception e) {
	        // 全局异常：回滚所有事务+关闭线程池（使用当前实例）
	        transactionMap.forEach((k, v) -> transactionManager.rollback(v));
	        groupExecutorManager.shutdownAll(); // 关闭当前实例的所有线程池
	        log.error("处理数据失败，所有事务组回滚+线程池关闭", e);
	        throw new RuntimeException("保存相关数据集失败", e);
	    }
	}
    
	
	/*
	 * ==================================  事务支持区  ==============================
	 */

	/**
	 * 获取事务组对象的唯一标识
	 * @param transactionGroupList
	 * @return
	 */
	private Map<Integer,Integer> transactionObjIdentityHashes(TransactionGroupList transactionGroupList){
		Map<Integer,Integer> map=new HashMap<>();
		for (int i = 0; i < transactionGroupList.size(); i++) {
			List<List<? extends Principle<?>>> oneGroup=transactionGroupList.get(i);
			for (List<? extends Principle<?>> oneData : oneGroup) {
				if(null!=oneData && oneData.size()>0) {}
		        int identityHash = System.identityHashCode(oneData);
		        map.put(identityHash, i);
			}
		}
		return map;
	}
	

	/**
	 * 提交/回滚事务组，并关闭专属线程池
	 */
	private void commitOrRollbackAndShutdown(Map<Integer, TransactionStatus> transactionMap,
	                                         Map<Integer, Boolean> groupResultMap,
	                                         Map<Integer, ThreadPoolTaskExecutor> groupExecutorMap,
	                                         TransactionGroupExecutorManager groupExecutorManager) {
	    for (Map.Entry<Integer, TransactionStatus> entry : transactionMap.entrySet()) {
	        int groupNum = entry.getKey();
	        TransactionStatus txStatus = entry.getValue();
	        ThreadPoolTaskExecutor executor = groupExecutorMap.get(groupNum);
	
	        try {
	            if (groupResultMap.get(groupNum)) {
	                // 在专属线程中提交事务（保证事务上下文一致）
	                CountDownLatch commitLatch = new CountDownLatch(1);
	                executor.submit(() -> {
	                    try {
	                        transactionManager.commit(txStatus);
	                        log.info("事务组{}在专属线程{}中提交事务", 
	                                 groupNum, Thread.currentThread().threadId());
	                    } finally {
	                        commitLatch.countDown();
	                    }
	                });
	                commitLatch.await(10, TimeUnit.SECONDS);
	            } else {
	                // 在专属线程中回滚事务
	                CountDownLatch rollbackLatch = new CountDownLatch(1);
	                executor.submit(() -> {
	                    try {
	                        transactionManager.rollback(txStatus);
	                        log.error("事务组{}在专属线程{}中回滚事务", 
	                                  groupNum, Thread.currentThread().threadId());
	                    } finally {
	                        rollbackLatch.countDown();
	                    }
	                });
	                rollbackLatch.await(10, TimeUnit.SECONDS);
	            }
	        } catch (Exception e) {
	            log.error("处理事务组{}失败", groupNum, e);
	            transactionManager.rollback(txStatus);
	        } finally {
	            // 关闭该事务组的专属线程池
	            groupExecutorManager.shutdownGroupExecutor(groupNum);
	        }
	    }
	}
    
	
	
	/**
	 * 检查单批次执行结果，标记失败的事务组
	 * @param saveResponse 单批次保存结果（包含新增/修改/删除的执行计数）
	 * @param transactionGroupMap 数据项hashCode → 事务组编号的映射
	 * @param relatedSegments 当前批次的原始数据（包含新增/修改/删除的子列表）
	 * @param groupResultMap 事务组执行结果标记（key=事务组编号，value=是否成功）
	 */
	private void checkSingleBatchResult(PrincipleImageResponse saveResponse,
	                                    Map<Integer, Integer> transactionGroupMap,
	                                    PrincipleImage relatedSegments,
	                                    Map<Integer, Boolean> groupResultMap) {
	    // 防御性校验：避免空指针
	    if (saveResponse == null || transactionGroupMap == null || relatedSegments == null || groupResultMap == null) {
	        log.warn("checkSingleBatchResult 参数为空，跳过结果检查");
	        return;
	    }
	
	    try {
	        // 1. 检查新增操作结果
	        checkOperateResult(
	                saveResponse.getAdditionResponse(),       // 新增操作的执行计数（key=子列表索引，value=成功数/-1失败）
	                relatedSegments.getAddition(),    // 新增的原始子列表
	                "新增",                           // 操作类型（日志用）
	                transactionGroupMap,
	                groupResultMap
	        );
	
	        // 2. 检查修改操作结果
	        checkOperateResult(
	                saveResponse.getModificationResponse(),
	                relatedSegments.getModification(),
	                "修改",
	                transactionGroupMap,
	                groupResultMap
	        );
	
	        // 3. 检查删除操作结果
	        checkOperateResult(
	                saveResponse.getDeletionResponse(),
	                relatedSegments.getDeletion(),
	                "删除",
	                transactionGroupMap,
	                groupResultMap
	        );
	
	    } catch (Exception e) {
	        log.error("检查单批次结果失败", e);
	        // 异常时标记所有事务组为失败（兜底，避免漏回滚）
	        groupResultMap.replaceAll((k, v) -> false);
	    }
	}
	
	/**
	 * 检查单个操作类型（新增/修改/删除）的执行结果，标记失败的事务组
	 * @param operateCount 操作执行计数（key=子列表索引，value=成功数/-1失败）
	 * @param subLists 该操作类型下的原始子列表集合
	 * @param operateType 操作类型（新增/修改/删除，仅日志用）
	 * @param transactionGroupMap 数据项hashCode → 事务组编号
	 * @param groupResultMap 事务组执行结果标记
	 */
	private void checkOperateResult(Map<Integer, Long> operateCount,
	                                List<List<? extends Principle<?>>> subLists,
	                                String operateType,
	                                Map<Integer, Integer> transactionGroupMap,
	                                Map<Integer, Boolean> groupResultMap) {
	    // 防御性校验
	    if (operateCount == null || subLists == null) {
	        log.debug("{}操作结果/原始数据为空，跳过检查", operateType);
	        return;
	    }
	
	    // 遍历每个子列表的执行结果
	    for (Map.Entry<Integer, Long> entry : operateCount.entrySet()) {
	        int subListIndex = entry.getKey(); // 子列表在集合中的索引
	        Long executeCount = entry.getValue(); // 执行结果：>=0成功数，-1失败
	
	        // 跳过空列表/成功的情况
	        if (executeCount == null || executeCount >= 0) {
	            continue;
	        }
	
	        // 找到失败的原始子列表
	        if (subListIndex < 0 || subListIndex >= subLists.size()) {
	            log.warn("{}操作子列表索引{}超出范围，总数{}", operateType, subListIndex, subLists.size());
	            continue;
	        }
	        List<? extends Principle<?>> failedSubList = subLists.get(subListIndex);
	        if (failedSubList == null || failedSubList.isEmpty()) {
	            log.debug("{}操作子列表{}为空，无需标记事务组", operateType, subListIndex);
	            continue;
	        }
	
	        // 根据子列表的hashCode找到所属事务组
	        int subListHashCode = System.identityHashCode(failedSubList);
	        Integer failedGroupNum = transactionGroupMap.get(subListHashCode);
	        if (failedGroupNum == null) {
	            log.warn("{}操作子列表{}未绑定事务组（hashCode={}），无法标记",
	                    operateType, subListIndex, subListHashCode);
	            continue;
	        }
	
	        // 标记该事务组为失败（一旦失败，永久标记为false）
	        if (groupResultMap.containsKey(failedGroupNum)) {
	            groupResultMap.put(failedGroupNum, false);
	            log.info("{}操作子列表{}执行失败，标记事务组{}为失败",
	                    operateType, subListIndex, failedGroupNum);
	        } else {
	            log.warn("事务组{}不存在于结果映射中，跳过标记", failedGroupNum);
	        }
	    }
	}
	
	
	
    
	/*
	 * ==================================日志 辅助工具区==============================
	 */

    /**
     * 打印PrincipleImage数据（JSON格式），识别PrincipleImagery并打印describe
     *
     * @param relatedSegments 要打印的原则图片数据
     */
    private void printPrincipleImage(PrincipleImage relatedSegments) {
        // 空值校验
        if (relatedSegments == null) {
            log.info("【PrincipleImage】数据为空");
            return;
        }

        log.info("============= 开始打印PrincipleImage =============");
        
        // 打印新增数据
        printJsonWithDescribe("新增(addition)", relatedSegments.getAddition());
        // 打印修改数据
        printJsonWithDescribe("修改(modification)", relatedSegments.getModification());
        // 打印删除数据
        printJsonWithDescribe("删除(deletion)", relatedSegments.getDeletion());
        
        log.info("============= 结束打印PrincipleImage =============");
    }

    /**
     * 核心方法：打印JSON + 识别PrincipleImagery并输出describe
     *
     * @param type 数据类型（新增/修改/删除）
     * @param dataList 要打印的嵌套列表数据
     */
    private void printJsonWithDescribe(String type, List<List<? extends Principle<?>>> dataList) {
        // 空值/空列表校验
        if (Objects.isNull(dataList) || dataList.isEmpty()) {
            log.info("【{}】无数据", type);
            return;
        }

        // 构建日志内容：先打印describe（如果有），再打印JSON
        StringBuilder logContent = new StringBuilder();
        logContent.append("【").append(type).append("】\n");

        // 遍历外层列表，识别PrincipleImagery并提取describe
        for (int i = 0; i < dataList.size(); i++) {
            List<? extends Principle<?>> innerList = dataList.get(i);
            if (innerList instanceof PrincipleImagery<?>) {
                String describe = ((PrincipleImagery<?>) innerList).getDescribe();
                logContent.append("  第").append(i ).append("组描述: ")
                          .append(Objects.isNull(describe) ? "无描述" : describe).append("\n");
            }
        }

        // 序列化JSON（捕获异常，避免序列化失败导致程序中断）
        String jsonStr;
        try {
            jsonStr = objectMapper.writeValueAsString(dataList);
        } catch (JsonProcessingException e) {
            jsonStr = "JSON序列化失败: " + e.getMessage();
            log.warn("【{}】数据JSON序列化异常", type, e);
        }

        // 拼接JSON并打印
        logContent.append("  数据(JSON格式): \n").append(jsonStr);
        log.info(logContent.toString());
    }
    
    
    
    
    
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
        	log.error("JSON序列化失败", e);
            return null; 
        }
    }
    
}
