package xyz.zhiwei.cognitivedesign.dao.impl.session;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import xyz.zhiwei.cognitivedesign.dao.Dao;
import xyz.zhiwei.cognitivedesign.dao.impl.session.config.RedisConfigSession;
import xyz.zhiwei.cognitivedesign.morphism.Principle;
import xyz.zhiwei.cognitivedesign.morphism.support.qualifier.PrincipleQualifier;



/**
 * 
 * @author zhanghaiting
 * @param <U>
 *
 */
public class BaseSessionDaoImpl<P extends Principle<String>> implements Dao<P> {

    final Integer timeOut=1;
	
	@Autowired
	@Qualifier(RedisConfigSession.SessionRedisTemplate)
	protected RedisTemplate<String, Object> redisTemplate;

	@Autowired
	@Qualifier(RedisConfigSession.SessionObjectMapper)
    protected ObjectMapper objectMapper;

    private Class<P> targetClazz; // 缓存泛型P的真实类型，仅初始化一次
    
    
    // 初始化方法：在Bean创建后解析泛型类型并缓存（推荐@PostConstruct，兼容Spring依赖注入）
    @PostConstruct
    private void initGenericType() {
        this.targetClazz = getGenericType();
    }
    
    
    
    
	//==========================================接口集===========================================
	
	

	@Override
	public List<P> subCollection(PrincipleQualifier<P> qualifier) {
		if(null!=qualifier.getSimilar()) {
			P similar=qualifier.getSimilar();
			if(null!=similar.getId()) {
				P p=getSessionAttribute(similar);
				return Arrays.asList(p) ;
			}
		}
		return null;
	}

	
	@Override
	public Long add(List<P> list) {
		int cc=0;
		for(P p:list) {
			cc+=saveSession(p);
		}
		return Long.valueOf(cc);
	}


	@Override
	public Long update(List<P> list) {
		return add(list);
	}
	

	@Override
	public Long delete(List<P> list) {
		int cc=0;
		for(P p:list) {
			cc+=invalidate(p);
		}
		return Long.valueOf(cc);
	}
	
	/*
	 * ===============================  session生命周期方法 ============================================
	 */

    // ========== 保存方法（使用缓存的泛型类型，可选：仅校验类型） ==========
    protected Integer saveSession(P p) {
        // 1. 空值+类型校验（可选，确保存入的是目标类型）
        Assert.notNull(p, "会话模型P不能为空");
        Assert.notNull(p.getId(), "会话模型P的token(id)不能为空");
        Assert.notNull(timeOut, "超时时间timeOut未配置");
        Assert.isTrue(targetClazz.isInstance(p), "存入的对象类型与泛型类型不匹配");

        String token = p.getId();
        try {
            // 2. 纯JSON序列化（无类型字段）
            String pureJson = objectMapper.writeValueAsString(p);
            redisTemplate.opsForValue().set(token, pureJson, timeOut, TimeUnit.HOURS);
            return 1;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("保存会话到Redis失败，token=" + token, e);
        }
    }

    // ========== 获取方法（直接使用缓存的泛型类型） ==========
    protected P getSessionAttribute(P p) {
        // 1. 空值校验
        if (p == null || p.getId() == null) {
            return null;
        }
        String token = p.getId();

        try {
            // 从Redis读取纯JSON字符串
            String pureJson = (String) redisTemplate.opsForValue().get(token);
            if (pureJson == null) {
                return null;
            }
            // 2. 直接使用缓存的targetClazz反序列化（无需重复反射）
            P result = objectMapper.readValue(pureJson, targetClazz);
            // 3. 重置过期时间
            redisTemplate.expire(token, timeOut, TimeUnit.HOURS);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("从Redis读取会话失败，token=" + token, e);
        }
    }
    
	/**
	 * 删除session
	 */
	private Integer invalidate(P p) {

		String token=p.getId();
		
		if(null==token) {
			return 0;
		}
		boolean sign=redisTemplate.delete(token);
		return sign?1:0;
	}
	
	



	/*
	 * ===============================  辅助方法 ============================================
	 */
	





    // 反射解析泛型类型（仅执行一次）
    private Class<P> getGenericType() {
        Type genericSuperclass = this.getClass().getGenericSuperclass();
        // 兼容多层继承场景（如子类又被继承）
        while (!(genericSuperclass instanceof ParameterizedType)) {
            if (genericSuperclass == Object.class) {
                throw new IllegalArgumentException("当前类的继承链中未找到泛型类型参数");
            }
            genericSuperclass = ((Class<?>) genericSuperclass).getGenericSuperclass();
        }

        ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        if (actualTypeArguments.length == 0) {
            throw new IllegalArgumentException("未找到泛型类型参数");
        }

        // 强转为Class<P>（确保类型安全）
        @SuppressWarnings("unchecked")
        Class<P> clazz = (Class<P>) actualTypeArguments[0];
        return clazz;
    }




    
}