package xyz.zhiwei.cognitivedesign.dao.daoimpl.methodcache;

import java.lang.reflect.Method;

import xyz.zhiwei.cognitivedesign.morphism.principle.source.qualifier.PrincipleQualifier;

/**
 * 直接Dao方法缓存
 */
public class DirectMethodCache extends DaoMethodCache{
	private final String genericPTypeName="P";

	public DirectMethodCache(Class<?> currentClass) {
		super(currentClass);
	}

    /**
     * 查询规则：
     * 1. 入参：仅1个参数，且是PrincipleQualifier<P>或其子类；
     * 2. 返回值：List<P>或其子类（支持泛型匹配）
     */
	@Override
	protected boolean isSubCollectionMethodMatch(Method method) {
        // 1. 拆解每个判断条件为独立变量，方便调试查看
        Class<?>[] paramTypes = method.getParameterTypes();
        boolean paramCountMatch = paramTypes.length == 1;  // 参数数量是否为1
        boolean paramTypeMatch = false;                    // 参数类型是否匹配PrincipleQualifier
        boolean returnTypeMatch = false;                   // 返回值是否匹配List<P>
        
        // 2. 分步判断，避免短路逻辑导致部分变量未赋值
        if (paramCountMatch) {
            paramTypeMatch = ReflectGenericUtils.isAssignableFrom(paramTypes[0], PrincipleQualifier.class);
        }
        returnTypeMatch = ReflectGenericUtils.isReturnListMatchGeneric(method, this.genericPType,genericPTypeName);
        
        // 4. 最终判断结果
        boolean isMatch = paramCountMatch && paramTypeMatch && returnTypeMatch;
        return isMatch;
    }

    /**
     * add 方法匹配规则：前缀add/insert + 入参List<P> + 返回Long
     */
	@Override
	protected boolean isAddMethodMatch(Method method) {
        // 拆解每个判断条件
        boolean isAddPrefix = ReflectGenericUtils.isMethodNameStartWith(method, SaveMethodPrefixEnum.ADD.getValue());
        boolean isInsertPrefix = ReflectGenericUtils.isMethodNameStartWith(method, SaveMethodPrefixEnum.INSERT.getValue());
        boolean isNameMatch = isAddPrefix || isInsertPrefix;  // 方法名前缀匹配
        boolean isParamMatch = ReflectGenericUtils.isParamListMatch(method, this.genericPType,genericPTypeName);  // 参数匹配
        boolean isReturnMatch = ReflectGenericUtils.isReturnLongMatch(method);  // 返回值匹配
        
        // 最终结果
        boolean isMatch = isNameMatch && isParamMatch && isReturnMatch;
        return isMatch;
    }

    /**
     * update 方法匹配规则：前缀update + 入参List<P> + 返回Long
     */
	@Override
	protected boolean isUpdateMethodMatch(Method method) {
        // 拆解每个判断条件
        boolean isNameMatch = ReflectGenericUtils.isMethodNameStartWith(method, SaveMethodPrefixEnum.UPDATE.getValue());  // 方法名前缀匹配
        boolean isParamMatch = ReflectGenericUtils.isParamListMatch(method, this.genericPType,genericPTypeName); // 参数匹配
        boolean isReturnMatch = ReflectGenericUtils.isReturnLongMatch(method);  // 返回值匹配
        
        
        // 最终结果
        boolean isMatch = isNameMatch && isParamMatch && isReturnMatch;
        return isMatch;
    }

    /**
     * delete 方法匹配规则：前缀delete + 入参List<P> + 返回Long
     */
	@Override
	protected boolean isDeleteMethodMatch(Method method) {
        // 拆解每个判断条件
        boolean isNameMatch = ReflectGenericUtils.isMethodNameStartWith(method, SaveMethodPrefixEnum.DELETE.getValue());  // 方法名前缀匹配
        boolean isParamMatch = ReflectGenericUtils.isParamListMatch(method, this.genericPType,genericPTypeName);  // 参数匹配
        boolean isReturnMatch = ReflectGenericUtils.isReturnLongMatch(method);  // 返回值匹配
        
        
        // 最终结果
        boolean isMatch = isNameMatch && isParamMatch && isReturnMatch;
        return isMatch;
    }

}
