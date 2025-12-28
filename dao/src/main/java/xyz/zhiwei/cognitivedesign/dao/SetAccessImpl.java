package xyz.zhiwei.cognitivedesign.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import xyz.zhiwei.cognitivedesign.morphism.Principle;
import xyz.zhiwei.cognitivedesign.morphism.support.image.PrincipleImage;
import xyz.zhiwei.cognitivedesign.morphism.support.image.PrincipleImageResponse;
import xyz.zhiwei.cognitivedesign.morphism.support.image.PrincipleImagery;
import xyz.zhiwei.cognitivedesign.morphism.support.qualifier.PrincipleQualifiers;
import xyz.zhiwei.cognitivedesign.morphism.support.source.PrincipleSource;
import xyz.zhiwei.cognitivedesign.service.SetAccessInterface;


/**
 * 本原集存取实现
 */
public class SetAccessImpl implements SetAccessInterface{
    private static final Logger log = LoggerFactory.getLogger(SetAccessImpl.class);
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SetAccessSupport setAccessSupport; 

    
	/*
	 * =================================================接口方法区===============================================================
	 */
    
    /**
     * 读取指定数据集
     * 根据子集描述符组的定义函数列表，返回子集描述符组列表所描述的数据集
     * @param qualifierFunctionList
     * @return List<RelatedCollection>
     *    结果容器List必不为null，至少返回空列表[]。
     */
    @Override
    public List<PrincipleSource> queryRelatedCollectionList(
    		List<Function<List<PrincipleSource>,PrincipleQualifiers>> qualifierFunctionList){

		List<PrincipleQualifiers> qualifierGroupList=new ArrayList<>();
		List<PrincipleSource> relatedCollectionList=new ArrayList<>();
		
		if(null!=qualifierFunctionList && !qualifierFunctionList.isEmpty()) {
			for (int i = 0; i < qualifierFunctionList.size(); i++) {
			    Function<List<PrincipleSource>, PrincipleQualifiers> qualifierFunction = qualifierFunctionList.get(i);
				PrincipleQualifiers qualifierGroup=qualifierFunction.apply(relatedCollectionList);
				qualifierGroupList.add(qualifierGroup);
				PrincipleSource relatedCollection=setAccessSupport.queryRelatedCollection(qualifierGroup);
				relatedCollectionList.add(relatedCollection);
			}
		}
		//查询完成后，统一打印查询结果
		log.info("PrincipleSourceQualifiersList is: {}",toJson(qualifierGroupList));
		log.info("PrincipleSourceList is: {}",toJson(relatedCollectionList));
		return relatedCollectionList;
    }
    
    
    
    
    
	/**
	 * 存储相关数据集
	 * 根据拆解函数列表，返回所有拆解目标的保存结果
	 * @param <P>
	 * @param deconstructFunctionList
	 * @return
	 */
	@Override
	public List<PrincipleImageResponse> saveRelatedSegmentsList(
			List<Function<List<PrincipleImageResponse>, PrincipleImage>> deconstructFunctionList) {
		
		List<PrincipleImageResponse> saveResponseList=new ArrayList<>();
		
		if(null!=deconstructFunctionList && !deconstructFunctionList.isEmpty()) {
			for (int i = 0; i < deconstructFunctionList.size(); i++) {
				Function<List<PrincipleImageResponse>, PrincipleImage> deconstructFunction = deconstructFunctionList.get(i);
				PrincipleImage relatedSegments=deconstructFunction.apply(saveResponseList);
				
				printPrincipleImage(relatedSegments);//写入前打印内容
				
                PrincipleImageResponse saveResponse = null;
                try {
                	// 调用代理对象的事务方法，而非 this.saveRelatedSegments
                    saveResponse = setAccessSupport.saveRelatedSegments(relatedSegments);
                } catch (Exception e) {
                    // 异常处理：仅回滚当次事务，不中断后续循环
                    log.error("第{}条拆解函数处理失败，仅回滚当次操作，异常信息：{}", i, e.getMessage(), e);
                    // 保留原有逻辑：异常时添加空响应（或根据业务调整）
                    saveResponse = new PrincipleImageResponse();
                }
				log.info("PrincipleImageResponseList {} is: {}",i,toJson(saveResponse));//立即打印结果
				saveResponseList.add(saveResponse);
			}
		}

		return saveResponseList;
	}
    

	/*
	 * ==================================日志 辅助工具区==============================
	 */

    /**
     * 打印PrincipleImage数据（JSON格式），识别PrincipleImagery并打印describe
     *
     * @param relatedSegments 要打印的原则图片数据
     */
    private void printPrincipleImage(PrincipleImage relatedSegments) {
        // 空值校验
        if (relatedSegments == null) {
            log.info("【PrincipleImage】数据为空");
            return;
        }

        log.info("============= 开始打印PrincipleImage =============");
        
        // 打印新增数据
        printJsonWithDescribe("新增(addition)", relatedSegments.getAddition());
        // 打印修改数据
        printJsonWithDescribe("修改(modification)", relatedSegments.getModification());
        // 打印删除数据
        printJsonWithDescribe("删除(deletion)", relatedSegments.getDeletion());
        
        log.info("============= 结束打印PrincipleImage =============");
    }

    /**
     * 核心方法：打印JSON + 识别PrincipleImagery并输出describe
     *
     * @param type 数据类型（新增/修改/删除）
     * @param dataList 要打印的嵌套列表数据
     */
    private void printJsonWithDescribe(String type, List<List<? extends Principle<?>>> dataList) {
        // 空值/空列表校验
        if (Objects.isNull(dataList) || dataList.isEmpty()) {
            log.info("【{}】无数据", type);
            return;
        }

        // 构建日志内容：先打印describe（如果有），再打印JSON
        StringBuilder logContent = new StringBuilder();
        logContent.append("【").append(type).append("】\n");

        // 遍历外层列表，识别PrincipleImagery并提取describe
        for (int i = 0; i < dataList.size(); i++) {
            List<? extends Principle<?>> innerList = dataList.get(i);
            if (innerList instanceof PrincipleImagery<?>) {
                String describe = ((PrincipleImagery<?>) innerList).getDescribe();
                logContent.append("  第").append(i ).append("组描述: ")
                          .append(Objects.isNull(describe) ? "无描述" : describe).append("\n");
            }
        }

        // 序列化JSON（捕获异常，避免序列化失败导致程序中断）
        String jsonStr;
        try {
            jsonStr = objectMapper.writeValueAsString(dataList);
        } catch (JsonProcessingException e) {
            jsonStr = "JSON序列化失败: " + e.getMessage();
            log.warn("【{}】数据JSON序列化异常", type, e);
        }

        // 拼接JSON并打印
        logContent.append("  数据(JSON格式): \n").append(jsonStr);
        log.info(logContent.toString());
    }
    
    
    
    
    
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
        	log.error("JSON序列化失败", e);
            return null; 
        }
    }
    
}
