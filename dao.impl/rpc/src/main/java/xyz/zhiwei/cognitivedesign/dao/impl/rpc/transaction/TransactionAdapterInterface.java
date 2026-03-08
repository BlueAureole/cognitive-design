package xyz.zhiwei.cognitivedesign.dao.impl.rpc.transaction;

import java.util.function.Consumer;

import org.springframework.transaction.support.TransactionSynchronization;



public interface TransactionAdapterInterface extends TransactionSynchronization{


    /**
     * 添加任务到事务后置队列
     * @param consumer 任务执行逻辑
     * @param t 任务参数
     * @param <T> 参数类型
     */
    <T> void addToQueue(Consumer<T> consumer, T t);
	
}
