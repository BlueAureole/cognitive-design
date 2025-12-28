package xyz.zhiwei.cognitivedesign.morphism.support.qualifier;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import xyz.zhiwei.cognitivedesign.morphism.Principle;


/**
 * 本原限定符-可扩展
 */
public class PrincipleQualifier<P extends Principle<?>> {
	
	private final Object lock = new Object();
	private volatile Class<P> principleClazz;
	
	private String describe;
	
	private P similar;
	private List<P> similarList;
	
	private P rangeStart;
	private P rangeEnd;

	/*
	 * 语义同String.substring(int beginIndex, int endIndex): 
	 * 左闭右开[beginIndex, endIndex)
	 */
	private Long limitStart;
	private Long limitEnd;

    
    // 1. 显式传入Class对象的构造函数
    public PrincipleQualifier(Class<P> principleClazz) {
        this.principleClazz = principleClazz;
    }
    
    // 2. 通过实例对象构造（如果入参只有P，且P是t的父类，获取泛型时将返回t类型-P的子类）
    public PrincipleQualifier(Class<P> principleClazz,P similar) {
        this.principleClazz = principleClazz;
        this.similar = similar;
    }
    public PrincipleQualifier(Class<P> principleClazz,P similar,Integer pageNum,Integer pageSize) {
        this.principleClazz = principleClazz;
        this.similar = similar;        
        if (pageNum != null && pageSize != null && pageNum > 0 && pageSize > 0) { // 增加页码/页数合法性校验
            long offset = (long) (pageNum - 1) * pageSize;
            this.limitStart = offset;
            this.limitEnd = offset + pageSize; // 左闭右开：end = start + 行数
        } else {
            // 默认取前10条：[0,10)
            this.limitStart = 0L;
            this.limitEnd = 10L;
        }
    }
    public PrincipleQualifier(Class<P> principleClazz,List<P> similarList) {
        this.principleClazz = principleClazz;
        this.similarList = similarList;
    }
    

    
    
    
    // 3. 无参构造函数（仅供直属子类使用）
    protected PrincipleQualifier() {}
    
    
    
    
    
    
    /**
     * 因为service要根据这个泛型来获取dao实现类，所以才有如此复杂的泛型获取方法。
     * @return
     */
    @SuppressWarnings("unchecked")
	public Class<P> getPrincipleClazz() {
        if (principleClazz == null) {
            synchronized (lock) {
                if (principleClazz == null) {
                	
                    // 尝试从第一个非空的P类型实例中获取泛型
                    P nonNullInstance = null;
                    if (similar != null) {
                        nonNullInstance = similar;
                    }else if (similarList != null && !similarList.isEmpty()) {
                        for (P item : similarList) {
                            if (item != null) {
                                nonNullInstance = item;
                                break; 
                            }
                        }
                    }else if (rangeStart != null) {
                        nonNullInstance = rangeStart;
                    }else if (rangeEnd != null) {
                        nonNullInstance = rangeEnd;
                    }
                    
                    if (nonNullInstance != null) {
                    	principleClazz= (Class<P>) nonNullInstance.getClass();
                    	return principleClazz;
                    }
                    
                    // 尝试从子类中获取泛型
                    Type genericSuper = this.getClass().getGenericSuperclass();
                    if (genericSuper instanceof ParameterizedType) {
                        ParameterizedType paramType = (ParameterizedType) genericSuper;
                        Type actualType = paramType.getActualTypeArguments()[0];
                        
                        if (actualType instanceof Class) {
                        	principleClazz= (Class<P>) actualType;
                        	return principleClazz;
                        }
                    }
                    throw new IllegalStateException("无法解析泛型类型，请显式指定Class对象");
                }
            }
        }
        return principleClazz;
    }
    
    
    // 计算并返回int类型的pageSize（核心：左闭右开，页数=end - start）
    public int getPageSize() {
        if (limitStart == null || limitEnd == null || limitEnd < limitStart) {
            return 0; // 空值/非法值返回0
        }
        long size = limitEnd - limitStart;
        // 防止超出int范围，同时保证非负
        return size > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0, size);
    }

    // 计算并返回int类型的pageNum（基于正确的pageSize和offset计算）
    public int getPageNum() {
        int pageSize = getPageSize();
        if (limitStart == null || pageSize <= 0) {
            return 1; // 异常情况默认第1页
        }
        // 页码从1开始：offset / pageSize + 1（整除/非整除都适用，因为offset是(pageNum-1)*pageSize）
        long pageNumLong = (limitStart / pageSize) + 1;
        // 防止超出int范围，默认返回1
        return pageNumLong > Integer.MAX_VALUE ? 1 : (int) pageNumLong;
    }


	
	public P getSimilar() {
		return similar;
	}
	public void setSimilar(P similar) {
		this.similar = similar;
	}
	public List<P> getSimilarList() {
		return similarList;
	}
	public void setSimilarList(List<P> similarList) {
		this.similarList = similarList;
	}
	public P getRangeStart() {
		return rangeStart;
	}
	public void setRangeStart(P rangeStart) {
		this.rangeStart = rangeStart;
	}
	public P getRangeEnd() {
		return rangeEnd;
	}
	public void setRangeEnd(P rangeEnd) {
		this.rangeEnd = rangeEnd;
	}
	public Long getLimitStart() {
		return limitStart;
	}
	public void setLimitStart(Long limitStart) {
		this.limitStart = limitStart;
	}
	public Long getLimitEnd() {
		return limitEnd;
	}
	public void setLimitEnd(Long limitEnd) {
		this.limitEnd = limitEnd;
	}

	public String getDescribe() {
		return describe;
	}

	public void setDescribe(String describe) {
		this.describe = describe;
	}


	

}
