package xyz.zhiwei.cognitivedesign.dao;

import java.util.List;

import xyz.zhiwei.cognitivedesign.morphism.Principle;
import xyz.zhiwei.cognitivedesign.morphism.support.qualifier.PrincipleQualifier;





/**
 * 本原集存取
 * @updateBy zhanghaiting
 * @param <P>
 */
public interface Dao<P extends Principle<?>> {

    public List<P> subCollection(PrincipleQualifier<P> qualifier);
    
    public Long add(List<P> list);
    
    public Long update(List<P> list);
    
    public Long delete(List<P> list);
	
}
