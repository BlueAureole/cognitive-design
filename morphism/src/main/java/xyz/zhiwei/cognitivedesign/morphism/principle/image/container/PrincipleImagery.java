package xyz.zhiwei.cognitivedesign.morphism.principle.image.container;

import java.util.ArrayList;
import java.util.List;

import xyz.zhiwei.cognitivedesign.morphism.Principle;

/**
 * 本原块
 * @param <T>
 */
public class PrincipleImagery<P extends Principle<?>> extends ArrayList<P>{
	private static final long serialVersionUID = 1L;
	private String id;
	private String describe;

	
	
	public PrincipleImagery(List<P> list) {
		if(null==list) {
			return;
		}
		if(list instanceof PrincipleImagery) {
			PrincipleImagery<P> principleImagery=(PrincipleImagery<P>) list;
			this.setId(principleImagery.getId());
			this.setDescribe(principleImagery.getDescribe());
		}
		if(list.size()>0) {
			addAll(list);
		}
		
	}
	
	

    public PrincipleImagery(String id,List<P> list) {
		this.setId(id);
		if(null==list || list.size()==0) {
			return;
		}
		addAll(list);
	}
	
	public PrincipleImagery(List<P> list,String desc) {
		this.describe=desc;
		if(null==list || list.size()==0) {
			return;
		}
		addAll(list);
	}
	

	
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	} 
	
	public String getDescribe() {
		return describe;
	}

	public void setDescribe(String describe) {
		this.describe = describe;
	}

	
	
	
	
}
