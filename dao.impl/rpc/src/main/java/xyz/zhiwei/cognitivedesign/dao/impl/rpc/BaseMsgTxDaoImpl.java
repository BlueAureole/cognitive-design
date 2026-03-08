package xyz.zhiwei.cognitivedesign.dao.impl.rpc;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import xyz.zhiwei.cognitivedesign.dao.TransactionDao;
import xyz.zhiwei.cognitivedesign.dao.daoimpl.BaseMsgImpl;
import xyz.zhiwei.cognitivedesign.dao.daoimpl.methodcache.ReflectGenericUtils;
import xyz.zhiwei.cognitivedesign.dao.daoimpl.model.Message;
import xyz.zhiwei.cognitivedesign.dao.impl.rpc.transaction.TransactionAdapterInterface;
import xyz.zhiwei.cognitivedesign.dao.impl.rpc.transaction.TransactionRecordDao;
import xyz.zhiwei.cognitivedesign.dao.impl.rpc.transaction.model.TransactionRecord;
import xyz.zhiwei.cognitivedesign.morphism.Principle;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.container.PrincipleImagery;



/**
 * @updateBy zhanghaiting
 * @param <P>
 */
public abstract class BaseMsgTxDaoImpl<P extends Principle<Long>> extends BaseMsgImpl<P> implements TransactionDao<P> {

    protected final Type genericPType;
	
    @Autowired
	protected ObjectMapper objectMapper;
    
    protected Long WAIT=-200L;
    
    protected Integer STATUS_INIT=0;
    protected Integer STATUS_SUCCESS=1;
    protected Integer STATUS_FALSE=-1;
	

    //init
	public BaseMsgTxDaoImpl() {
        Class<?> currentClass = this.getClass();
    	this.genericPType = ReflectGenericUtils.getGenericTypeParameter(currentClass, Arrays.asList(BaseMsgImpl.class), 0);
    }
	
	
	//获取事务唯一标识键
	protected abstract String getTransactionKey();
	//事务回调器
	protected abstract TransactionAdapterInterface getTransactionAdapterInterface();
	//事务记录器
	protected abstract TransactionRecordDao getTransactionRecordDao();

	
    /**
     * 重写分发方法，以支持事务最终一致性
     */
	@Override
	protected Message<Long> dispatchSaveMethod(PrincipleImagery<P> principleImagery) {
	    boolean isInTransaction = TransactionSynchronizationManager.isActualTransactionActive();
	    if(!isInTransaction) {
	    	return super.dispatchSaveMethod(principleImagery);
	    }
	    
	    
        // Reuse adapter if exists in current transaction context to support multiple RPC tasks in one transaction
        TransactionAdapterInterface transactionAdapter;
        Object adapterKey = TransactionAdapterInterface.class;
        
        if (TransactionSynchronizationManager.hasResource(adapterKey)) {
            transactionAdapter = (TransactionAdapterInterface) TransactionSynchronizationManager.getResource(adapterKey);
        } else {
            transactionAdapter = getTransactionAdapterInterface();
            TransactionSynchronizationManager.bindResource(adapterKey, transactionAdapter);
            TransactionSynchronizationManager.registerSynchronization(transactionAdapter);
        }

	    TransactionRecordDao transactionRecordDao=getTransactionRecordDao();
	    
	    
        
	    String transactionKey=getTransactionKey();//"tn-"+UUID.randomUUID().toString();
		String principleName=this.genericPType.getTypeName();
    	String operation=principleImagery.getDescribe();
		String paramStr=getJson(principleImagery);
		TransactionRecord transactionRecord=new TransactionRecord(transactionKey,STATUS_INIT,principleName,operation,paramStr);


		//携带事务唯一标识
		PrincipleImagery<P> transactionImagery=new PrincipleImagery<>(principleImagery);
		transactionImagery.setId(transactionKey);
		
		
		//事务提交后，执行的函数
		Consumer<PrincipleImagery<P>> consumer=getConsumer();
		
	    try {
	        // 插入事务记录
	    	transactionRecordDao.add(transactionRecord);
	        // 任务入队
	    	transactionAdapter.addToQueue(consumer, transactionImagery);
	    } catch (Exception e) {
	        // 入队失败，回滚事务记录插入
	        if (null!=transactionKey) {
	        	transactionRecordDao.deleteByTxKey(new TransactionRecord(transactionKey));
	        }
	        log.error("事务记录插入/任务入队失败", e);
	        throw new RuntimeException(e);
	    }
		
	    return Message.success(WAIT);
    }
	
	
	/**
	 * 事务提交后，执行的函数
	 * @return
	 */
	private Consumer<PrincipleImagery<P>> getConsumer(){
	    TransactionRecordDao transactionRecordDao=getTransactionRecordDao();
		
		Consumer<PrincipleImagery<P>> consumer=(transactionImageryData)->{
			
			Integer status=null;
			Message<Long> msg=null;
			String msgStr=null;
			try {
				msg=super.dispatchSaveMethod(transactionImageryData);
				msgStr=getJson(msg);
				status=STATUS_SUCCESS;
			}catch (Exception e) {
				log.error("事务后置任务执行异常，参数：{}", getJson(transactionImageryData), e);
				status=STATUS_FALSE;
			}
			
			//根据更新结果，补充事务完成状态		
			TransactionRecord trm=new TransactionRecord(transactionImageryData.getId(),status,msgStr);
			transactionRecordDao.updateByTxKey(trm);
		};
		
		return consumer;
	}
	
	
	

	/**
	 * 如果序列化失败则抛异常，因为无法落库记录操作数，从而无法发起重试。进而无法保证事务最终一致性。
	 * @param t
	 * @return
	 */
	protected String getJson(Object t) {
	    try {
	        return objectMapper.writeValueAsString(t);
	    } catch (JsonProcessingException e) {
	        String errorMsg = String.format("序列化对象失败，类型：%s", t.getClass().getName());
	        log.error(errorMsg, e);
	        throw new RuntimeException(errorMsg, e);
	    }
	}




}
