package xyz.zhiwei.cognitivedesign.dao.impl.session.config;

import java.time.Duration;

import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import redis.clients.jedis.JedisPoolConfig;

public abstract class RedisConfigBase {

    // 连接池配置（强制配置，无默认值）
    protected Integer maxActive;
    protected Integer maxWait; // 单位：毫秒
    protected Integer maxIdle;
    protected Integer minIdle;

    // Redis服务器配置（强制配置，无默认值）
    protected String servers;
    protected Integer port;
    protected Integer datebase;
    protected String password;
    protected Long timeout; // 单位：毫秒

    // 连接工厂和模板（复用避免重复创建）
    protected RedisConnectionFactory connectionFactory = null;
    protected RedisTemplate<String, Object> template = null;

    // 抽象方法：由子类实现配置初始化（需赋值所有属性，包括packageName）
    protected abstract boolean initRedisConfig(Environment env);

    /**
     * 获取Redis连接工厂（单例模式）- 强制配置，未配置则抛异常
     */
    protected RedisConnectionFactory getRedisConnectionFactory() {
        if (this.connectionFactory != null) {
            return this.connectionFactory;
        }

        // 1. 基础Redis服务器配置（无空值判断，未配置则NPE，强制配置）
        RedisStandaloneConfiguration rsc = new RedisStandaloneConfiguration();
        rsc.setHostName(servers);
        rsc.setPort(port);
        rsc.setDatabase(datebase);
        rsc.setPassword(RedisPassword.of(password));

        // 2. 配置Jedis客户端参数（timeout强制配置）
        JedisClientConfiguration.JedisClientConfigurationBuilder clientConfigBuilder = JedisClientConfiguration.builder();
        clientConfigBuilder.connectTimeout(Duration.ofMillis(timeout));
        clientConfigBuilder.readTimeout(Duration.ofMillis(timeout));

        // 3. 配置连接池（所有参数强制配置，未配置则NPE）
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(maxActive);
        poolConfig.setMaxWait(Duration.ofMillis(maxWait));
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);

        // 绑定连接池到客户端配置
        clientConfigBuilder.usePooling().poolConfig(poolConfig);

        // 4. 创建连接工厂
        JedisConnectionFactory connectionFactory = new JedisConnectionFactory(rsc, clientConfigBuilder.build());
        connectionFactory.afterPropertiesSet();
        this.connectionFactory = connectionFactory;
        return connectionFactory;
    }

    /**
     * 创建RedisTemplate
     */
    protected RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 全局使用String序列化（Key/Value均为字符串，Value存储纯JSON）
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        template.afterPropertiesSet();
        return template;
    }
}