package xyz.zhiwei.cognitivedesign.service;

import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import xyz.zhiwei.cognitivedesign.morphism.Appearance;
import xyz.zhiwei.cognitivedesign.morphism.support.image.PrincipleImage;
import xyz.zhiwei.cognitivedesign.morphism.support.image.PrincipleImageResponse;
import xyz.zhiwei.cognitivedesign.morphism.support.source.PrincipleSource;


/**
 * 标准流程
 * <A> 表象
 * <P> 本原
 */
public class Service {
    private static final Logger logger = LoggerFactory.getLogger(Service.class);
    private final ObjectMapper objectMapper;
	

	//本原集存取接口
    private SetAccessInterface setAccessImpl;
    
    
    //构造方法
    public Service(SetAccessInterface setAccessImpl,ObjectMapper objectMapper) {
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
		List<PrincipleSource> relatedCollectionList=setAccessImpl.queryRelatedCollectionList(a.qualifierFunction());
		
		
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
		List<PrincipleSource> relatedCollectionList=setAccessImpl.queryRelatedCollectionList(a.qualifierFunction());
		//构造
		@SuppressWarnings("unchecked")
		A preAppearance=(A) a.construct(relatedCollectionList);
		logger.info("preAppearance is {}:",toJson(preAppearance));
		
		
		
		//变换
		@SuppressWarnings("unchecked")
		A postAppearance=(A) preAppearance.transforms();
		logger.info("postAppearance is {}:",toJson(postAppearance));
		
		
		
		//解构
		List<Function<List<PrincipleImageResponse>,PrincipleImage>> deconstructFunctionList=postAppearance.deconstructFunction();
		//本原集存储
		List<PrincipleImageResponse> saveResponseList=setAccessImpl.saveRelatedSegmentsList(deconstructFunctionList);
		return new MorphismResponse<A>(postAppearance,saveResponseList);
	}


	
	
	
	
	
	
	
	
	//日志辅助
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
        	logger.error("JSON序列化失败", e);
            return null; 
        }
    }
}

























