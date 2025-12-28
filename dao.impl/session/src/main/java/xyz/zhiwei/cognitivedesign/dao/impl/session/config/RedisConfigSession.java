package xyz.zhiwei.cognitivedesign.dao.impl.session.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

/**
* @author zhanghaiting
*/

public class RedisConfigSession extends RedisConfigBase{
	
	public static final String SessionRedisTemplate="sessionRedisTemplate";
	public static final String SessionObjectMapper="sessionObjectMapper";
    
	@Override
	public boolean initRedisConfig(Environment env) {
    	this.maxActive=Integer.valueOf(env.getProperty("spring.redis.jedis.pool.max-active"));
    	this.maxWait=Integer.valueOf(env.getProperty("spring.redis.jedis.pool.max-wait"));
    	this.maxIdle=Integer.valueOf(env.getProperty("spring.redis.jedis.pool.max-idle"));
    	this.minIdle=Integer.valueOf(env.getProperty("spring.redis.jedis.pool.min-idle"));
    	
		this.servers=env.getProperty("spring.sessionRedis.host");
		this.port=Integer.valueOf(env.getProperty("spring.sessionRedis.port"));
		this.datebase=Integer.valueOf(env.getProperty("spring.sessionRedis.database"));
		this.password=env.getProperty("spring.sessionRedis.password");
		this.timeout=Long.valueOf(env.getProperty("spring.sessionRedis.timeout"));
		return true;
	}
	
	
	
	@Bean(name = SessionRedisTemplate)
	public RedisTemplate<String, Object> redisTemplate(Environment env){
		initRedisConfig(env);
		return redisTemplate(getRedisConnectionFactory());
	}
	

    @Bean(name = SessionObjectMapper)
    public ObjectMapper objectMapper() {
        ObjectMapper om = new ObjectMapper();

        // 1. 核心：确保日期序列化为时间戳（针对java.util.Date）
        // WRITE_DATES_AS_TIMESTAMPS默认是true，显式开启更清晰（禁用则序列化为字符串）
        om.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 2. 基础容错配置（保留你原有推荐配置）
        om.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false); // 空对象不报错
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // 忽略未知字段

        // 3. 关键：配置JDK8+日期类型（LocalDateTime/LocalDate）序列化为时间戳
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        // 3.1 LocalDateTime 序列化为毫秒级时间戳
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer() {
			private static final long serialVersionUID = 1L;

			@Override
            public void serialize(LocalDateTime value, com.fasterxml.jackson.core.JsonGenerator gen, com.fasterxml.jackson.databind.SerializerProvider serializers) throws java.io.IOException {
                // 转换为毫秒级时间戳（默认UTC，如需本地时区则调整ZoneId）
                long timestamp = value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                gen.writeNumber(timestamp);
            }
        });
        // 3.2 LocalDate 序列化为日期对应的毫秒级时间戳（00:00:00）
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer() {
			private static final long serialVersionUID = 1L;

			@Override
            public void serialize(LocalDate value, com.fasterxml.jackson.core.JsonGenerator gen, com.fasterxml.jackson.databind.SerializerProvider serializers) throws java.io.IOException {
                long timestamp = value.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                gen.writeNumber(timestamp);
            }
        });
        om.registerModule(javaTimeModule);


        return om;
    }
    
    
    
    
}
