package xyz.zhiwei.cognitivedesign.morphism.principle.image.container;

import java.util.ArrayList;
import java.util.List;

/**
 * 本原段
 */
public class PrincipleImage extends ArrayList<PrincipleImagery<?>> {
	private static final long serialVersionUID = 1L;
	
	public PrincipleImage() {}
	
	public PrincipleImage(List<PrincipleImagery<?>> list) {
		this.addAll(list);
	}

	public PrincipleImage(PrincipleImagery<?> frist) {
		this.add(frist);
	}
	public PrincipleImage(PrincipleImagery<?> first,PrincipleImagery<?> second) {
		this.add(first);
		this.add(second);
	}
	public PrincipleImage(PrincipleImagery<?> first,PrincipleImagery<?> second,PrincipleImagery<?> third) {
		this.add(first);
		this.add(second);
		this.add(third);
	}
	public PrincipleImage(PrincipleImagery<?> first,PrincipleImagery<?> second,PrincipleImagery<?> third,PrincipleImagery<?> fourth) {
		this.add(first);
		this.add(second);
		this.add(third);
		this.add(fourth);
	}
	public PrincipleImage(PrincipleImagery<?> first,PrincipleImagery<?> second,PrincipleImagery<?> third,PrincipleImagery<?> fourth,PrincipleImagery<?> fifth) {
		this.add(first);
		this.add(second);
		this.add(third);
		this.add(fourth);
		this.add(fifth);
	}

    
	public PrincipleImage addOne(PrincipleImagery<?> first) {
		this.add(first);
		return this;
	}
	
	public PrincipleImage add(PrincipleImagery<?> first,PrincipleImagery<?> second) {
		this.add(first);
		this.add(second);
		return this;
	}

	public PrincipleImage add(PrincipleImagery<?> first,PrincipleImagery<?> second,PrincipleImagery<?> third) {
		this.add(first);
		this.add(second);
		this.add(third);
		return this;
	}
	public PrincipleImage add(PrincipleImagery<?> first,PrincipleImagery<?> second,PrincipleImagery<?> third,PrincipleImagery<?> fourth) {
		this.add(first);
		this.add(second);
		this.add(third);
		this.add(fourth);
		return this;
	}

	public PrincipleImage add(PrincipleImagery<?> first,PrincipleImagery<?> second,PrincipleImagery<?> third,PrincipleImagery<?> fourth,PrincipleImagery<?> fifth) {
		this.add(first);
		this.add(second);
		this.add(third);
		this.add(fourth);
		this.add(fifth);
		return this;
	}
}
