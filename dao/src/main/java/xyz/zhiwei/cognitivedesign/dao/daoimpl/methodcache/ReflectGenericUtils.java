package xyz.zhiwei.cognitivedesign.dao.daoimpl.methodcache;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import xyz.zhiwei.cognitivedesign.dao.daoimpl.model.Message;

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
     * 校验方法返回值是否为 Message<Long> 类型（兼容 Long 装箱类型/long 基本类型的泛型匹配）
     * @param method 目标方法
     * @return 是否匹配 Message<Long> 类型
     */
    public static boolean isReturnMessageLongMatch(Method method) {
        // 1. 校验返回值原始类型是精准的 Message 类（非子类）
        Class<?> returnType = method.getReturnType();
        if (returnType != Message.class) {
            return false;
        }

        // 2. 校验返回值是参数化类型（即 Message<XXX>，而非裸类型 Message）
        Type genericReturnType = method.getGenericReturnType();
        if (!(genericReturnType instanceof ParameterizedType)) {
            return false; // 排除裸类型 Message
        }

        // 3. 解析 Message 的泛型参数
        ParameterizedType messageParamType = (ParameterizedType) genericReturnType;
        Type[] actualTypeArguments = messageParamType.getActualTypeArguments();
        if (actualTypeArguments.length != 1) {
            return false; // Message 只能有一个泛型参数
        }

        // 4. 校验泛型参数是 Long（装箱）或 long（基本类型）
        Type genericType = actualTypeArguments[0];
        // 兼容：泛型参数是 Long.class 或 long.class（基本类型自动装箱场景）
        return genericType == Long.class || genericType == long.class;
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
     * 校验方法返回值是否为精准的 Message<List<P>> 类型（Message不包含子类，List包含子类）
     * @param method 目标方法
     * @param genericPType 泛型P的实际类型（如Person.class）
     * @param genericPTypeName 泛型P的类型名（如"com.example.Person"）
     * @return 是否匹配精准的 Message<List<P>> 类型
     */
    public static boolean isReturnExactMessageListMatchGeneric(Method method, Type genericPType, String genericPTypeName) {
        // 1. 校验返回值是【精准的 Message 类】（排除子类）
        Class<?> returnType = method.getReturnType();
        // 关键修改：用 == 替代 isAssignableFrom，确保是精准的 Message 类而非子类
        if (returnType != Message.class) {
            return false;
        }

        // 2. 校验返回值是参数化类型（即 Message<XXX>，而非裸类型 Message）
        Type genericReturnType = method.getGenericReturnType();
        if (!(genericReturnType instanceof ParameterizedType)) {
            return false; // 非参数化类型（如 Message 而非 Message<List<P>>）
        }

        // 3. 解析外层 Message 的泛型参数（应为 List/P 的子类）
        ParameterizedType messageParamType = (ParameterizedType) genericReturnType;
        Type[] messageActualTypes = messageParamType.getActualTypeArguments();
        if (messageActualTypes.length != 1) {
            return false; // Message 只能有一个泛型参数
        }
        Type messageGenericType = messageActualTypes[0];

        // 4. 校验 Message 的泛型参数是【参数化的 List/其子类】（允许 List 子类）
        if (!(messageGenericType instanceof ParameterizedType)) {
            return false; // 如 Message<ArrayList> 而非 Message<ArrayList<P>> 也会返回false
        }

        // 解析 List 层的参数化类型
        ParameterizedType listParamType = (ParameterizedType) messageGenericType;
        // 4.1 校验 List 层的原始类型是 List 或其子类
        Type listRawType = listParamType.getRawType();
        if (!(listRawType instanceof Class) || !List.class.isAssignableFrom((Class<?>) listRawType)) {
            return false; // 非 List 及其子类（如 Message<Map<P>> 会被过滤）
        }

        // 4.2 校验 List 层有且仅有一个泛型参数（即 List<P> 而非 List）
        Type[] listActualTypes = listParamType.getActualTypeArguments();
        if (listActualTypes.length != 1) {
            return false;
        }
        Type listGenericType = listActualTypes[0];

        // 5. 校验 List 的泛型参数是否匹配目标 P 类型（兼容多场景）
        // 场景1：泛型参数是 TypeVariable（如 List<P> 中的 P）
        boolean isTypeVariableMatch = listGenericType.equals(genericPType);
        // 场景2：泛型参数是具体 Class（如 List<Person>），支持子类赋值
        boolean isClassMatch = false;
        if (listGenericType instanceof Class && genericPType instanceof Class) {
            isClassMatch = ((Class<?>) listGenericType).isAssignableFrom((Class<?>) genericPType)
                    || ((Class<?>) genericPType).isAssignableFrom((Class<?>) listGenericType);
        }
        // 场景3：通过类型名匹配（兼容全限定名一致的情况）
        boolean isTypeNameMatch = genericPTypeName.equals(listGenericType.getTypeName());

        // 任一匹配即返回 true
        return isTypeVariableMatch || isClassMatch || isTypeNameMatch;
    }
    
    
    
    
    /**
     * 获取类的泛型参数实际类型（如BaseCollection<Person>中的Person）
     * @param targetClass 目标类
     * @param stopClasses 向上查找的终止类列表（遍历到列表中的任意一个类时，检查该类后停止）
     * @param paramIndex 泛型参数索引（默认0，对应第一个泛型参数）
     * @return 泛型参数的实际类型，未找到则返回Object.class
     */
    public static Type getGenericTypeParameter(Class<?> targetClass,
                                               List<Class<?>> stopClasses,
                                               int paramIndex) {
        // 入参合法性校验
        if (targetClass == null || stopClasses == null || paramIndex < 0) {
            return Object.class;
        }
        // 空列表处理：视为无终止限制，只遍历到Object为止
        List<Class<?>> effectiveStopClasses = stopClasses.isEmpty()
                ? Collections.emptyList()
                : stopClasses;

        Class<?> currentClass = targetClass;
        // 递归向上查找泛型父类，直到遍历到Object类
        while (currentClass != null) {
            Type superclass = currentClass.getGenericSuperclass();
            // 检查当前类的父类是否是带泛型参数的类型
            if (superclass instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType) superclass;
                Type[] actualTypeArguments = paramType.getActualTypeArguments();
                if (actualTypeArguments.length > paramIndex) {
                    return actualTypeArguments[paramIndex];
                }
                // 索引越界时不再继续查找
                break;
            }

            // 检查当前类是否在终止列表中（包含终止类层级），若是则停止遍历
            if (effectiveStopClasses.contains(currentClass)) {
                break;
            }

            // 向上遍历父类，若到Object类则停止
            currentClass = currentClass.getSuperclass();
            if (currentClass == Object.class) {
                break;
            }
        }

        // 未找到泛型父类、索引越界，返回Object.class
        return Object.class;
    }

}
