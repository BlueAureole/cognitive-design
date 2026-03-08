package xyz.zhiwei.cognitivedesign.dao.accessimpl.write.normalgroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.zhiwei.cognitivedesign.dao.Dao;
import xyz.zhiwei.cognitivedesign.dao.accessimpl.CustomWriteThreadPool;
import xyz.zhiwei.cognitivedesign.dao.accessimpl.DaoBeanCache;
import xyz.zhiwei.cognitivedesign.dao.accessimpl.TimeOutConfig;
import xyz.zhiwei.cognitivedesign.dao.accessimpl.write.WriteAccessLog;
import xyz.zhiwei.cognitivedesign.morphism.Principle;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.container.ImageLane;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.container.PrincipleImage;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.container.PrincipleImagery;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.response.ImageResponse;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.response.ResponseLane;

/**
 * 非事务泳道
 */
public class WriteNormalLane {
    private static final Logger log = LoggerFactory.getLogger(WriteNormalLane.class);
    private Integer TIMEOUT=TimeOutConfig.TIME_OUT_WRITE_UNIT;
    private final Long FAIL_VALUE=-1L;
    private final Long EMPTY_VALUE=0L;

    private DaoBeanCache daoBeanCache;
    private Executor daoWriteExecutor;
	
    public WriteNormalLane(DaoBeanCache daoBeanCache,Executor daoWriteExecutor) {
    	this.daoBeanCache=daoBeanCache;
    	this.daoWriteExecutor=daoWriteExecutor;
    }
    

	/*
	 * ================================================= 泳道 ===============================================================
	 */

	/**
	 * 存储一个泳道
	 * @param noTransactionLane
	 * @param laneIndex 泳道序号
	 * @return
	 */
	public ResponseLane saveLane(ImageLane noTransactionLane, int laneIndex) {
		
		List<ImageResponse> respList=new ArrayList<>();

        log.info("============= 非事务组 第{}号泳道 写入开始 =============", laneIndex);
		
		for (int i = 0; i < noTransactionLane.size(); i++) {
			Function<List<ImageResponse>,PrincipleImage> batchFun = noTransactionLane.get(i);
			PrincipleImage principleImage=batchFun.apply(respList);
			WriteAccessLog.image(-1,laneIndex, i, principleImage);
			ImageResponse imageResponse=saveBatch(principleImage);
			WriteAccessLog.resp(-1,laneIndex, i, imageResponse);
			respList.add(imageResponse);
		}

        log.info("============= 非事务组 第{}号泳道 写入结束 =============", laneIndex);
		
		return new ResponseLane(respList);
	}
    
	
	


	/*
	 * ================================================= 批次 ===============================================================
	 */

	
	
	/**
	 * 存储一个批次
	 * @param principleImage
	 * @return
	 */
	private ImageResponse saveBatch(PrincipleImage principleImage) {
		ImageResponse imageResponse=new ImageResponse(); 

        if (isEmpty(principleImage)) {
            return imageResponse;
        }
		
		List<CompletableFuture<Long>> futures = new ArrayList<>();
		for (int i = 0; i < principleImage.size(); i++) {
			final PrincipleImagery<?> principleImagery = principleImage.get(i);
			
			Executor executorToUse = this.daoWriteExecutor;
			Dao<?> dao = daoBeanCache.getDaoBeanByPrincipleClass(daoBeanCache.getClassFromList(principleImagery));
			if (dao instanceof CustomWriteThreadPool) {
				Executor customExecutor = ((CustomWriteThreadPool) dao).getWriteExecutor();
				if (customExecutor != null) {
					executorToUse = customExecutor;
				}
			}
			
			CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
				try {
					return saveUnit(principleImagery);
				} catch (Exception e) {
					log.error("saveBatch unit error", e);
					return FAIL_VALUE;
				}
			}, executorToUse);
			futures.add(future);
		}
		
		for (int i = 0; i < futures.size(); i++) {
			CompletableFuture<Long> future = futures.get(i);
			Long count;
			try {
				count = future.get(TIMEOUT, TimeUnit.SECONDS);
			} catch (Exception e) {
				log.error("saveBatch unit error/timeout", e);
				count = FAIL_VALUE;
			}
			if (count == null) {
				count = EMPTY_VALUE;
			}
			imageResponse.put(i, count);
		}
		
		return imageResponse;
	}
	
	
	
	
	
	

	/*
	 * ================================================= 单元 ===============================================================
	 */

	
	
	
	/**
	 * 存储子列表
	 * （借助入参套出泛型）
	 * @param <P>
	 * @param principleImagery
	 * @return
	 */
	private <P extends Principle<?>> Long saveUnit(PrincipleImagery<P> principleImagery) {

		Dao<P> relatedDao=daoBeanCache.getDaoBeanByPrincipleClass(daoBeanCache.getClassFromList(principleImagery));
		Long relateCount=relatedDao.save(principleImagery);
		if(null==relateCount) {
			return EMPTY_VALUE;
		}
		return relateCount;
	}
	
	
	/**
	 * @param list
	 * @return
	 */
	private boolean isEmpty(List<?> list) {

        if (null == list || list.isEmpty() || !list.stream().anyMatch(Objects::nonNull)) {
            return true;
        }
        return false;
	}
	
}
