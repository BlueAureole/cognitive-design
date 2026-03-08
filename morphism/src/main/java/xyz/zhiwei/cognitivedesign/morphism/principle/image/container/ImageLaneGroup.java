package xyz.zhiwei.cognitivedesign.morphism.principle.image.container;

import java.util.ArrayList;

/**
 * 映象泳道组=事务组
 */
public class ImageLaneGroup extends ArrayList<ImageLane>{
	private static final long serialVersionUID = 1L;

	

	public ImageLaneGroup() {}
	
	public ImageLaneGroup(ImageLane frist) {
		this.add(frist);
	}
	public ImageLaneGroup(ImageLane first,ImageLane second) {
		this.add(first);
		this.add(second);
	}
	public ImageLaneGroup(ImageLane first,ImageLane second,ImageLane third) {
		this.add(first);
		this.add(second);
		this.add(third);
	}
	public ImageLaneGroup(ImageLane first,ImageLane second,ImageLane third,ImageLane fourth) {
		this.add(first);
		this.add(second);
		this.add(third);
		this.add(fourth);
	}
	public ImageLaneGroup(ImageLane first,ImageLane second,ImageLane third,ImageLane fourth,ImageLane fifth) {
		this.add(first);
		this.add(second);
		this.add(third);
		this.add(fourth);
		this.add(fifth);
	}

    
	public ImageLaneGroup addOne(ImageLane first) {
		this.add(first);
		return this;
	}
	
	public ImageLaneGroup add(ImageLane first,ImageLane second) {
		this.add(first);
		this.add(second);
		return this;
	}

	public ImageLaneGroup add(ImageLane first,ImageLane second,ImageLane third) {
		this.add(first);
		this.add(second);
		this.add(third);
		return this;
	}
	public ImageLaneGroup add(ImageLane first,ImageLane second,ImageLane third,ImageLane fourth) {
		this.add(first);
		this.add(second);
		this.add(third);
		this.add(fourth);
		return this;
	}

	public ImageLaneGroup add(ImageLane first,ImageLane second,ImageLane third,ImageLane fourth,ImageLane fifth) {
		this.add(first);
		this.add(second);
		this.add(third);
		this.add(fourth);
		this.add(fifth);
		return this;
	}
	
	
}
