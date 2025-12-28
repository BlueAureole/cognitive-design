package xyz.zhiwei.cognitivedesign.dao.utils;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * 反射&泛型辅助工具类
 */
public class ReflectGenericUtils {
	

    /**
     * 判断名称是否不以 array 任一前缀开头（大小写不敏感）
     * @param name 待校验的名称（如方法名、接口名等）
     * @return true=不以目标前缀开头 / false=以任一目标前缀开头 / 空值返回true（按"无前缀"处理）
     */
    public static boolean isNotStartWithPrefix(String name,String[] array) {
        // 2. 统一转为小写，避免大小写干扰（如Add/ADD/insert等场景）
        String nameLower = name.trim().toLowerCase();
        // 3. 遍历校验是否以目标前缀开头
        for (String prefix : array) {
            if (nameLower.startsWith(prefix)) {
                return false; // 只要匹配一个前缀，立即返回false
            }
        }
        return true;
    }
	
	
    /**
     * 判断方法名是否以指定前缀开头（忽略大小写，可选）
     */
    public static boolean isMethodNameStartWith(Method method, String prefix) {
        return method.getName().startsWith(prefix);
    }

    /**
     * 校验方法入参是否为 List<P> 或其子类
     */
    public static boolean isParamListMatch(Method method, Type genericPType, String genericPTypeName) {
        Class<?>[] paramTypes = method.getParameterTypes();
        // 仅1个参数，且是 List 或其子类
        if (paramTypes.length != 1 || !List.class.isAssignableFrom(paramTypes[0])) {
            return false;
        }
        // 校验泛型（可选，若需严格匹配 List<P> 泛型）
        Type[] genericParamTypes = method.getGenericParameterTypes();
        if (!(genericParamTypes[0] instanceof ParameterizedType)) {
            return true; // 不校验泛型则直接返回true，按需调整
        }
        ParameterizedType paramType = (ParameterizedType) genericParamTypes[0];
        Type[] actualTypeArguments = paramType.getActualTypeArguments();
        

        if (actualTypeArguments.length != 1) {
            return false;
        }
        
        Type listGenericType = actualTypeArguments[0];
        boolean currentGenericTypeSign= listGenericType.equals(genericPType);
        if(currentGenericTypeSign) {
        	return true;
        }
        if(genericPTypeName.equals(listGenericType.getTypeName())) {
        	return true;
        }
        return false;
        
    }

    /**
     * 校验方法返回值是否为 Long 或其子类（基本类型 long 自动装箱）
     */
    public static boolean isReturnLongMatch(Method method) {
        Class<?> returnType = method.getReturnType();
        return returnType == Long.class || returnType == long.class;
    }
	
	

    
    
    
    
    
    
    
    /**
     * 判断子类是否继承/实现父类（支持类/接口，递归向上匹配）
     */
    public static boolean isAssignableFrom(Class<?> subClass, Class<?> superClass) {
        if (subClass == null || superClass == null) {
            return false;
        }
        // 直接继承/实现
        if (superClass.isAssignableFrom(subClass)) {
            return true;
        }
        // 递归匹配接口
        for (Class<?> ifc : subClass.getInterfaces()) {
            if (isAssignableFrom(ifc, superClass)) {
                return true;
            }
        }
        // 递归匹配父类
        return isAssignableFrom(subClass.getSuperclass(), superClass);
    }

    /**
     * 校验方法返回值是否为 List<P> 或其子类（匹配泛型类型）
     * @param method 目标方法
     * @param genericPType 泛型P的实际类型（如Person.class）
     */
    public static boolean isReturnListMatchGeneric(Method method, Type genericPType, String genericPTypeName) {
        // 1. 校验返回值是List或其子类（原始类型）
        Class<?> returnType = method.getReturnType();
        if (!List.class.isAssignableFrom(returnType)) {
            return false;
        }

        // 2. 校验泛型类型（参数化类型）
        Type genericReturnType = method.getGenericReturnType();
        if (!(genericReturnType instanceof ParameterizedType)) {
            return false; // 非参数化类型（如List而非List<P>）
        }

        ParameterizedType paramType = (ParameterizedType) genericReturnType;
        Type[] actualTypeArguments = paramType.getActualTypeArguments();
        if (actualTypeArguments.length != 1) {
            return false;
        }

        // 3. 匹配泛型参数与目标类型
        Type listGenericType = actualTypeArguments[0];
        // 兼容泛型参数是Class或TypeVariable的情况
        boolean currentGenericTypeSign= listGenericType.equals(genericPType) 
                || (listGenericType instanceof Class && ((Class<?>) listGenericType).isAssignableFrom((Class<?>) genericPType));
        if(currentGenericTypeSign) {
        	return true;
        }
        if(genericPTypeName.equals(listGenericType.getTypeName())) {
        	return true;
        }
        return false;
    }

    /**
     * 获取类的泛型参数实际类型（如BaseCollection<Person>中的Person）
     * @param targetClass 目标类
     * @param genericSuperClass 泛型父类（如BaseCollection.class）
     * @param paramIndex 泛型参数索引（默认0，对应P）
     */
    public static Type getGenericTypeParameter(Class<?> targetClass, Class<?> genericSuperClass, int paramIndex) {
        Type superclass = targetClass.getGenericSuperclass();
        // 递归向上查找泛型父类
        while (!(superclass instanceof ParameterizedType)) {
            targetClass = targetClass.getSuperclass();
            if (targetClass == Object.class) {
                return Object.class; // 未找到泛型父类
            }
            superclass = targetClass.getGenericSuperclass();
        }

        ParameterizedType paramType = (ParameterizedType) superclass;
        Type[] actualTypeArguments = paramType.getActualTypeArguments();
        if (actualTypeArguments.length <= paramIndex) {
            return Object.class;
        }
        return actualTypeArguments[paramIndex];
    }
}
