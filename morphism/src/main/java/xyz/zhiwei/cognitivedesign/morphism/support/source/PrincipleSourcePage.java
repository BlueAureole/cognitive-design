package xyz.zhiwei.cognitivedesign.morphism.support.source;

import java.util.ArrayList;
import java.util.List;

import xyz.zhiwei.cognitivedesign.morphism.Principle;

/**
 * 原象页
 * 简化自com.github.pagehelper.Page
 * @param <E>
 */
public class PrincipleSourcePage<P extends Principle<?>> extends ArrayList<P>{
    private static final long serialVersionUID = 1L;
    /**
     * 总数
     */
    private Long total;
    
	public Long getTotal() {
		return total;
	}
	public void setTotal(Long total) {
		this.total = total;
	}

	public PrincipleSourcePage(){}

	public PrincipleSourcePage(List<P> list){
		if(null!=list && list.size()>0) {
			list.stream().forEach(e-> add(e));
			this.total=Long.valueOf(list.size());
		}else {
			this.total=0L;
		}
		
	}
	
	public PrincipleSourcePage(List<P> list,Long total){
		if(null!=list && list.size()>0) {
			list.stream().forEach(e-> add(e));
		}
		this.total=total;
	}
}
