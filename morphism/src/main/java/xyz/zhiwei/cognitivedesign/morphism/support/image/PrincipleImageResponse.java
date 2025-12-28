package xyz.zhiwei.cognitivedesign.morphism.support.image;

import java.util.HashMap;
import java.util.Map;

/**
 * 本原响应数
 */
public class PrincipleImageResponse {
	
	private Map<Integer,Long> additionResponse;
	private Map<Integer,Long> modificationResponse;
	private Map<Integer,Long> deletionResponse;
	
	
	
	public PrincipleImageResponse() {
		this.additionResponse=new HashMap<>();
		this.modificationResponse=new HashMap<>();
		this.deletionResponse=new HashMap<>();
	}

	public PrincipleImageResponse(Map<Integer,Long> additionResponse,Map<Integer,Long> modificationResponse,Map<Integer,Long> deletionResponse) {
		this.additionResponse=additionResponse;
		this.modificationResponse=modificationResponse;
		this.deletionResponse=deletionResponse;
	}
	
	
	
	
	
	public void setAdditionResponse(Integer index,Long count) {
		this.additionResponse.put(index, count);
	}
	public void setModificationResponse(Integer index,Long count) {
		this.modificationResponse.put(index, count);
	}
	public void setDeletionResponse(Integer index,Long count) {
		this.deletionResponse.put(index, count);
	}

	
	
	public Long getAdditionResponse(Integer index) {
		return additionResponse.get(index);
	}
	public Long getModificationResponse(Integer index) {
		return modificationResponse.get(index);
	}
	public Long getDeletionResponse(Integer index) {
		return deletionResponse.get(index);
	}
	
	
	
	public Map<Integer, Long> getAdditionResponse() {
		return additionResponse;
	}
	public Map<Integer, Long> getModificationResponse() {
		return modificationResponse;
	}
	public Map<Integer, Long> getDeletionResponse() {
		return deletionResponse;
	}
	
	
	
}
