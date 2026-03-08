package xyz.zhiwei.cognitivedesign.service;

import java.util.List;

import xyz.zhiwei.cognitivedesign.morphism.principle.image.container.ImagePackage;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.response.ResponsePackage;
import xyz.zhiwei.cognitivedesign.morphism.principle.source.container.PrincipleSourceLane;
import xyz.zhiwei.cognitivedesign.morphism.principle.source.qualifier.QualifiersLane;



/**
 * 本原集存取接口
 */
public interface PrincipleAccessInterface{
	
	
	
    /**
     * 读取指定数据集
     * 根据查询泳道列表，返回各泳道的查询结果集。
     * 根据子集描述符组的定义函数列表，返回子集描述符组列表所描述的数据集
     * @param qualifiersLaneList
     * @return
     */
    public List<PrincipleSourceLane> query(List<QualifiersLane> qualifiersLaneList);
    
    
    
    
    

	
	/**
	 * 存储相关数据集
	 * 
	 * @param imagePackage
	 * @return
	 */
	public ResponsePackage save(ImagePackage imagePackage);
    
    
    
    
	
}
