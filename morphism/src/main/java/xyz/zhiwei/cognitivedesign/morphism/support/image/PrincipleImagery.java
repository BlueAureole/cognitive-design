package xyz.zhiwei.cognitivedesign.morphism.support.image;

import java.util.ArrayList;
import java.util.List;

import xyz.zhiwei.cognitivedesign.morphism.Principle;

/**
 * 本原块
 * @param <T>
 */
public class PrincipleImagery<P extends Principle<?>> extends ArrayList<P>{

	private static final long serialVersionUID = 8329881559572535988L;
	
	private String describe;

	
	
	public PrincipleImagery(List<P> list,String desc) {
		if(null==list || list.size()==0) {
			return;
		}
		addAll(list);
		this.describe=desc;
	}
	
	
	public String getDescribe() {
		return describe;
	}

	public void setDescribe(String describe) {
		this.describe = describe;
	} 
}
