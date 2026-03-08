package xyz.zhiwei.cognitivedesign.dao.accessimpl.write;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import xyz.zhiwei.cognitivedesign.morphism.Principle;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.container.PrincipleImage;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.container.PrincipleImagery;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.response.ImageResponse;

/**
 * 日志打印支持
 */
public class WriteAccessLog {
    private static final Logger log = LoggerFactory.getLogger(WriteAccessLog.class);
    private static ObjectMapper objectMapper = new ObjectMapper();
    private static int maxPrintSize=10;

	

    /*
     * ============================映象入参=============================
     */

	/**
	 * 映象日志
	 * @param laneIndex
	 * @param batchIndex
	 * @param principleImage
	 */
    public static void image(int txGroupIndex,int laneIndex,int batchIndex, PrincipleImage principleImage) {
    	log.info(forImage(txGroupIndex,laneIndex,batchIndex,principleImage));
    }

    private static String forImage(int txGroupIndex,int laneIndex,int batchIndex, PrincipleImage principleImage) {
        StringBuffer logBuffer = new StringBuffer();
        if(-1==txGroupIndex) {
            logBuffer.append(String.format("%n==================== 非事务组 第%d号泳道 映象批次 %d ",laneIndex, batchIndex));
        }else {
            logBuffer.append(String.format("%n==================== 事务组%d 第%d号泳道 映象批次 %d ",txGroupIndex, laneIndex, batchIndex));
        }

        if (principleImage == null) {
            return String.format("%n principleImage is null");
        }
        if (principleImage.isEmpty()) {
            return String.format("%n principleImage isEmpty");
        }

        for (int groupIndex = 0; groupIndex < principleImage.size(); groupIndex++) {
            PrincipleImagery<?> group = principleImage.get(groupIndex);
            String desc = (null==group ? null : group.getDescribe());
            logBuffer.append(String.format("%n---------- 映象集 %d 描述: %s", groupIndex,desc));

            if (group == null) {
                continue;
            }

            int totalSize = group.size();
            int maxPrint = maxPrintSize;
            int printSize = Math.min(totalSize, maxPrint);

            for (int itemIndex = 0; itemIndex < printSize; itemIndex++) {
                Principle<?> principle = group.get(itemIndex);
                String principleJson;
                try {
                    principleJson = objectMapper.writeValueAsString(principle);
                } catch (JsonProcessingException e) {
                    principleJson = String.format("  映象 %d 序列化失败：%s，原始对象：%s",
                            itemIndex, e.getMessage(), principle);
                }
                logBuffer.append(String.format("%n"));
                logBuffer.append(principleJson);
            }

            if (totalSize > maxPrint) {
                logBuffer.append(String.format(
                        "%n  映象总数 %d，日志仅输出前 %d 条",
                        totalSize, maxPrint));
            }
        }

        return logBuffer.toString();
    }

    /*
     * ============================返回结果=============================
     */

    /**
     * 返回值日志
     * @param laneIndex
     * @param batchIndex
     * @param imageResponse
     */
    public static void resp(int txGroupIndex,int laneIndex,int batchIndex, ImageResponse imageResponse) {	
        if (imageResponse == null || imageResponse.isEmpty()) {
            if(-1==txGroupIndex) {
                log.info(String.format("%n==================== 非事务组 第%d号泳道 映象批次 %d 返回结果为空 ",laneIndex, batchIndex));
            }else {
                log.info(String.format("%n==================== 事务组%d 第%d号泳道 映象批次 %d 返回结果为空 ",txGroupIndex,laneIndex, batchIndex));
            }
            return;
        }

        StringBuffer logBuffer = new StringBuffer();
        if(-1==txGroupIndex) {
            logBuffer.append(String.format("%n==================== 非事务组 第%d号泳道 映象批次 %d 返回结果 ",laneIndex, batchIndex));
        }else {
            logBuffer.append(String.format("%n==================== 事务组%d 第%d号泳道 映象批次 %d 返回结果 ",txGroupIndex,laneIndex, batchIndex));
        }

        imageResponse.forEach((index, count) -> {
            logBuffer.append(String.format("%n"));
            logBuffer.append(String.format(" ----映象集 %d 响应数：%d", index, count));
        });

        log.info(logBuffer.toString());
    } 
}
