package xyz.zhiwei.cognitivedesign.dao.impl.rpc.config;

import java.util.concurrent.TimeUnit;

import org.springframework.core.env.Environment;

import feign.Contract;
import feign.Feign;
import feign.Request;
import feign.RequestInterceptor;
import feign.codec.Decoder;
import feign.codec.Encoder;
import jakarta.annotation.PostConstruct;

/**
 * Feign代理创建工具：改为普通类，交由Spring容器管理（移除静态特性）
 */
//@Component // 交给Spring扫描创建实例
public class FeignProxyUtil {

    // 1. 注入Spring环境（读取配置）
    private final Environment environment;
    // 2. 注入通用契约和拦截器（Spring容器中的Bean）
    private final Contract feignContract;
    private final RequestInterceptor istioHeaderInterceptor;

    private final Encoder feignEncoder;
    private final Decoder feignDecoder;

    // 3. 超时配置（从Spring配置读取）
    private int connectTimeout;
    private int readTimeout;

    // ========== 构造器注入依赖（推荐：强制依赖，避免空指针） ==========
    public FeignProxyUtil(Environment environment,
                          Contract feignContract,
                          RequestInterceptor istioHeaderInterceptor,
                          Encoder feignEncoder,
                          Decoder feignDecoder
                          ) {
        this.environment = environment;
        this.feignContract = feignContract;
        this.istioHeaderInterceptor = istioHeaderInterceptor;
        this.feignEncoder = feignEncoder;
        this.feignDecoder = feignDecoder;
    }

    // ========== 初始化：读取超时配置 ==========
    @PostConstruct
    public void initTimeoutConfig() {
        // 从application.yml读取配置，指定默认值
        this.connectTimeout = Integer.parseInt(
                environment.getProperty("feign.client.config.default.connectTimeout", "3000")
        );
        this.readTimeout = Integer.parseInt(
                environment.getProperty("feign.client.config.default.readTimeout", "5000")
        );
    }

    // ========== 普通方法：创建代理（移除static） ==========
    /**
     * 极简代理创建：普通方法，依赖Spring注入的契约/拦截器
     */
    public <T> T createProxy(Class<T> apiClass, String serviceUrl) {
        // 构建超时配置（无过时警告）
        Request.Options options = new Request.Options(
                connectTimeout, TimeUnit.MILLISECONDS,
                readTimeout, TimeUnit.MILLISECONDS,
                true // 跟随重定向
        );

        return Feign.builder()
                .encoder(feignEncoder)
                .decoder(feignDecoder)
                .contract(feignContract)        // 注入的契约（父类/子类实现）
                .requestInterceptor(istioHeaderInterceptor) // 注入的Istio拦截器
                .options(options)
                .target(apiClass, serviceUrl);
    }

    /**
     * 极简代理创建：普通方法，依赖Spring注入的契约/拦截器, 指定读取超时时间
     */
    public <T> T createProxy(Class<T> apiClass, String serviceUrl,int readTimeout) {
        // 构建超时配置（无过时警告）
        Request.Options options = new Request.Options(
                connectTimeout, TimeUnit.MILLISECONDS,
                readTimeout, TimeUnit.MILLISECONDS,
                true // 跟随重定向
        );

        return Feign.builder()
                .encoder(feignEncoder)
                .decoder(feignDecoder)
                .contract(feignContract)        // 注入的契约（父类/子类实现）
                .requestInterceptor(istioHeaderInterceptor) // 注入的Istio拦截器
                .options(options)
                .target(apiClass, serviceUrl);
    }
}
