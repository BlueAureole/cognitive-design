package xyz.zhiwei.cognitivedesign.morphism;

import java.util.Arrays;
import java.util.List;

import xyz.zhiwei.cognitivedesign.morphism.principle.image.container.ImageLane;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.container.ImageLaneGroup;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.container.ImagePackage;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.container.PrincipleImage;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.response.ImageResponse;
import xyz.zhiwei.cognitivedesign.morphism.principle.source.container.PrincipleSource;
import xyz.zhiwei.cognitivedesign.morphism.principle.source.container.PrincipleSourceLane;
import xyz.zhiwei.cognitivedesign.morphism.principle.source.qualifier.PrincipleQualifiers;
import xyz.zhiwei.cognitivedesign.morphism.principle.source.qualifier.QualifiersLane;

/**
 * 表象
 */
public interface Appearance {

	/*
	 * ================================ 关系1： 表象所关联的本原集(子集描述符) =============================
	 */

	/**
	 * 关系1：表象所关联的本原集(定义)
	 * 查询泳道列表
	 * @return
	 */
	default List<QualifiersLane> qualifiersLanes(){
		return Arrays.asList(new QualifiersLane(this::qualifiersLaneA1st));
	}
	
	/**
	 * 关系1：表象所关联的本原集(定义)
	 * 默认的第一个查询条件（无依赖）
	 * @param principleSourceList
	 * @return
	 */
	default PrincipleQualifiers qualifiersLaneA1st(List<PrincipleSource> principleSourceList) {
		return new PrincipleQualifiers();
	};
	
	
	
	/*
	 * ================================ 关系2： 表象由本原集构造而成，本原集由表象拆解而来 ===================
	 */
	
	/**
	 * 关系2之一：表象由本原集构造而成
	 * 入参列表对应上面的查询泳道列表
	 * @param principleSourceList
	 * @return
	 */
	default Appearance construct(List<PrincipleSourceLane> principleSourceLaneList){
		return this;
	};
	
	/**
	 * 关系2之二：本原集由表象拆解而来
	 * @return 映象包
	 */
	default ImagePackage deconstruct(){
		return new ImagePackage(new ImageLaneGroup(new ImageLane(this::deconstructLaneA1st)));
	}
	
	/**
	 * 关系2之二：本原集由表象拆解而来
	 * @return
	 */
	default PrincipleImage deconstructLaneA1st(List<ImageResponse> ImageResponseList) {
		return new PrincipleImage();
	};
	
	

	/*
	 * ================================ 关系3： 表象变化实质是本原集变化(增删改) =============================
	 */
	
	
	/**
	 * 关系3：表象变化实质是本原集变化
	 */
	default Appearance transforms() {
		return this;
	};
	

}
