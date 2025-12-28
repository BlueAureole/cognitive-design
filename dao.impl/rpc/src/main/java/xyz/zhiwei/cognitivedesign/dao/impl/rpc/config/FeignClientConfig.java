package xyz.zhiwei.cognitivedesign.dao.impl.rpc.config;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CaseFormat;

import feign.Contract;
import feign.MethodMetadata;
import feign.Request.HttpMethod;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Response;
import feign.codec.Decoder;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;

/**
 * Feign全局配置：仅包含契约、拦截器（无具体API代理）
 */
//@Configuration
public class FeignClientConfig {

    // 全局Jackson实例（保证序列化配置统一）
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // 线程安全缓存：方法签名 → 入参类型（第一个参数）
    private static final Map<String, Class<?>> METHOD_BODY_TYPE_CACHE = new ConcurrentHashMap<>();
    // 线程安全缓存：方法签名 → 返回值类型（支持泛型）
    private static final Map<String, Type> METHOD_RETURN_TYPE_CACHE = new ConcurrentHashMap<>();
	
    
    
    
    
    
    
    
    /**
     * 自定义契约：无注解推导路径/HTTP方法
     */
	@Bean
    public Contract feignContract() {
        return new Contract() {
            @Override
            public List<MethodMetadata> parseAndValidateMetadata(Class<?> targetType) {
                List<MethodMetadata> metadataList = new ArrayList<>();

                // 遍历目标接口所有方法（跳过默认/静态方法）
                for (Method method : targetType.getDeclaredMethods()) {
                    if (method.isDefault() || Modifier.isStatic(method.getModifiers())) {
                        continue;
                    }

                    // 1. 反射创建MethodMetadata（仅创建实例，不操作私有字段）
                    MethodMetadata metadata = createMethodMetadataByReflection(method);
                    if (metadata == null) continue;

                    // 2. 核心配置（仅用Feign公开API）
                    String configKey = buildConfigKey(targetType, method);
                    metadata.configKey(configKey);
                    metadata.template().method(getHttpMethod(method));
                    metadata.template().uri(getClassBasePath(targetType) + getMethodPath(method));
                    metadata.template().header("Content-Type", "application/json");


                    // 3. 缓存入参+返回值类型（核心扩展）
                    if (method.getParameterCount() > 0) {
                        metadata.bodyIndex(0);
                        METHOD_BODY_TYPE_CACHE.put(configKey, method.getParameterTypes()[0]);
                    }
                    // 缓存返回值类型（支持泛型，如List<Explorer>）
                    METHOD_RETURN_TYPE_CACHE.put(configKey, method.getGenericReturnType());
                    
                    metadataList.add(metadata);
                }

                return metadataList;
            }

            

            /**
             * 反射创建MethodMetadata并绑定method + returnType（解决所有null异常）
             */
            private MethodMetadata createMethodMetadataByReflection(Method method) {
                try {
                    // 1. 创建MethodMetadata实例
                    Constructor<MethodMetadata> constructor = MethodMetadata.class.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    MethodMetadata metadata = constructor.newInstance();

                    // 2. 绑定method字段（解决method=null）
                    Field methodField = MethodMetadata.class.getDeclaredField("method");
                    methodField.setAccessible(true);
                    methodField.set(metadata, method);

                    // 3. 绑定returnType字段（解决returnType=null）
                    Field returnTypeField = MethodMetadata.class.getDeclaredField("returnType");
                    returnTypeField.setAccessible(true);
                    returnTypeField.set(metadata, method.getGenericReturnType()); // 绑定返回值类型

                    return metadata;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            /**
             * 拼接configKey（Feign 12.4必需）
             */
            private String buildConfigKey(Class<?> targetType, Method method) {
                StringBuilder sb = new StringBuilder();
                sb.append(targetType.getName()).append("#");
                sb.append(method.getName()).append("(");
                Class<?>[] parameterTypes = method.getParameterTypes();
                for (int i = 0; i < parameterTypes.length; i++) {
                    sb.append(parameterTypes[i].getName());
                    if (i < parameterTypes.length - 1) {
                        sb.append(",");
                    }
                }
                sb.append(")");
                return sb.toString();
            }
        };
    }

    /**
     * 自定义编码器：从缓存获取入参类型
     */
    @Bean
    public Encoder feignEncoder() {
        return new Encoder() {
            private final JacksonEncoder delegate = new JacksonEncoder(OBJECT_MAPPER);

            @Override
            public void encode(Object object, Type bodyType, RequestTemplate template) throws EncodeException {
                String configKey = template.feignTarget().name() + "#" + template.methodMetadata().configKey();
                Class<?> cachedType = METHOD_BODY_TYPE_CACHE.get(configKey);
                
                Type actualType = cachedType != null ? cachedType : 
                                  (object != null ? object.getClass() : bodyType);
                
                if (actualType == null) {
                    throw new EncodeException("Request body and bodyType are both null");
                }
                delegate.encode(object, actualType, template);
            }
        };
    }

    /**
     * 自定义解码器：从缓存获取返回值类型（兜底解决returnType=null）
     */
    @Bean
    public Decoder feignDecoder() {
        return new Decoder() {
            private final JacksonDecoder delegate = new JacksonDecoder(OBJECT_MAPPER);

            @Override
            public Object decode(Response response, Type type) throws IOException {
                // 1. 解析configKey（从Response中获取方法元数据）
                String configKey = response.request().requestTemplate().methodMetadata().configKey();
                // 2. 从缓存获取返回值类型（优先级：缓存 → 原type）
                Type actualReturnType = METHOD_RETURN_TYPE_CACHE.getOrDefault(configKey, type);
                
                if (actualReturnType == null) {
                    throw new EncodeException("Return type is null for method: " + configKey);
                }
                // 3. 用缓存的返回值类型解码
                return delegate.decode(response, actualReturnType);
            }
        };
    }
	
    /**
     * Istio追踪头拦截器（透传分布式追踪头）
     */
    @Bean
    public RequestInterceptor istioHeaderInterceptor() {
        return template -> {
            template.header("Content-Type", "application/json");
            template.header("x-request-id", "{x-request-id}");
            template.header("x-b3-traceid", "{x-b3-traceid}");
            template.header("x-b3-spanid", "{x-b3-spanid}");
        };
    }
    


    /**
     * Feign代理工具（注入所有配置）
     */
    @Bean
    public FeignProxyUtil feignProxyUtil(Environment environment,
                                         Contract feignContract,
                                         RequestInterceptor istioHeaderInterceptor,
                                         Encoder feignEncoder,
                                         Decoder feignDecoder) {
        return new FeignProxyUtil(environment, feignContract, istioHeaderInterceptor, feignEncoder, feignDecoder);
    }
    
    

    // ==================== 路径映射工具：类名/方法名 → HTTP路径/方法, 子类按需重写 ====================
    /**
     * 类名转基础路径：LowerCamelApi → /lowercamel
     */
    protected String getClassBasePath(Class<?> apiInterface) {
        String className = apiInterface.getSimpleName();
        // 移除Api后缀，统一转为全小写
        String baseName = className.replaceAll("Api$", "");
        String lowerName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, baseName).toLowerCase();
        return "/" + lowerName;
    }

    /**
     * 方法名转接口路径：queryOne → /queryOne.do
     */
    protected String getMethodPath(Method method) {
        String methodName = method.getName();
        return "/" + methodName + ".do";
    }

    /**
     * 推导HTTP方法
     */
    protected HttpMethod getHttpMethod(Method method) {
        return HttpMethod.POST;
    }
    
}
