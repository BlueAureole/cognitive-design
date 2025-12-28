package xyz.zhiwei.cognitivedesign.morphism;

import java.util.List;
import java.util.function.Function;

import xyz.zhiwei.cognitivedesign.morphism.support.image.PrincipleImage;
import xyz.zhiwei.cognitivedesign.morphism.support.image.PrincipleImageResponse;
import xyz.zhiwei.cognitivedesign.morphism.support.qualifier.PrincipleQualifiers;
import xyz.zhiwei.cognitivedesign.morphism.support.source.PrincipleSource;

/**
 * 表象
 */
public interface Appearance {

	/*
	 * ================================ 关系1： 表象所关联的本原集(子集描述符) =============================
	 */

	/**
	 * 关系1：表象所关联的本原集(定义)
	 * 查询条件函数列表
	 * 每一个查询条件都可能依赖前面所有的查询结果集
	 * @return
	 */
	default List<Function<List<PrincipleSource>,PrincipleQualifiers>> qualifierFunction(){
		return List.of(this::qualifierGroup1st);
	}
	/**
	 * 关系1：表象所关联的本原集(定义)
	 * 默认的第一个查询条件（无依赖）
	 * @param principleSourceList
	 * @return
	 */
	default PrincipleQualifiers qualifierGroup1st(List<PrincipleSource> principleSourceList) {
		return new PrincipleQualifiers();
	};
	
	
	
	/*
	 * ================================ 关系2： 表象由本原集构造而成，本原集由表象拆解而来 ===================
	 */
	
	/**
	 * 关系2之一：表象由本原集构造而成
	 * 入参列表对应上面的查询函数列表
	 * @param principleSourceList
	 * @return
	 */
	default Appearance construct(List<PrincipleSource> principleSourceList){
		if(null!=principleSourceList && !principleSourceList.isEmpty()) {
			return construct(principleSourceList.getFirst());
		}
		return this;
	};
	/**
	 * 关系2之一：表象由本原集构造而成
	 * 默认的第一个入参
	 * @param principleSource
	 * @return
	 */
	default Appearance construct(PrincipleSource principleSource) {
		return this;
	};

	
	
	
	/**
	 * 关系2之二：本原集由表象拆解而来
	 * 拆解函数列表
	 * 每一个拆解函数都可能依赖前面所有的存储结果
	 * @return
	 */
	default List<Function<List<PrincipleImageResponse>,PrincipleImage>> deconstructFunction(){
		return List.of(this::deconstruct);
	}
	/**
	 * 关系2之二：本原集由表象拆解而来
	 * @return
	 */
	default PrincipleImage deconstruct(List<PrincipleImageResponse> principleImageResponseList) {
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
