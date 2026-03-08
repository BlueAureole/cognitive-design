package xyz.zhiwei.cognitivedesign.service;

import java.util.List;

import xyz.zhiwei.cognitivedesign.morphism.Appearance;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.response.ImageResponse;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.response.ResponseLane;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.response.ResponseLaneGroup;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.response.ResponsePackage;

/**
 * 变换响应数
 */
public class MorphismResponse<A extends Appearance> {
	private A a;
	private ResponsePackage responsePackage;


	public MorphismResponse(A a,ResponsePackage responsePackage) {
		this.a=a;
		this.responsePackage=responsePackage;
	}



	
	public Long getResponse(Integer index) {
		ImageResponse frist=getNoTxImageResponse(0,0);
		if(null==frist) {
			return null;
		}
		return frist.get(index);
	}
	
	
	
	public Long getResponse(int lane,int batch,Integer index) {
		ImageResponse one=getNoTxImageResponse(lane,batch);
		if(null==one) {
			return null;
		}
		return one.get(index);
	}
	
	
	public Long getResponse(int group,int lane,int batch,Integer index) {
		ImageResponse one=getTxImageResponse(group,lane,batch);
		if(null==one) {
			return null;
		}
		return one.get(index);
	}

	
	
	private ImageResponse getNoTxImageResponse(int lane,int batch) {
		ResponseLaneGroup noTransactionGroup=this.responsePackage.getNoTransactionGroup();
		
		if(null==noTransactionGroup || noTransactionGroup.size() < lane+1) {
			return null;
		}
		ResponseLane responseLane=noTransactionGroup.get(lane);


		if(null==responseLane || responseLane.size() < batch+1) {
			return null;
		}
		ImageResponse imageResponse=responseLane.get(batch);
		
		return imageResponse;
	}

	
	private ImageResponse getTxImageResponse(int group,int lane,int batch) {
		List<ResponseLaneGroup> transactionGroupList=this.responsePackage.getTransactionGroupList();

		if(null==transactionGroupList || transactionGroupList.size() < group+1) {
			return null;
		}
		ResponseLaneGroup transactionGroup=transactionGroupList.get(group);
		
		
		if(null==transactionGroup || transactionGroup.size() < lane+1) {
			return null;
		}
		ResponseLane responseLane=transactionGroup.get(lane);


		if(null==responseLane || responseLane.size() < batch+1) {
			return null;
		}
		ImageResponse imageResponse=responseLane.get(batch);
		
		return imageResponse;
	}




	public A getA() {
		return a;
	}

	public ResponsePackage getResponsePackage() {
		return responsePackage;
	}
	
}
