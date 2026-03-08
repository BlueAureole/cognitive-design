package xyz.zhiwei.cognitivedesign.dao.impl.rpc.transaction.synchronization;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.zhiwei.cognitivedesign.dao.impl.rpc.transaction.TransactionAdapterInterface;

/**
* @author zhanghaiting
*/

public class TransactionAdapter implements TransactionAdapterInterface {

	Logger log  =  LoggerFactory.getLogger(TransactionAdapter.class);
    

    private Executor rpcTxWorkExecutor;
    private List<ConsumerAndData<?>> consumerAndDataList = new ArrayList<>();
    
    
    public TransactionAdapter(Executor rpcTxWorkExecutor) {
    	this.rpcTxWorkExecutor=rpcTxWorkExecutor;
    }
    
    
   @Override
   public void afterCommit() {
       log.info("afterCommit afterCommit start....");
       if(this.consumerAndDataList.isEmpty()) {
           log.info("consumerAndDataList is empty. and afterCommit end ...");
           return;
       }
       
       for (ConsumerAndData<?> consumerAndData : consumerAndDataList) {
           Runnable runnable = consumerAndData.getRunnable();
           if(null!=runnable) {
               try {
                   rpcTxWorkExecutor.execute(runnable);
               } catch (Exception e) {
                   log.error("Failed to submit transaction post-commit task", e);
               }
           }
       }
       log.info("afterCommit afterCommit end....");
   }

   
	@Override
	public <T> void addToQueue(Consumer<T> consumer, T t) {
		ConsumerAndData<?> consumerAndData=new ConsumerAndData<T>(consumer, t);
		this.consumerAndDataList.add(consumerAndData);
	}

   
	
	/**
	 * 执行容器
	 * @param <T>
	 */
	class ConsumerAndData<T>{
		private Consumer<T> consumer;
		private T t;
		
		public ConsumerAndData(Consumer<T> consumer, T t) {
			this.consumer=consumer;
			this.t=t;
		}

		public Consumer<T> getConsumer() {
			return consumer;
		}
		public T getT() {
			return t;
		}

		
		public Runnable getRunnable() {
			if(null==this.consumer || null==this.t) {
				return null;
			}
			
	        // 封装任务为Runnable
	        Runnable runnable = () -> {
	            try {
	                consumer.accept(t);
	                log.debug("事务后置任务执行成功，参数类型：{}", t.getClass().getSimpleName());
	            } catch (Exception e) {
	                log.error("事务后置任务执行异常，参数类型：{}", t.getClass().getSimpleName(), e);
	                throw new RuntimeException("事务后置任务执行失败", e); // 抛出异常便于重试机制捕获
	            }
	        };
			
			return runnable;
		}
		
		
		
	}
   
}

