package xyz.zhiwei.cognitivedesign.morphism.principle.image.response;

import java.util.List;

/**
 * 映象包
 */
public class ResponsePackage {
	/**
	 * 非事务组
	 */
	private ResponseLaneGroup noTransactionGroup;
	/**
	 * 事务组列表
	 */
	private List<ResponseLaneGroup> transactionGroupList;
	
	
	public ResponsePackage(ResponseLaneGroup noTransactionGroup) {
		this.noTransactionGroup=noTransactionGroup;
	}

	
	public ResponsePackage(List<ResponseLaneGroup> transactionGroupList) {
		this.transactionGroupList = transactionGroupList;
	}
	
	
	public ResponsePackage(ResponseLaneGroup noTransactionGroup, List<ResponseLaneGroup> transactionGroupList) {
		this.noTransactionGroup = noTransactionGroup;
		this.transactionGroupList = transactionGroupList;
	}

	
	
	public ResponseLaneGroup getNoTransactionGroup() {
		return noTransactionGroup;
	}
	public void setNoTransactionGroup(ResponseLaneGroup noTransactionGroup) {
		this.noTransactionGroup = noTransactionGroup;
	}
	public List<ResponseLaneGroup> getTransactionGroupList() {
		return transactionGroupList;
	}
	public void setTransactionGroupList(List<ResponseLaneGroup> transactionGroupList) {
		this.transactionGroupList = transactionGroupList;
	}

}
