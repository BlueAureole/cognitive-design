package xyz.zhiwei.cognitivedesign.dao.accessimpl.write;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.jta.JtaTransactionManager;

import xyz.zhiwei.cognitivedesign.dao.accessimpl.DaoBeanCache;
import xyz.zhiwei.cognitivedesign.dao.accessimpl.write.normalgroup.WriteNormalGroup;
import xyz.zhiwei.cognitivedesign.dao.accessimpl.write.txgroup.WriteTxGroup;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.container.ImageLaneGroup;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.container.ImagePackage;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.response.ResponseLaneGroup;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.response.ResponsePackage;


/**
 * 本原集存实现
 */
public class WriteAccess{
    private static final Logger log = LoggerFactory.getLogger(WriteAccess.class);
    
    
    private Executor daoScheduleExecutor;

    private WriteNormalGroup writeNormalGroup;
    private WriteTxGroup writeTxGroup;
    
    
    public WriteAccess(DaoBeanCache daoBeanCache,JtaTransactionManager jtaTransactionManager,
    		Executor daoScheduleExecutor,Executor daoWriteExecutor) {
    	
    	this.daoScheduleExecutor=daoScheduleExecutor;

    	this.writeNormalGroup=new WriteNormalGroup(daoBeanCache,daoScheduleExecutor,daoWriteExecutor);
    	this.writeTxGroup=new WriteTxGroup(daoBeanCache,jtaTransactionManager,daoScheduleExecutor,daoWriteExecutor);
    }
    
    
    

	/**
	 * 存储相关数据集
	 * 
	 * @param imagePackage
	 * @return
	 */
	public ResponsePackage save(ImagePackage imagePackage) {
    	
		ImageLaneGroup noTransactionGroup=imagePackage.getNoTransactionGroup();
		CompletableFuture<ResponseLaneGroup> noTransactionGroupFuture;
		if(noTransactionGroup==null || noTransactionGroup.isEmpty()) {
	        log.info("非事务组为空");
			noTransactionGroupFuture = CompletableFuture.completedFuture(new ResponseLaneGroup());
		}else {
			noTransactionGroupFuture = CompletableFuture.supplyAsync(() -> 
				writeNormalGroup.save(noTransactionGroup), daoScheduleExecutor
			).exceptionally(e -> {
				log.error("save noTransactionGroup error", e);
				return new ResponseLaneGroup();
			});
		}

		List<CompletableFuture<ResponseLaneGroup>> txFutures = new ArrayList<>();
		List<ImageLaneGroup> transactionGroupList=imagePackage.getTransactionGroupList();
		if(transactionGroupList!=null && !transactionGroupList.isEmpty()) {
			for (int i = 0; i < transactionGroupList.size(); i++) {
				final int txGroupIndex=i;
				ImageLaneGroup transactionGroup = transactionGroupList.get(txGroupIndex);
				CompletableFuture<ResponseLaneGroup> future;
				if(transactionGroup==null || transactionGroup.isEmpty()) {
			        log.info("事务组{}为空",txGroupIndex);
					future = CompletableFuture.completedFuture(new ResponseLaneGroup());
				}else {
					future = CompletableFuture.supplyAsync(() -> 
						writeTxGroup.save(transactionGroup,txGroupIndex), daoScheduleExecutor
					).exceptionally(e -> {
						log.error("save transactionGroup error, txGroupIndex={}", txGroupIndex, e);
						return new ResponseLaneGroup();
					});
				}
				txFutures.add(future);
			}
		}else {
	        log.info("事务组列表为空");
		}

		
		
		
		
		//非事务组结果收集
		ResponseLaneGroup noTransactionGroupResp;
		try {
			noTransactionGroupResp = noTransactionGroupFuture.get();
		} catch (Exception e) {
			log.error("save noTransactionGroup error", e);
			noTransactionGroupResp = new ResponseLaneGroup();
		}

		//事务组结果收集
		List<ResponseLaneGroup> transactionGroupListResp = new ArrayList<>();
		for (int i = 0; i < txFutures.size(); i++) {
			CompletableFuture<ResponseLaneGroup> future = txFutures.get(i);
			int txGroupIndex = i;
			ResponseLaneGroup resp;
			try {
				resp = future.get();
			} catch (Exception e) {
				log.error("save transactionGroup error, txGroupIndex={}", txGroupIndex, e);
				resp = new ResponseLaneGroup();
			}
			transactionGroupListResp.add(resp);
		}
		
    	return new ResponsePackage(noTransactionGroupResp,transactionGroupListResp);
    }
    
    
    
    
}
