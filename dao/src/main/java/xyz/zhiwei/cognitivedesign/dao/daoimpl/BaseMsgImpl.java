package xyz.zhiwei.cognitivedesign.dao.daoimpl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.zhiwei.cognitivedesign.dao.Dao;
import xyz.zhiwei.cognitivedesign.dao.daoimpl.methodcache.DaoMethodCache;
import xyz.zhiwei.cognitivedesign.dao.daoimpl.methodcache.MsgMethodCache;
import xyz.zhiwei.cognitivedesign.dao.daoimpl.model.Message;
import xyz.zhiwei.cognitivedesign.morphism.Principle;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.container.PrincipleImagery;
import xyz.zhiwei.cognitivedesign.morphism.principle.source.qualifier.PrincipleQualifier;

/**
 * 本原集存取,方法分发基础
 * @updateBy zhanghaiting
 * @param <P>
 */
public abstract class BaseMsgImpl<P extends Principle<?>> implements Dao<P>{
	protected Logger log;
    //方法缓存
    DaoMethodCache daoMethodCache;

    
    //init
    public BaseMsgImpl() {
        Class<?> currentClass = this.getClass();
		this.log = LoggerFactory.getLogger(currentClass);
        //方法缓存
        this.daoMethodCache=new MsgMethodCache(currentClass);
    }



    
	//==========================================向上实现接口集===========================================
    
    

	@Override
    public List<P> subCollection(PrincipleQualifier<P> qualifier) {
		Message<List<P>> msg=dispatchQueryMethod(qualifier);
		if(null!=msg){
			return msg.getResult();
		}else {
			return null;
		}
    }

	
	@Override
	public Long save(PrincipleImagery<P> principleImagery) {
		Message<Long> msg=dispatchSaveMethod(principleImagery);
		if(null!=msg){
			return msg.getResult();
		}else {
			return null;
		}
	}


	

	/**
	 * 向下依赖接口集
	 * 
	protected abstract Message<Long> add(List<P> list);
	protected abstract Message<Long> update(List<P> list);
	protected abstract Message<Long> delete(List<P> list);
	*/
	
	
	

	//==========================================辅助方法： 方法分发===========================================


	/**
	 * 查询分发方法
	 * @param qualifier
	 * @return
	 */
    @SuppressWarnings("unchecked")
    protected Message<List<P>> dispatchQueryMethod(PrincipleQualifier<P> qualifier) {

		if(null==qualifier) {
			return Message.success(new ArrayList<>());
		}
        String methodName = qualifier.getDescribe();
        // 1. 入参校验
        if (methodName == null) {
			return Message.success(new ArrayList<>());
        }

        // 2. 从缓存获取方法（不存在则抛异常）
        Method targetMethod = daoMethodCache.getQueryMethod(methodName);
        if (targetMethod == null) {
			return Message.success(new ArrayList<>());
        }

        // 3. 动态调用方法
        try {
            Object result = targetMethod.invoke(this, qualifier);
            return (Message<List<P>>) result;
        } catch (Exception e) {
            throw new RuntimeException(String.format("调用方法%s失败", methodName), e);
        }

    }
	
	
    /**
     * 通用分发逻辑：按 describe 调用缓存方法
     */
    @SuppressWarnings("unchecked")
	protected Message<Long> dispatchSaveMethod(PrincipleImagery<P> principleImagery) {
        if (isEmpty(principleImagery)) {
			return Message.success(0L);
        }
        
    	//获取描述符（泛型擦除导致无法直接写<P>）
    	String describe=principleImagery.getDescribe();
    	
        // 从缓存获取方法并调用
    	String methodName=describe;
        Method targetMethod = daoMethodCache.getSaveMethod(methodName);
        if (targetMethod == null) {
            throw new UnsupportedOperationException(String.format("未找到符合规则的%s方法：%s", describe, methodName));
        }
        try {
            Object result = targetMethod.invoke(this, principleImagery);
            return (Message<Long>) result;
        } catch (Exception e) {
            throw new RuntimeException(String.format("调用%s方法%s失败", describe, methodName), e);
        }
    }
	

	/**
	 * @param list
	 * @return
	 */
	private boolean isEmpty(List<?> list) {

        if (null == list || list.isEmpty() || !list.stream().anyMatch(Objects::nonNull)) {
            return true;
        }
        return false;
	}

}
