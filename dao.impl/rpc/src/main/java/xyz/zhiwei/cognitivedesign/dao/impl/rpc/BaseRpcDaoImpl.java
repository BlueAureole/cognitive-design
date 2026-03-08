package xyz.zhiwei.cognitivedesign.dao.impl.rpc;

import java.util.UUID;
import java.util.concurrent.Executor;

import javax.sql.XADataSource;

import org.springframework.beans.factory.annotation.Autowired;

import xyz.zhiwei.cognitivedesign.dao.accessimpl.CustomWriteThreadPool;
import xyz.zhiwei.cognitivedesign.dao.impl.rpc.transaction.TransactionAdapterInterface;
import xyz.zhiwei.cognitivedesign.dao.impl.rpc.transaction.TransactionRecordDao;
import xyz.zhiwei.cognitivedesign.dao.impl.rpc.transaction.synchronization.RpcTxWorkExecutorHolder;
import xyz.zhiwei.cognitivedesign.dao.impl.rpc.transaction.synchronization.TransactionAdapter;
import xyz.zhiwei.cognitivedesign.morphism.Principle;



/**
 * @updateBy zhanghaiting
 * @param <P>
 */
public class BaseRpcDaoImpl<P extends Principle<Long>> extends BaseMsgTxDaoImpl<P> {

	@Autowired
	private TransactionRecordDao transactionRecordDao;

	@Autowired
	private RpcTxWorkExecutorHolder rpcTxWorkExecutorHolder;
	
	@Override
	public XADataSource getXADataSource() {
		return transactionRecordDao.getXADataSource();
	}

	@Override
	protected TransactionRecordDao getTransactionRecordDao() {
		return transactionRecordDao;
	}
	
	@Override
	protected TransactionAdapterInterface getTransactionAdapterInterface() {
		Executor executor = rpcTxWorkExecutorHolder.getRpcTxWorkExecutor();
		if (this instanceof CustomWriteThreadPool) {
			Executor customExecutor = ((CustomWriteThreadPool) this).getWriteExecutor();
			if (customExecutor != null) {
				executor = customExecutor;
			}
		}
		return new TransactionAdapter(executor);
	}

	
	/*
	 * ==============可根据需要重写=====
	 */

	@Override
	public Object getTransactionVisibilityKey() {
		return UUID.randomUUID().toString();
	}
	@Override
	protected String getTransactionKey() {
		return "tn-"+UUID.randomUUID().toString();
	}



}
