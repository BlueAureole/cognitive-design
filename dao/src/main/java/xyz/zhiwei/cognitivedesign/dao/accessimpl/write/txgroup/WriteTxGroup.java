package xyz.zhiwei.cognitivedesign.dao.accessimpl.write.txgroup;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import xyz.zhiwei.cognitivedesign.dao.accessimpl.DaoBeanCache;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.container.ImageLane;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.container.ImageLaneGroup;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.response.ResponseLane;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.response.ResponseLaneGroup;

/**
 * 事务组处理
 */
public class WriteTxGroup {
    private static final Logger log = LoggerFactory.getLogger(WriteTxGroup.class);

    private JtaTransactionManager jtaTransactionManager;
    private Executor daoScheduleExecutor;

    private WriteTxLane writeTxLane;
	
    
    public WriteTxGroup(DaoBeanCache daoBeanCache, JtaTransactionManager jtaTransactionManager,
    		Executor daoScheduleExecutor, Executor daoWriteExecutor) {
    	if (jtaTransactionManager.getTransactionManager() == null) {
    		throw new IllegalArgumentException("JtaTransactionManager must have a valid jakarta.transaction.TransactionManager");
    	}
    	
    	this.jtaTransactionManager=jtaTransactionManager;
    	this.daoScheduleExecutor=daoScheduleExecutor;
    	this.writeTxLane=new WriteTxLane(daoBeanCache, daoWriteExecutor);
    }
    
    /**
     * 存储一个组
     *  整个组是一个独立的全局事务
     *  包含若干泳道
     *  @param transactionGroup
     *  @param txGroupIndex
     *  @return
     */
	public ResponseLaneGroup save(ImageLaneGroup transactionGroup,int txGroupIndex) {

        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setName("GroupTransactionDefinition");
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus status = jtaTransactionManager.getTransaction(def);
        
        // 跨线程事务相关资源
        TransactionManager jtaTm = jtaTransactionManager.getTransactionManager();
        CrossThreadSyncCollector syncCollector = new CrossThreadSyncCollector();
        // 收集所有泳道的连接，在 Group 提交后再关闭
        Queue<Connection> connectionCollector = new ConcurrentLinkedQueue<>();
        
        Transaction jtaTransaction = null;
        boolean isSuspended = false;
        
        try {
        	// 1. 获取并挂起 JTA 事务
			jtaTransaction = jtaTm.suspend();
			isSuspended = true;

            ResponseLaneGroup responseGroup = new ResponseLaneGroup();
            List<CompletableFuture<ResponseLane>> futures = new ArrayList<>();
            
            // 2. 并行执行泳道
            // 注意：此时 jtaTransaction 已经被挂起，可以安全地传递给子线程
            // 子线程将使用 Manual Enlistment 模式，不需要 Resume 事务，从而避免锁竞争
            final Transaction sharedTx = jtaTransaction;
            
            for (int i = 0; i < transactionGroup.size(); i++) {
                final int laneIndex = i;
                ImageLane imageLane = transactionGroup.get(laneIndex);
                
                CompletableFuture<ResponseLane> future = CompletableFuture.supplyAsync(() -> {
                	try {
						return writeTxLane.saveLane(imageLane, txGroupIndex, laneIndex, sharedTx, syncCollector, connectionCollector);
					} catch (Exception e) {
						throw new RuntimeException("Lane execution failed", e);
					}
                }, daoScheduleExecutor);
                
                futures.add(future);
            } 
            
            // 3. 等待所有泳道完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            for (CompletableFuture<ResponseLane> f : futures) {
            	responseGroup.add(f.get());
            }

            // 4. 恢复事务并提交
            if (isSuspended) {
            	jtaTm.resume(jtaTransaction);
            	isSuspended = false;
            	
            	// 5. 注册收集到的回调（确保在 Commit 前触发）
            	syncCollector.registerToCurrentThread();
            }

            jtaTransactionManager.commit(status);
            return responseGroup;
            
        } catch (Exception ex) {
        	log.error("Transaction group failed, rolling back", ex);
        	// 确保回滚
        	if (isSuspended) {
        		try {
					jtaTm.resume(jtaTransaction);
				} catch (Exception e) {
					log.error("Failed to resume transaction for rollback", e);
				}
        	}
        	
        	try {
        		jtaTransactionManager.rollback(status);
        	} catch (Exception e) {
        		log.error("Rollback failed", e);
        	}
        	
            throw new RuntimeException(ex);
        } finally {
        	syncCollector.clear();
        	// 统一关闭所有连接
        	closeConnections(connectionCollector);
        }
		

    }
    
	/**
	 * 清理连接
	 * 策略：Manual Enlistment 模式下，Transaction.enlistResource 仅告知 TM 资源参与事务
	 * 但连接的释放（close）仍需由应用层负责。我们在此统一关闭，将连接归还给连接池。
	 * @param connections
	 */
	private void closeConnections(Collection<Connection> connections) {
		connections.forEach(conn -> {
			try {
				if (conn != null && !conn.isClosed()) {
					conn.close();
				}
			} catch (Exception e) {
				log.warn("Failed to close lane connection", e);
			}
		});
		connections.clear();
	}
}
