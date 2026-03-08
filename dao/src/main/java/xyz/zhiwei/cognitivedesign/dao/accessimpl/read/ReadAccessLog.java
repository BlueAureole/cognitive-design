package xyz.zhiwei.cognitivedesign.dao.accessimpl.read;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import xyz.zhiwei.cognitivedesign.morphism.Principle;
import xyz.zhiwei.cognitivedesign.morphism.principle.source.container.PrincipleSource;
import xyz.zhiwei.cognitivedesign.morphism.principle.source.qualifier.PrincipleQualifier;
import xyz.zhiwei.cognitivedesign.morphism.principle.source.qualifier.PrincipleQualifiers;


/**
 * 日志打印支持
 */
public class ReadAccessLog {
    private static final Logger log = LoggerFactory.getLogger(ReadAccessLog.class);
    private static ObjectMapper objectMapper = new ObjectMapper();
    private static int maxPrintSize=10;
    
	/**
	 * 限定符日志
	 * @param laneIndex 泳道序号
	 * @param groupIndex 批次序号
	 * @param qualifierGroupList
	 */
    public static void qualifiers(int laneIndex,int batchIndex,PrincipleQualifiers qualifierGroup) {
		log.info(forQualifiers(laneIndex,batchIndex,qualifierGroup));
    }
	private static String forQualifiers(int laneIndex,int batchIndex,PrincipleQualifiers qualifiersGroup) {
	    // 初始化线程安全的字符串缓冲区（日志方法可能被多线程调用）
	    StringBuffer logBuffer = new StringBuffer();
	    
	        logBuffer.append(String.format("%n==================== 第%d号泳道 限定符批次 %d ", laneIndex, batchIndex));
	        
	        // 4. 处理当前分组为null的边界情况
	        if (qualifiersGroup == null) {
	            return String.format("%n qualifiersGroup is null");
	        }
	        // 5. 处理当前分组为空的情况
	        if (qualifiersGroup.isEmpty()) {
	            return String.format("%n qualifiersGroup isEmpty");
	        }
	        
	        // 6. 遍历分组内的每个 PrincipleQualifier，每个单独一行
	        for (int qualifierIndex = 0; qualifierIndex < qualifiersGroup.size(); qualifierIndex++) {
	            PrincipleQualifier<?> qualifier = qualifiersGroup.get(qualifierIndex);
	            String qualifierJson;
	            try {
	                // 将资格器对象转为格式化的JSON（带缩进，易读）
	                qualifierJson = objectMapper.writeValueAsString(qualifier);
	            } catch (JsonProcessingException e) {
	                // 序列化失败时的兜底日志，避免整体异常
	                qualifierJson = String.format("第%d个限定符序列化失败：%s，原始对象：%s",
	                        qualifierIndex, e.getMessage(), qualifier);
	            }
	            // 每个资格器单独一行，添加序号便于定位
	            logBuffer.append(String.format("%n"));
	            logBuffer.append(qualifierJson);
	        }

	    return logBuffer.toString();
	}
	
	
	/**
	 * 原象集日志
	 * @param laneIndex 泳道序号
	 * @param batchIndex 批次序号
	 * @param relatedCollection
	 */
	public static void source(int laneIndex,int batchIndex,PrincipleSource relatedCollection) {
		log.info(forSource(laneIndex,batchIndex,relatedCollection));
	}
	/**
	 * 查询结果
	 * 分层打印 PrincipleSource 相关列表的日志方法
	 * 层级规则：
	 * - 每个 PrincipleSource → 一批
	 * - 批内每个 List<? extends Principle<?>> → 一组
	 * - 组内每个 Principle<?> → 单独一行
	 */
	private static String forSource(int laneIndex,int batchIndex,PrincipleSource principleSource) {

	    // 初始化线程安全的字符串缓冲区（静态方法可能被多线程调用）
	    StringBuffer logBuffer = new StringBuffer();
	
        logBuffer.append(String.format("%n==================== 第%d号泳道 本原集批次 %d ", laneIndex, batchIndex));

        // 处理当前批次为null的边界情况
        if (principleSource == null) {
            return String.format("%n principleSource is null");
        }
        // 处理当前批次为空的情况
        if (principleSource.isEmpty()) {
            return String.format("%n principleSource isEmpty");
        }

        // 4. 中层遍历：批次内每个 List<? extends Principle<?>> 作为一组
        for (int groupIndex = 0; groupIndex < principleSource.size(); groupIndex++) {
            List<? extends Principle<?>> principleList = principleSource.get(groupIndex);
            // 添加分组标题，明确组编号
            logBuffer.append(String.format("%n---------- 本原集 %d ", groupIndex));

            // 处理当前分组为null的边界情况
            if (principleList == null) {
                continue;
            }
            // 处理当前分组为空的情况
            if (principleList.isEmpty()) {
                continue;
            }

            int totalSize = principleList.size();
            int maxPrint = maxPrintSize;
            int printSize = Math.min(totalSize, maxPrint);

            // 5. 内层遍历：分组内每个 Principle<?> 单独一行
            for (int itemIndex = 0; itemIndex < printSize; itemIndex++) {
                Principle<?> principle = principleList.get(itemIndex);
                String principleJson;
                try {
                    // 将 Principle 对象转为格式化JSON（带缩进，提升可读性）
                    principleJson = objectMapper.writeValueAsString(principle);
                } catch (JsonProcessingException e) {
                    // 序列化失败时兜底，避免整体方法抛异常
                    principleJson = String.format("  本原 %d 序列化失败：%s，原始对象：%s",
                            itemIndex, e.getMessage(), principle);
                }
	            logBuffer.append(String.format("%n"));
                logBuffer.append(principleJson);
            }

            if (totalSize > maxPrint) {
                logBuffer.append(String.format(
                        "%n  本原总数 %d，日志仅输出前 %d 条",
                        totalSize, maxPrint));
            }
        }

	    return logBuffer.toString();
	}
	
	

}
