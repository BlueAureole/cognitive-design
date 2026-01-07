package xyz.zhiwei.cognitivedesign.morphism.support.image;

import java.util.ArrayList;
import java.util.List;

import xyz.zhiwei.cognitivedesign.morphism.Principle;

/**
 * 事务标识组列表
 */
public class TransactionGroupList extends ArrayList<List<List<? extends Principle<?>>>>{
	private static final long serialVersionUID = 1L;
	
	
	public TransactionGroupList() {}

	
	/*
	 * ==============方便的构造方法==========
	 */
	
	public TransactionGroupList(List<List<? extends Principle<?>>> first) {
		this.add(first);
	}

	public TransactionGroupList(List<List<? extends Principle<?>>> first,List<List<? extends Principle<?>>> second) {
		this.add(first);
		this.add(second);
	}
	
    // 3个参数的构造方法
    public TransactionGroupList(List<List<? extends Principle<?>>> first, List<List<? extends Principle<?>>> second,
                                List<List<? extends Principle<?>>> third) {
        this.add(first);
        this.add(second);
        this.add(third);
    }

    // 4个参数的构造方法
    public TransactionGroupList(List<List<? extends Principle<?>>> first, List<List<? extends Principle<?>>> second,
                                List<List<? extends Principle<?>>> third, List<List<? extends Principle<?>>> fourth) {
        this.add(first);
        this.add(second);
        this.add(third);
        this.add(fourth);
    }

    // 5个参数的构造方法
    public TransactionGroupList(List<List<? extends Principle<?>>> first, List<List<? extends Principle<?>>> second,
                                List<List<? extends Principle<?>>> third, List<List<? extends Principle<?>>> fourth,
                                List<List<? extends Principle<?>>> fifth) {
        this.add(first);
        this.add(second);
        this.add(third);
        this.add(fourth);
        this.add(fifth);
    }
    

	public TransactionGroupList addOne(List<List<? extends Principle<?>>> first) {
		this.add(first);
		return this;
	}
	
	public TransactionGroupList add(List<List<? extends Principle<?>>> first,List<List<? extends Principle<?>>> second) {
		this.add(first);
		this.add(second);
		return this;
	}

    // 3个参数
    public TransactionGroupList add(List<List<? extends Principle<?>>> first, List<List<? extends Principle<?>>> second,
                                    List<List<? extends Principle<?>>> third) {
        this.add(first);
        this.add(second);
        this.add(third);
        return this;
    }

    // 4个参数
    public TransactionGroupList add(List<List<? extends Principle<?>>> first, List<List<? extends Principle<?>>> second,
                                    List<List<? extends Principle<?>>> third, List<List<? extends Principle<?>>> fourth) {
        this.add(first);
        this.add(second);
        this.add(third);
        this.add(fourth);
        return this;
    }

    // 5个参数
    public TransactionGroupList add(List<List<? extends Principle<?>>> first, List<List<? extends Principle<?>>> second,
                                    List<List<? extends Principle<?>>> third, List<List<? extends Principle<?>>> fourth,
                                    List<List<? extends Principle<?>>> fifth) {
        this.add(first);
        this.add(second);
        this.add(third);
        this.add(fourth);
        this.add(fifth);
        return this;
    }
}
