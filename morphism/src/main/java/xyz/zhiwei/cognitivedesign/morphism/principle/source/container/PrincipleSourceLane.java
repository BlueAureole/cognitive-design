package xyz.zhiwei.cognitivedesign.morphism.principle.source.container;

import java.util.ArrayList;
import java.util.List;

/**
 * 泳道本原集
 */
public class PrincipleSourceLane extends ArrayList<PrincipleSource>{
	private static final long serialVersionUID = 1L;
	
	public PrincipleSourceLane() {}


	public PrincipleSourceLane(List<PrincipleSource> list) {
		this.addAll(list);
	}
}
