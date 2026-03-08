package xyz.zhiwei.cognitivedesign.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import xyz.zhiwei.cognitivedesign.morphism.Appearance;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.container.ImagePackage;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.response.ResponsePackage;
import xyz.zhiwei.cognitivedesign.morphism.principle.source.container.PrincipleSourceLane;


/**
 * 标准流程
 * <A> 表象
 * <P> 本原
 */
public class Service {
    private static final Logger logger = LoggerFactory.getLogger(Service.class);
    
	//本原集存取接口
    private PrincipleAccessInterface setAccessImpl;
    private final ObjectMapper objectMapper;
    
    
    //构造方法
    public Service(PrincipleAccessInterface setAccessImpl,ObjectMapper objectMapper) {
    	this.setAccessImpl=setAccessImpl;
    	this.objectMapper=objectMapper;
    }
    
    
	/**
	 * 表象展示
	 * @param <A>
	 * @param <P>
	 * @param a
	 * @return
	 */
	public <A extends Appearance> A view(A a) {
		logger.info("initial a is {}:",toJson(a));
		
		//本原集获取
		List<PrincipleSourceLane> relatedCollectionList=setAccessImpl.query(a.qualifiersLanes());
		
		
		//构造
		@SuppressWarnings("unchecked")
		A preAppearance=(A) a.construct(relatedCollectionList);
		logger.info("preAppearance is {}:",toJson(preAppearance));


		return preAppearance;
	}
	
	
	/**
	 * 表象变换
	 * @param <A>
	 * @param <P>
	 * @param a
	 * @return
	 */
	public <A extends Appearance> MorphismResponse<A> process(A a){
		logger.info("initial a is {}:",toJson(a));
		//本原集获取
		List<PrincipleSourceLane> relatedCollectionList=setAccessImpl.query(a.qualifiersLanes());
		//构造
		@SuppressWarnings("unchecked")
		A preAppearance=(A) a.construct(relatedCollectionList);
		logger.info("preAppearance is {}:",toJson(preAppearance));
		
		
		
		//变换
		@SuppressWarnings("unchecked")
		A postAppearance=(A) preAppearance.transforms();
		logger.info("postAppearance is {}:",toJson(postAppearance));
		
		
		
		//解构
		ImagePackage imagePackage=postAppearance.deconstruct();
		//本原集存储
		ResponsePackage responsePackage=setAccessImpl.save(imagePackage);
		return new MorphismResponse<A>(postAppearance,responsePackage);
	}


	
	
	
	
	
	
	
	
	//日志辅助
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
        	logger.error("Service 标准流程日志，JSON序列化失败", e);
            return null; 
        }
    }
}

























