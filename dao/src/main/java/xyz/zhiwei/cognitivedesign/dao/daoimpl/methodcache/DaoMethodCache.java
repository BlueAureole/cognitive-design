package xyz.zhiwei.cognitivedesign.dao.daoimpl.methodcache;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.zhiwei.cognitivedesign.dao.daoimpl.BaseDaoImpl;
import xyz.zhiwei.cognitivedesign.dao.daoimpl.BaseMsgImpl;


/**
 * Dao方法缓存
 */
public abstract class DaoMethodCache {
	protected final Class<?> currentClass;
    protected final Type genericPType;
	protected Logger log;

    // 缓存：方法名 -> 匹配的方法对象（线程安全）
    protected final Map<String, Method> queryMethodCache = new ConcurrentHashMap<>();
    protected final Map<String, Method> addMethodCache = new ConcurrentHashMap<>();
    protected final Map<String, Method> updateMethodCache = new ConcurrentHashMap<>();
    protected final Map<String, Method> deleteMethodCache = new ConcurrentHashMap<>();
	
    
    public DaoMethodCache(Class<?> currentClass) {
    	this.log = LoggerFactory.getLogger(currentClass);
    	this.currentClass = currentClass;
        // 获取当前类泛型P的实际类型
        this.genericPType = ReflectGenericUtils.getGenericTypeParameter(this.currentClass, Arrays.asList(BaseDaoImpl.class,BaseMsgImpl.class), 0);

        // 初始化方法缓存（扫描当前类+所有父类）
        initAllMethodCaches();
    }
	
    


    
	//==========================================接口集===========================================
    

	public Method getQueryMethod(String desc) {
		return queryMethodCache.get(desc);
	}
	
	public Method getSaveMethod(String desc) {
		if(SaveMethodPrefixEnum.isAddName(desc)) {
			return addMethodCache.get(desc);
		}

		if(SaveMethodPrefixEnum.isUpdateName(desc)) {
			return updateMethodCache.get(desc);
		}

		if(SaveMethodPrefixEnum.isDeleteName(desc)) {
			return deleteMethodCache.get(desc);
		}
		return null;
	}

	

	//========================================== 依赖接口集===========================================
    
	

    protected abstract boolean isSubCollectionMethodMatch(Method method);
    protected abstract boolean isAddMethodMatch(Method method);
    protected abstract boolean isUpdateMethodMatch(Method method);
    protected abstract boolean isDeleteMethodMatch(Method method);


	//========================================== 私有方法区===========================================
    

    /**
     * 初始化所有缓存：扫描当前类+父类的符合规则方法
     */
    private void initAllMethodCaches() {
        // 递归扫描当前类 + 父类链
        scanAndCacheMethods(this.currentClass);
    }
    
    /**
     * 递归扫描类及其父类的方法，按规则分类缓存
     */
    private void scanAndCacheMethods(Class<?> currentClass) {
        if (currentClass == BaseDaoImpl.class || currentClass == BaseMsgImpl.class) {
        	printAllMethodCaches();
            return;
        }

        Method[] allMethods = currentClass.getDeclaredMethods();
        for (Method method : allMethods) {
            if (method.isBridge()) {
                continue; // 跳过桥接方法
            }

            //0, 只要权限为protected或默认的方法，不要public和private的
            int modifiers = method.getModifiers();
            boolean isProtectedOrDefault = 
                    Modifier.isProtected(modifiers) || 
                    (!Modifier.isPublic(modifiers) && !Modifier.isProtected(modifiers) && !Modifier.isPrivate(modifiers));
            if(!isProtectedOrDefault) {
            	continue;
            }

            
            // 1. 筛选 subCollection 方法（原有规则）
            if (isSubCollectionMethodMatch(method)) {
                method.setAccessible(true);
                queryMethodCache.put(method.getName(), method);
            }

            // 2. 筛选 add 方法：前缀add/insert + 入参List<P> + 返回Long
            if (isAddMethodMatch(method)) {
                method.setAccessible(true);
                addMethodCache.put(method.getName(), method);
            }

            // 3. 筛选 update 方法：前缀update + 入参List<P> + 返回Long
            if (isUpdateMethodMatch(method)) {
                method.setAccessible(true);
                updateMethodCache.put(method.getName(), method);
            }

            // 4. 筛选 delete 方法：前缀delete + 入参List<P> + 返回Long
            if (isDeleteMethodMatch(method)) {
                method.setAccessible(true);
                deleteMethodCache.put(method.getName(), method);
            }
            
        }

        // 递归扫描父类
        scanAndCacheMethods(currentClass.getSuperclass());
    }

    /*
     * ========================================== 方法缓存日志 ===========================================
     */
    
    
    /**
     * 打印所有方法缓存的详细信息（仅通过SLF4J输出）
     */
    private void printAllMethodCaches() {
    	log.info(buildAllMethodCachesLogContent());
    }
    
    private String buildAllMethodCachesLogContent() {
    	StringBuilder builder = new StringBuilder();
    	builder.append("类")
    		.append(this.currentClass.getName())
    		.append("<")
    		.append(this.genericPType.getTypeName())
    		.append("> 的缓存方法列表：");
    	
    	printCacheDetails(builder, "查询方法缓存", queryMethodCache);
    	printCacheDetails(builder, "新增方法缓存", addMethodCache);
    	printCacheDetails(builder, "更新方法缓存", updateMethodCache);
    	printCacheDetails(builder, "删除方法缓存", deleteMethodCache);
    	return builder.toString();
    }
    

    /**
     * 打印单个缓存的详细信息
     * @param cacheDesc 缓存描述（用于日志区分）
     * @param cache 目标缓存容器
     */
    private void printCacheDetails(StringBuilder builder, String cacheDesc, Map<String, Method> cache) {
    	builder.append(System.lineSeparator())
			.append("  ")
    		.append(cacheDesc)
    		.append(": 共 ")
    		.append(cache.size())
    		.append(" 个元素 : ");

        if (cache.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Method> entry : cache.entrySet()) {
            String key = entry.getKey();
            Method method = entry.getValue();
            builder.append(System.lineSeparator())
				.append("      键：")
            	.append(key)
            	.append(" → ")
            	.append(getMethodSignature(method));
        }
    }

    /**
     * 格式化方法签名，返回易读的方法信息
     * @param method 目标方法
     * @return 类名.方法名(参数类型1, 参数类型2) : 返回值类型
     */
    private String getMethodSignature(Method method) {
        if (method == null) {
            return "null";
        }

        // 拼接类名
        String className = method.getDeclaringClass().getSimpleName();
        // 拼接方法名
        String methodName = method.getName();
        // 拼接参数类型
        StringBuilder paramTypes = new StringBuilder();
        Class<?>[] params = method.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            paramTypes.append(params[i].getSimpleName());
            if (i < params.length - 1) {
                paramTypes.append(", ");
            }
        }
        // 拼接返回值类型
        String returnType = method.getReturnType().getSimpleName();

        return String.format("%s.%s(%s) : %s", className, methodName, paramTypes, returnType);
    }


	
}
