package xyz.zhiwei.cognitivedesign.dao.impl.rpc.config;

import java.util.concurrent.Executor;

import javax.sql.XADataSource;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.annotation.Bean;

import xyz.zhiwei.cognitivedesign.dao.impl.rpc.transaction.TransactionRecordDao;
import xyz.zhiwei.cognitivedesign.dao.impl.rpc.transaction.recorder.TransactionRecordDaoImpl;
import xyz.zhiwei.cognitivedesign.dao.impl.rpc.transaction.synchronization.RpcTxWorkExecutorHolder;

public abstract class BaseRpcTxConfig {

    
    /**
     * 事务记录器
     * @param sqlSessionTemplate
     * @return
     */
	@Bean
	public TransactionRecordDao transactionRecordDao() {
		TransactionRecordDaoImpl transactionRecordDaoImpl=new TransactionRecordDaoImpl(getTransactionRecordXADataSource(),getTransactionRecordSqlSession());
		return transactionRecordDaoImpl;
	}
	
	/**
	 * rpc事务工作线程池持有者
	 * @return
	 */
	@Bean
	public RpcTxWorkExecutorHolder rpcTxWorkExecutorHolder() {
		RpcTxWorkExecutorHolder rpcTxWorkExecutorHolder=new RpcTxWorkExecutorHolder(getRpcTxWorkExecutor());
		return rpcTxWorkExecutorHolder;
	}
	
	
	protected abstract XADataSource getTransactionRecordXADataSource();
	protected abstract SqlSessionTemplate getTransactionRecordSqlSession();
	protected abstract Executor getRpcTxWorkExecutor();
	
}

