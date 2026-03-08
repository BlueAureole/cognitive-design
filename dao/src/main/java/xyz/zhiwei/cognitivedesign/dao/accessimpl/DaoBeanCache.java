package xyz.zhiwei.cognitivedesign.dao.accessimpl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;

import xyz.zhiwei.cognitivedesign.dao.Dao;
import xyz.zhiwei.cognitivedesign.morphism.Principle;

/**
 * 缓存：Class -> DaoImpl（如Account.class -> AccountDaoImpl）
 */
public class DaoBeanCache extends ConcurrentHashMap<Class<?>, Dao<?>>{
	private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DaoBeanCache.class);


    public DaoBeanCache(ApplicationContext context) {
    	this.initDaoCache(context);
    }
    

	/*
	 * ==================================接口方法区==============================
	 */

	/**
	 * 获取列表的泛型
	 * @param <P>
	 * @param list
	 * @return
	 */
	public <P extends Principle<?>> Class<P> getClassFromList(List<P> list){
		if(null==list || list.isEmpty()) {
			return null;
		}
		P fristNotNull=list.stream().filter(Objects::nonNull).findFirst().orElse(null);
		if(null==fristNotNull) {
			return null;
		}
		
		Class<P> clazz=findInCache(fristNotNull);
		return clazz;
	}
	

    /**
     * 
     * 根据实体Class获取对应的Dao实现bean
     * @param <P>
     * @param principleClass ： Demo.class
     * @param applicationContext
     * @return Dao<Demo>: DemoDaoImpl
     */
	public <P extends Principle<?>> Dao<P> getDaoBeanByPrincipleClass(Class<P> principleClass){
    	
    	Dao<?> daoImpl = this.get(principleClass);
        if (daoImpl == null) {
            throw new IllegalArgumentException("未找到实体类[" + principleClass.getName() + "]对应的Dao实现");
        }

		@SuppressWarnings("unchecked")
		Dao<P> daoBean = (Dao<P>) daoImpl;
    	return daoBean;
    }
    

	/*
	 * ==================================辅助方法区: 确定类型==============================
	 */
	

    /**
     * 查找实例的类或其父类在缓存中对应的类型
     * @param instance 要检查的实例
     * @return 缓存中找到的类型，如未找到则返回null
     */
	@SuppressWarnings("unchecked")
	private <P> Class<P> findInCache(P instance) {
		
        Class<?> clazz=findInCache(instance.getClass());
        if(null==clazz) {
        	return null;
        }
        return (Class<P>) clazz;
    }

    /**
     * 查找类或其父类在缓存中对应的类型
     * @param clazz 要检查的类
     * @return 缓存中找到的类型，如未找到则返回null
     */
    private Class<?> findInCache(Class<?> clazz) {
    	
        // 检查当前类是否在缓存中
        if (this.containsKey(clazz)) {
            return clazz;
        }
        
        // 获取父类
        Class<?> superClass = clazz.getSuperclass();
        
        // 如果父类存在且不是Object类，递归查找
        if (superClass != null && superClass != Object.class) {
            return findInCache(superClass);
        }
        
        // 找不到任何匹配的类型
        return null;
    }


	/*
	 * ==================================辅助方法区: 初始化dao 缓存==============================
	 */
	

    /**
     * 初始化缓存变量（优先解析直接实现的Dao接口泛型）
     * @param context Spring应用上下文
     */
    private void initDaoCache(ApplicationContext context) {
        if (this.isEmpty()) { // 第一次检查（无锁）
            synchronized (this) { // 双重检查锁，确保初始化唯一性
                if (this.isEmpty()) {
                    // 1. 从Spring容器获取所有Dao实现类Bean
                    @SuppressWarnings("rawtypes")
                    Map<String, Dao> daoBeans = context.getBeansOfType(Dao.class);

                    // 2. 遍历解析并缓存
                    for (Dao<?> daoImpl : daoBeans.values()) {
                        // 工具类：解AOP代理，获取原始类
                        Class<?> daoImplClass = AopUtils.getTargetClass(daoImpl);
                        // 工具类：解析泛型实体类（优先接口、后父类）
                        Class<?> genericEntityClass = DaoBeanCacheUtils.resolveGenericEntityClass(daoImplClass, Dao.class);

                        if (genericEntityClass != null) {
                            this.put(genericEntityClass, daoImpl);
                        }
                    }
                }
            }
        }
        //打印容器里的内容
    	printDaoCacheContent();
    }

	/*
	 * ==================================日志 辅助工具区==============================
	 */

    /**
     * 格式化打印this中的内容（私有辅助方法）
     */
    private void printDaoCacheContent() {
    	log.info(buildDaoCacheLogContent());
    }
    
    private String buildDaoCacheLogContent() {
    	StringBuilder builder = new StringBuilder();
    	
    	if (this.isEmpty()) {
    		builder.append("【Dao缓存初始化】 容器为空，未加载任何Dao实现类");
    		return builder.toString();
    	}

    	builder.append("【Dao缓存初始化】 容器内容如下（共")
    		.append(this.size())
    		.append("个Dao实现类）：");
        for (Map.Entry<Class<?>, Dao<?>> entry : this.entrySet()) {
            Class<?> entityClass = entry.getKey();
            Dao<?> daoImpl = entry.getValue();
            Class<?> daoImplClass = AopUtils.getTargetClass(daoImpl);
            builder.append(System.lineSeparator())
            	.append("  实体类：")
            	.append(entityClass.getName())
            	.append(" -> Dao实现类：")
            	.append(daoImplClass.getName());
        }
        return builder.toString();
    }
    


}
