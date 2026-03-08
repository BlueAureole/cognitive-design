package xyz.zhiwei.cognitivedesign.morphism.principle.source.qualifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import xyz.zhiwei.cognitivedesign.morphism.principle.source.container.PrincipleSource;

/**
 * 限定符泳道
 * 1泳道=批次列表
 *      1批次=PrincipleQualifiers=限定符列表
 */
public class QualifiersLane extends ArrayList<Function<List<PrincipleSource>,PrincipleQualifiers>>{
	private static final long serialVersionUID = 744486526313474622L;

	public QualifiersLane() {}
	

	public QualifiersLane(List<Function<List<PrincipleSource>,PrincipleQualifiers>> funList) {
		this.addAll(funList);
	}
	
	public QualifiersLane(Function<List<PrincipleSource>,PrincipleQualifiers> fun) {
		this.add(fun);
	}
	public QualifiersLane(Function<List<PrincipleSource>,PrincipleQualifiers> first,Function<List<PrincipleSource>,PrincipleQualifiers> second) {
		this.add(first);
		this.add(second);
	}
	public QualifiersLane(Function<List<PrincipleSource>,PrincipleQualifiers> first,Function<List<PrincipleSource>,PrincipleQualifiers> second,Function<List<PrincipleSource>,PrincipleQualifiers> third) {
		this.add(first);
		this.add(second);
		this.add(third);
	}
	public QualifiersLane(Function<List<PrincipleSource>,PrincipleQualifiers> first,Function<List<PrincipleSource>,PrincipleQualifiers> second,Function<List<PrincipleSource>,PrincipleQualifiers> third,Function<List<PrincipleSource>,PrincipleQualifiers> fourth) {
		this.add(first);
		this.add(second);
		this.add(third);
		this.add(fourth);
	}
	public QualifiersLane(Function<List<PrincipleSource>,PrincipleQualifiers> first,Function<List<PrincipleSource>,PrincipleQualifiers> second,Function<List<PrincipleSource>,PrincipleQualifiers> third,Function<List<PrincipleSource>,PrincipleQualifiers> fourth,Function<List<PrincipleSource>,PrincipleQualifiers> fifth) {
		this.add(first);
		this.add(second);
		this.add(third);
		this.add(fourth);
		this.add(fifth);
	}

    
	public QualifiersLane addOne(Function<List<PrincipleSource>,PrincipleQualifiers> first) {
		this.add(first);
		return this;
	}
	
	public QualifiersLane add(Function<List<PrincipleSource>,PrincipleQualifiers> first,Function<List<PrincipleSource>,PrincipleQualifiers> second) {
		this.add(first);
		this.add(second);
		return this;
	}

	public QualifiersLane add(Function<List<PrincipleSource>,PrincipleQualifiers> first,Function<List<PrincipleSource>,PrincipleQualifiers> second,Function<List<PrincipleSource>,PrincipleQualifiers> third) {
		this.add(first);
		this.add(second);
		this.add(third);
		return this;
	}
	public QualifiersLane add(Function<List<PrincipleSource>,PrincipleQualifiers> first,Function<List<PrincipleSource>,PrincipleQualifiers> second,Function<List<PrincipleSource>,PrincipleQualifiers> third,Function<List<PrincipleSource>,PrincipleQualifiers> fourth) {
		this.add(first);
		this.add(second);
		this.add(third);
		this.add(fourth);
		return this;
	}

	public QualifiersLane add(Function<List<PrincipleSource>,PrincipleQualifiers> first,Function<List<PrincipleSource>,PrincipleQualifiers> second,Function<List<PrincipleSource>,PrincipleQualifiers> third,Function<List<PrincipleSource>,PrincipleQualifiers> fourth,Function<List<PrincipleSource>,PrincipleQualifiers> fifth) {
		this.add(first);
		this.add(second);
		this.add(third);
		this.add(fourth);
		this.add(fifth);
		return this;
	}

}
