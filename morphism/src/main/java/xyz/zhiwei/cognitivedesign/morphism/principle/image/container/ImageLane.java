package xyz.zhiwei.cognitivedesign.morphism.principle.image.container;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import xyz.zhiwei.cognitivedesign.morphism.principle.image.response.ImageResponse;

/**
 * 映像集泳道
 */
public class ImageLane extends ArrayList<Function<List<ImageResponse>,PrincipleImage>>{
	private static final long serialVersionUID = 1L;

	

	public ImageLane() {}
	

	public ImageLane(List<Function<List<ImageResponse>,PrincipleImage>> funList) {
		this.addAll(funList);
	}
	
	public ImageLane(Function<List<ImageResponse>,PrincipleImage> fun) {
		this.add(fun);
	}
	public ImageLane(Function<List<ImageResponse>,PrincipleImage> first,Function<List<ImageResponse>,PrincipleImage> second) {
		this.add(first);
		this.add(second);
	}
	public ImageLane(Function<List<ImageResponse>,PrincipleImage> first,Function<List<ImageResponse>,PrincipleImage> second,Function<List<ImageResponse>,PrincipleImage> third) {
		this.add(first);
		this.add(second);
		this.add(third);
	}
	public ImageLane(Function<List<ImageResponse>,PrincipleImage> first,Function<List<ImageResponse>,PrincipleImage> second,Function<List<ImageResponse>,PrincipleImage> third,Function<List<ImageResponse>,PrincipleImage> fourth) {
		this.add(first);
		this.add(second);
		this.add(third);
		this.add(fourth);
	}
	public ImageLane(Function<List<ImageResponse>,PrincipleImage> first,Function<List<ImageResponse>,PrincipleImage> second,Function<List<ImageResponse>,PrincipleImage> third,Function<List<ImageResponse>,PrincipleImage> fourth,Function<List<ImageResponse>,PrincipleImage> fifth) {
		this.add(first);
		this.add(second);
		this.add(third);
		this.add(fourth);
		this.add(fifth);
	}

    
	public ImageLane addOne(Function<List<ImageResponse>,PrincipleImage> first) {
		this.add(first);
		return this;
	}
	
	public ImageLane add(Function<List<ImageResponse>,PrincipleImage> first,Function<List<ImageResponse>,PrincipleImage> second) {
		this.add(first);
		this.add(second);
		return this;
	}

	public ImageLane add(Function<List<ImageResponse>,PrincipleImage> first,Function<List<ImageResponse>,PrincipleImage> second,Function<List<ImageResponse>,PrincipleImage> third) {
		this.add(first);
		this.add(second);
		this.add(third);
		return this;
	}
	public ImageLane add(Function<List<ImageResponse>,PrincipleImage> first,Function<List<ImageResponse>,PrincipleImage> second,Function<List<ImageResponse>,PrincipleImage> third,Function<List<ImageResponse>,PrincipleImage> fourth) {
		this.add(first);
		this.add(second);
		this.add(third);
		this.add(fourth);
		return this;
	}

	public ImageLane add(Function<List<ImageResponse>,PrincipleImage> first,Function<List<ImageResponse>,PrincipleImage> second,Function<List<ImageResponse>,PrincipleImage> third,Function<List<ImageResponse>,PrincipleImage> fourth,Function<List<ImageResponse>,PrincipleImage> fifth) {
		this.add(first);
		this.add(second);
		this.add(third);
		this.add(fourth);
		this.add(fifth);
		return this;
	}
	
	
}
