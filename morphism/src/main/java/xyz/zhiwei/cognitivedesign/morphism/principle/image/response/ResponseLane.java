package xyz.zhiwei.cognitivedesign.morphism.principle.image.response;

import java.util.ArrayList;
import java.util.List;

/**
 * 泳道响应
 */
public class ResponseLane extends ArrayList<ImageResponse>{
	private static final long serialVersionUID = 1L;

	public ResponseLane(List<ImageResponse> list) {
		this.addAll(list);
	}
	
}
