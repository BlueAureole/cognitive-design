package xyz.zhiwei.cognitivedesign.service;

import java.util.List;
import java.util.function.Function;

import xyz.zhiwei.cognitivedesign.morphism.support.image.PrincipleImage;
import xyz.zhiwei.cognitivedesign.morphism.support.image.PrincipleImageResponse;
import xyz.zhiwei.cognitivedesign.morphism.support.qualifier.PrincipleQualifiers;
import xyz.zhiwei.cognitivedesign.morphism.support.source.PrincipleSource;



/**
 * 本原集存取接口
 */
public interface SetAccessInterface{
	
	
	
    /**
     * 读取指定数据集
     * 根据子集描述符组的定义函数列表，返回子集描述符组列表所描述的数据集
     * @param qualifierFunctionList
     * @return
     */
    public List<PrincipleSource> queryRelatedCollectionList(
    		List<Function<List<PrincipleSource>,PrincipleQualifiers>> qualifierFunctionList);
    
	/**
	 * 存储相关数据集
	 * 根据拆解函数列表，返回所有拆解目标的保存结果
	 * @param deconstructFunctionList
	 * @return
	 */
	public List<PrincipleImageResponse> saveRelatedSegmentsList(
			List<Function<List<PrincipleImageResponse>,PrincipleImage>> deconstructFunctionList);
    
    
    
    
	
}
