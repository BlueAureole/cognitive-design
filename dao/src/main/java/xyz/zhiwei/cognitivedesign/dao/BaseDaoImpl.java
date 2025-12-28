package xyz.zhiwei.cognitivedesign.dao;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.zhiwei.cognitivedesign.dao.utils.ReflectGenericUtils;
import xyz.zhiwei.cognitivedesign.morphism.Principle;
import xyz.zhiwei.cognitivedesign.morphism.support.image.PrincipleImagery;
import xyz.zhiwei.cognitivedesign.morphism.support.qualifier.PrincipleQualifier;

/**
 * 本原集存取,方法分发基础
 * @updateBy zhanghaiting
 * @param <P>
 */
public abstract class BaseDaoImpl<P extends Principle<?>> implements Dao<P>{
	protected Logger log;
    protected final Type genericPType;
    
    // 缓存：方法名 -> 匹配的方法对象（线程安全）
    protected final Map<String, Method> queryMethodCache = new ConcurrentHashMap<>();
    protected final Map<String, Method> addMethodCache = new ConcurrentHashMap<>();
    protected final Map<String, Method> updateMethodCache = new ConcurrentHashMap<>();
    protected final Map<String, Method> deleteMethodCache = new ConcurrentHashMap<>();

    //init
    public BaseDaoImpl() {
        Class<?> currentClass = this.getClass();
		this.log = LoggerFactory.getLogger(currentClass);
        // 获取当前类泛型P的实际类型
        this.genericPType = ReflectGenericUtils.getGenericTypeParameter(this.getClass(), BaseDaoImpl.class, 0);
        // 初始化方法缓存（扫描当前类+所有父类）
        initAllMethodCaches();
    }

    //子类可选实现的默认方法
	protected abstract Long addList(List<P> list);
	protected abstract Long updateList(List<P> list);
	protected abstract Long deleteList(List<P> list);
    
	//==========================================接口集===========================================
    

	@Override
    public List<P> subCollection(PrincipleQualifier<P> qualifier) {
		return dispatchQueryMethod(qualifier);
    }
	

	@Override
	public Long add(List<P> list) {
        return dispatchSaveMethod(list, addMethodCache, this::addList);
	}

	@Override
	public Long update(List<P> list) {
        return dispatchSaveMethod(list, updateMethodCache, this::updateList);
	}

	@Override
	public Long delete(List<P> list) {
        return dispatchSaveMethod(list, deleteMethodCache, this::deleteList);
	}



	//==========================================辅助方法： 方法分发===========================================


	/**
	 * 查询分发方法
	 * @param qualifier
	 * @return
	 */
    @SuppressWarnings("unchecked")
    protected List<P> dispatchQueryMethod(PrincipleQualifier<P> qualifier) {

		if(null==qualifier) {
			return new ArrayList<>();
		}
        String methodName = qualifier.getDescribe();
        // 1. 入参校验
        if (methodName == null) {
			return new ArrayList<>();
        }

        // 2. 从缓存获取方法（不存在则抛异常）
        Method targetMethod = queryMethodCache.get(methodName);
        if (targetMethod == null) {
			return new ArrayList<>();
        }

        // 3. 动态调用方法
        try {
            Object result = targetMethod.invoke(this, qualifier);
            return (List<P>) result;
        } catch (Exception e) {
            throw new RuntimeException(String.format("调用方法%s失败", methodName), e);
        }

    }
	
	
    /**
     * 通用分发逻辑：按 describe 调用缓存方法
     */
    protected Long dispatchSaveMethod(List<P> list, Map<String, Method> cache, Function<List<P>,Long> defaultFun) {
        if (list == null) {
            return 0L;
        }
        
    	//获取描述符（泛型擦除导致无法直接写<P>）
    	String describe=null;
        if (list instanceof PrincipleImagery<?>) {
        	describe=((PrincipleImagery<?>)list).getDescribe();
        }
        //没有描述符时，调用默认的方法。
        if(null==describe) {
        	return defaultFun.apply(list);
        }
    	
        // 从缓存获取方法并调用
    	String methodName=describe;
        Method targetMethod = cache.get(methodName);
        if (targetMethod == null) {
            throw new UnsupportedOperationException(String.format("未找到符合规则的%s方法：%s", describe, methodName));
        }
        try {
            Object result = targetMethod.invoke(this, list);
            return (Long) result;
        } catch (Exception e) {
            throw new RuntimeException(String.format("调用%s方法%s失败", describe, methodName), e);
        }
    }
	

	//==========================================辅助方法： 缓存方法列表到Map===========================================

    
    /**
     * 初始化所有缓存：扫描当前类+父类的符合规则方法
     */
    private void initAllMethodCaches() {
        // 递归扫描当前类 + 父类链
        scanAndCacheMethods(this.getClass());
    }
    
    /**
     * 递归扫描类及其父类的方法，按规则分类缓存
     */
    private void scanAndCacheMethods(Class<?> currentClass) {
        if (currentClass == BaseDaoImpl.class) {
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


    // ----------------- 方法匹配规则 -----------------
    
    
    /**
     * 查询规则：
     * 1. 入参：仅1个参数，且是PrincipleQualifier<P>或其子类；
     * 2. 返回值：List<P>或其子类（支持泛型匹配）
     */
    private boolean isSubCollectionMethodMatch(Method method) {
        // 1. 拆解每个判断条件为独立变量，方便调试查看
        Class<?>[] paramTypes = method.getParameterTypes();
        boolean paramCountMatch = paramTypes.length == 1;  // 参数数量是否为1
        boolean paramTypeMatch = false;                    // 参数类型是否匹配PrincipleQualifier
        boolean returnTypeMatch = false;                   // 返回值是否匹配List<P>
        
        // 2. 分步判断，避免短路逻辑导致部分变量未赋值
        if (paramCountMatch) {
            paramTypeMatch = ReflectGenericUtils.isAssignableFrom(paramTypes[0], PrincipleQualifier.class);
        }
        returnTypeMatch = ReflectGenericUtils.isReturnListMatchGeneric(method, this.genericPType,"P");
        
        // 4. 最终判断结果
        boolean isMatch = paramCountMatch && paramTypeMatch && returnTypeMatch;
        return isMatch;
    }

    /**
     * add 方法匹配规则：前缀add/insert + 入参List<P> + 返回Long
     */
    private boolean isAddMethodMatch(Method method) {
        // 拆解每个判断条件
        boolean isAddPrefix = ReflectGenericUtils.isMethodNameStartWith(method, "add");
        boolean isInsertPrefix = ReflectGenericUtils.isMethodNameStartWith(method, "insert");
        boolean isNameMatch = isAddPrefix || isInsertPrefix;  // 方法名前缀匹配
        boolean isParamMatch = ReflectGenericUtils.isParamListMatch(method, this.genericPType,"P");  // 参数匹配
        boolean isReturnMatch = ReflectGenericUtils.isReturnLongMatch(method);  // 返回值匹配
        
        // 最终结果
        boolean isMatch = isNameMatch && isParamMatch && isReturnMatch;
        return isMatch;
    }

    /**
     * update 方法匹配规则：前缀update + 入参List<P> + 返回Long
     */
    private boolean isUpdateMethodMatch(Method method) {
        // 拆解每个判断条件
        boolean isNameMatch = ReflectGenericUtils.isMethodNameStartWith(method, "update");  // 方法名前缀匹配
        boolean isParamMatch = ReflectGenericUtils.isParamListMatch(method, this.genericPType,"P"); // 参数匹配
        boolean isReturnMatch = ReflectGenericUtils.isReturnLongMatch(method);  // 返回值匹配
        
        
        // 最终结果
        boolean isMatch = isNameMatch && isParamMatch && isReturnMatch;
        return isMatch;
    }

    /**
     * delete 方法匹配规则：前缀delete + 入参List<P> + 返回Long
     */
    private boolean isDeleteMethodMatch(Method method) {
        // 拆解每个判断条件
        boolean isNameMatch = ReflectGenericUtils.isMethodNameStartWith(method, "delete");  // 方法名前缀匹配
        boolean isParamMatch = ReflectGenericUtils.isParamListMatch(method, this.genericPType,"P");  // 参数匹配
        boolean isReturnMatch = ReflectGenericUtils.isReturnLongMatch(method);  // 返回值匹配
        
        
        // 最终结果
        boolean isMatch = isNameMatch && isParamMatch && isReturnMatch;
        return isMatch;
    }

    // ----------------- 日志打印辅助 -----------------
    /**
     * 打印所有方法缓存的详细信息（仅通过SLF4J输出，DEBUG级别）
     */
    private void printAllMethodCaches() {
        printCacheDetails("查询方法缓存", queryMethodCache);
        printCacheDetails("新增方法缓存", addMethodCache);
        printCacheDetails("更新方法缓存", updateMethodCache);
        printCacheDetails("删除方法缓存", deleteMethodCache);
    }
    
    /**
     * 打印单个缓存的详细信息
     * @param cacheDesc 缓存描述（用于日志区分）
     * @param cache 目标缓存容器
     */
    private void printCacheDetails(String cacheDesc, Map<String, Method> cache) {
        if (!log.isDebugEnabled()) {
            return; // DEBUG级别未开启时直接返回，避免无效计算
        }

        // 打印缓存基础信息
        log.debug("========== {} ({}): 共 {} 个元素 ==========", 
                cacheDesc, cache.getClass().getSimpleName(), cache.size());

        if (cache.isEmpty()) {
            log.debug("{}: 无数据", cacheDesc);
            log.debug("============================================\n");
            return;
        }

        // 遍历打印每个缓存项的键和方法详情
        for (Map.Entry<String, Method> entry : cache.entrySet()) {
            String key = entry.getKey();
            Method method = entry.getValue();
            log.debug("键: {} → 方法详情: {}", key, getMethodSignature(method));
        }

        // 打印分隔线，区分不同缓存
        log.debug("============================================\n");
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
