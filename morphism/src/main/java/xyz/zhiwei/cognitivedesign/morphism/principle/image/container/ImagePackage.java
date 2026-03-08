package xyz.zhiwei.cognitivedesign.morphism.principle.image.container;

import java.util.List;

/**
 * 映象包
 */
public class ImagePackage {
	/**
	 * 非事务组
	 */
	private ImageLaneGroup noTransactionGroup;
	/**
	 * 事务组列表
	 */
	private List<ImageLaneGroup> transactionGroupList;
	
	
	public ImagePackage(ImageLaneGroup noTransactionGroup) {
		this.noTransactionGroup=noTransactionGroup;
	}

	
	public ImagePackage(List<ImageLaneGroup> transactionGroupList) {
		this.transactionGroupList = transactionGroupList;
	}
	
	
	public ImagePackage(ImageLaneGroup noTransactionGroup, List<ImageLaneGroup> transactionGroupList) {
		this.noTransactionGroup = noTransactionGroup;
		this.transactionGroupList = transactionGroupList;
	}

	
	
	public ImageLaneGroup getNoTransactionGroup() {
		return noTransactionGroup;
	}
	public void setNoTransactionGroup(ImageLaneGroup noTransactionGroup) {
		this.noTransactionGroup = noTransactionGroup;
	}
	public List<ImageLaneGroup> getTransactionGroupList() {
		return transactionGroupList;
	}
	public void setTransactionGroupList(List<ImageLaneGroup> transactionGroupList) {
		this.transactionGroupList = transactionGroupList;
	}

}
