package xyz.zhiwei.cognitivedesign.dao.accessimpl.write.normalgroup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.zhiwei.cognitivedesign.dao.accessimpl.DaoBeanCache;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.container.ImageLane;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.container.ImageLaneGroup;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.response.ResponseLane;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.response.ResponseLaneGroup;


/**
 * 非事务组处理
 */
public class WriteNormalGroup {
    private static final Logger log = LoggerFactory.getLogger(WriteNormalGroup.class);

    private Executor daoScheduleExecutor;
    private WriteNormalLane writeNormalLane;
    
    
    
    public WriteNormalGroup(DaoBeanCache daoBeanCache,Executor daoScheduleExecutor,Executor daoWriteExecutor) {
    	this.daoScheduleExecutor=daoScheduleExecutor;
    	this.writeNormalLane=new WriteNormalLane(daoBeanCache,daoWriteExecutor);
    }
    
    
	/**
	 * 存储一个组
	 * @param noTransactionGroup
	 * @return
	 */
	public ResponseLaneGroup save(ImageLaneGroup noTransactionGroup) {
		List<CompletableFuture<ResponseLane>> futures = new ArrayList<>();
		
		for (int laneIndex = 0; laneIndex < noTransactionGroup.size(); laneIndex++) {
			final int currentLaneIndex = laneIndex;
			ImageLane imageLane = noTransactionGroup.get(laneIndex);
			CompletableFuture<ResponseLane> future = CompletableFuture.supplyAsync(() -> 
					writeNormalLane.saveLane(imageLane, currentLaneIndex), daoScheduleExecutor
				).exceptionally(e -> {
					log.error("save noTransactionGroup lane error, laneIndex={}", currentLaneIndex, e);
					return new ResponseLane(new ArrayList<>());
				});
			futures.add(future);
		}
		
		ResponseLaneGroup responseLaneGroup=new ResponseLaneGroup();
		for (int i = 0; i < futures.size(); i++) {
			CompletableFuture<ResponseLane> future = futures.get(i);
			int laneIndex = i;
			ResponseLane responseLane;
			try {
				responseLane = future.get();
			} catch (Exception e) {
				log.error("save noTransactionGroup lane error, laneIndex={}", laneIndex, e);
				responseLane = new ResponseLane(new ArrayList<>());
			}
			responseLaneGroup.add(responseLane);
		}
		
		return responseLaneGroup;
	}

}
