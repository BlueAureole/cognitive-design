package xyz.zhiwei.cognitivedesign.dao;

import java.util.List;

import xyz.zhiwei.cognitivedesign.morphism.Principle;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.container.PrincipleImagery;
import xyz.zhiwei.cognitivedesign.morphism.principle.source.qualifier.PrincipleQualifier;





/**
 * 本原集存取
 * @updateBy zhanghaiting
 * @param <P>
 */
public interface Dao<P extends Principle<?>>{

    List<P> subCollection(PrincipleQualifier<P> qualifier);
    
    Long save(PrincipleImagery<P> principleImagery);
    
	
}
