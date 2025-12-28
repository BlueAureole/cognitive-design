package xyz.zhiwei.cognitivedesign.service;

import java.util.List;

import xyz.zhiwei.cognitivedesign.morphism.Appearance;
import xyz.zhiwei.cognitivedesign.morphism.support.image.PrincipleImageResponse;

/**
 * 变换响应数
 */
public class MorphismResponse<A extends Appearance> {
	private A a;
	private List<PrincipleImageResponse> principleResponseList;


	public MorphismResponse(A a,List<PrincipleImageResponse> principleResponseList) {
		this.a=a;
		this.principleResponseList=principleResponseList;
	}
	
	
	public A getA() {
		return a;
	}
	public List<PrincipleImageResponse> getPrincipleResponseList() {
		return principleResponseList;
	}

	

	
	public Long getAdditionResponse(Integer index) {
		PrincipleImageResponse frist=getFrist();
		if(null==frist) {
			return null;
		}
		return frist.getAdditionResponse(index);
	}
	
	public Long getModificationResponse(Integer index) {
		PrincipleImageResponse frist=getFrist();
		if(null==frist) {
			return null;
		}
		return frist.getModificationResponse(index);
	}
	
	public Long getDeletionResponse(Integer index) {
		PrincipleImageResponse frist=getFrist();
		if(null==frist) {
			return null;
		}
		return frist.getDeletionResponse(index);
	}
	
	
	
	private PrincipleImageResponse getFrist() {
		if(null!=principleResponseList && principleResponseList.size()>0) {
			return principleResponseList.getFirst();
		}
		return null;
	}
	
	
	
}
