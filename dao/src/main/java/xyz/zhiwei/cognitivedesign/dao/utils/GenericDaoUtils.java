package xyz.zhiwei.cognitivedesign.dao.utils;

import org.springframework.aop.support.AopUtils;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

/**
 * Dao泛型解析工具类
 * 专注于解析Dao实现类的泛型实体类（优先接口、后父类）
 */
public class GenericDaoUtils {

    /**
     * 解析Dao实现类的泛型实体类（优先级：直接实现的Dao接口 > 父类）
     * @param daoImplClass Dao实现类（原始类，非代理类）
     * @param daoInterface Dao核心接口（如Dao.class）
     * @return 泛型实体类，无则返回null
     */
    public static Class<?> resolveGenericEntityClass(Class<?> daoImplClass, Class<?> daoInterface) {
        // 空值防护
        if (daoImplClass == null || daoInterface == null) {
            return null;
        }

        // ========== 第一步：优先解析直接实现的Dao接口泛型 ==========
        Class<?> interfaceGeneric = resolveInterfaceGeneric(daoImplClass, daoInterface);
        if (interfaceGeneric != null) {
            return interfaceGeneric;
        }

        // ========== 第二步：接口未找到时，解析父类泛型 ==========
        return resolveSuperclassGeneric(daoImplClass);
    }

    /**
     * 解析直接实现的Dao接口泛型
     */
    private static Class<?> resolveInterfaceGeneric(Class<?> daoImplClass, Class<?> daoInterface) {
        Type[] genericInterfaces = daoImplClass.getGenericInterfaces();
        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType) {
                ParameterizedType parameterizedInterface = (ParameterizedType) genericInterface;
                Type rawInterfaceType = parameterizedInterface.getRawType();

                // 确认是目标Dao接口（排除其他接口干扰）
                if (rawInterfaceType instanceof Class<?> && daoInterface.isAssignableFrom((Class<?>) rawInterfaceType)) {
                    Type[] actualTypeArgs = parameterizedInterface.getActualTypeArguments();
                    if (actualTypeArgs.length > 0) {
                        return extractRawClassFromType(actualTypeArgs[0]);
                    }
                }
            }
        }
        return null;
    }

    /**
     * 解析父类泛型
     */
    private static Class<?> resolveSuperclassGeneric(Class<?> daoImplClass) {
        Type genericSuperclass = daoImplClass.getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType) {
            ParameterizedType parameterizedParent = (ParameterizedType) genericSuperclass;
            Type[] actualTypeArgs = parameterizedParent.getActualTypeArguments();
            if (actualTypeArgs.length > 0) {
                return extractRawClassFromType(actualTypeArgs[0]);
            }
        }
        return null;
    }

    /**
     * 从Type中提取原始Class（兼容普通Class/ParameterizedType/TypeVariable）
     * @param type 泛型类型（如Edition.class 或 Session<U>）
     * @return 原始Class，无则返回null
     */
    public static Class<?> extractRawClassFromType(Type type) {
        if (type instanceof Class<?>) {
            // 普通Class类型（如Edition.class）
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            // 带泛型的类型（如Session<U>），取原始类型（Session.class）
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();
            return rawType instanceof Class<?> ? (Class<?>) rawType : null;
        } else if (type instanceof TypeVariable<?>) {
            // 泛型变量（如T），无法解析则返回null
            return null;
        }
        return null;
    }

    /**
     * 解AOP代理，获取Dao实现类的原始类
     */
    public static Class<?> getRawDaoClass(Object daoImpl) {
        if (daoImpl == null) {
            return null;
        }
        return AopUtils.getTargetClass(daoImpl);
    }
}

