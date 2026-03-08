package xyz.zhiwei.cognitivedesign.dao.impl.rpc.transaction.synchronization;

import java.util.concurrent.Executor;



/**
 * rpc作业线程池
 */
public class RpcTxWorkExecutorHolder {


    private Executor rpcTxWorkExecutor;

    
    public RpcTxWorkExecutorHolder(Executor rpcTxWorkExecutor) {
    	this.rpcTxWorkExecutor=rpcTxWorkExecutor;
    }
    
    
	public Executor getRpcTxWorkExecutor() {
		return rpcTxWorkExecutor;
	}

    
}
