package xyz.zhiwei.cognitivedesign.morphism.support.qualifier;

import java.util.ArrayList;

/**
 * 本原限定组
 */
public class PrincipleQualifiers extends ArrayList<PrincipleQualifier<?>> {
	private static final long serialVersionUID = -5574074618114442162L;

	public PrincipleQualifiers() {}
	
	public PrincipleQualifiers(PrincipleQualifier<?> bq) {
		this.add(bq);
	}
	public PrincipleQualifiers(PrincipleQualifier<?> first,PrincipleQualifier<?> second) {
		this.add(first);
		this.add(second);
	}
	public PrincipleQualifiers(PrincipleQualifier<?> first,PrincipleQualifier<?> second,PrincipleQualifier<?> third) {
		this.add(first);
		this.add(second);
		this.add(third);
	}
	public PrincipleQualifiers(PrincipleQualifier<?> first,PrincipleQualifier<?> second,PrincipleQualifier<?> third,PrincipleQualifier<?> fourth) {
		this.add(first);
		this.add(second);
		this.add(third);
		this.add(fourth);
	}
	public PrincipleQualifiers(PrincipleQualifier<?> first,PrincipleQualifier<?> second,PrincipleQualifier<?> third,PrincipleQualifier<?> fourth,PrincipleQualifier<?> fifth) {
		this.add(first);
		this.add(second);
		this.add(third);
		this.add(fourth);
		this.add(fifth);
	}
	
	public PrincipleQualifiers addOne(PrincipleQualifier<?> first) {
		this.add(first);
		return this;
	}
	
	public PrincipleQualifiers add(PrincipleQualifier<?> first,PrincipleQualifier<?> second) {
		this.add(first);
		this.add(second);
		return this;
	}

	public PrincipleQualifiers add(PrincipleQualifier<?> first,PrincipleQualifier<?> second,PrincipleQualifier<?> third) {
		this.add(first);
		this.add(second);
		this.add(third);
		return this;
	}
	public PrincipleQualifiers add(PrincipleQualifier<?> first,PrincipleQualifier<?> second,PrincipleQualifier<?> third,PrincipleQualifier<?> fourth) {
		this.add(first);
		this.add(second);
		this.add(third);
		this.add(fourth);
		return this;
	}

	public PrincipleQualifiers add(PrincipleQualifier<?> first,PrincipleQualifier<?> second,PrincipleQualifier<?> third,PrincipleQualifier<?> fourth,PrincipleQualifier<?> fifth) {
		this.add(first);
		this.add(second);
		this.add(third);
		this.add(fourth);
		this.add(fifth);
		return this;
	}
}
