package xyz.zhiwei.cognitivedesign.dao.impl.rpc.transaction.model;

import java.io.Serializable;

public class TransactionRecord implements Serializable {
	private static final long serialVersionUID = 1L;

    private String transactionKey;//用于全局事务识别
    private Integer status;

	private String principleName;//本原类全路径
	private String operation;//操作名
    private String param;//入参
    private String resp;//返回值

    
    public TransactionRecord(String transactionKey,Integer status,String principleName,String operation,String param) {
    	this.transactionKey=transactionKey;
    	this.status=status;
    	this.principleName=principleName;
    	this.operation=operation;
    	this.param=param;
    }


    public TransactionRecord(String transactionKey,Integer status,String resp) {
    	this.transactionKey=transactionKey;
    	this.status=status;
    	this.resp=resp;
    }
    
    public TransactionRecord(String transactionKey) {
    	this.transactionKey=transactionKey;
    }
    
    
    /*
     * ================getter setter===========
     */
    
    
	public String getTransactionKey() {
		return transactionKey;
	}


	public void setTransactionKey(String transactionKey) {
		this.transactionKey = transactionKey;
	}


	public Integer getStatus() {
		return status;
	}


	public void setStatus(Integer status) {
		this.status = status;
	}


	public String getPrincipleName() {
		return principleName;
	}


	public void setPrincipleName(String principleName) {
		this.principleName = principleName;
	}


	public String getOperation() {
		return operation;
	}


	public void setOperation(String operation) {
		this.operation = operation;
	}


	public String getParam() {
		return param;
	}


	public void setParam(String param) {
		this.param = param;
	}


	public String getResp() {
		return resp;
	}


	public void setResp(String resp) {
		this.resp = resp;
	}
    
    
}