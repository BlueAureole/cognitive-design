
package xyz.zhiwei.cognitivedesign.morphism;


import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 本原
 * @author zhanghaiting
 */
public class Principle<ID> implements Serializable {

    private static final long serialVersionUID = 10L;
	protected  Class<ID> idClazz;

    protected ID id;//Integer/Long/String
    protected String name;
    
    
    public Principle() {}
    
    public Principle(ID id) {
    	this.id=id;
    }
    public Principle(ID id,String name) {
    	this.id=id;
    	this.name=name;
    }

	public ID getId() {
		return id;
	}
	public void setId(ID id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
        this.name = name == null ? null : name.trim();
	}

	
	
	/**
	 * 获取泛型的类型
	 */
	@SuppressWarnings("unchecked")
	public Class<ID> queryIdClazz() {
		if(null==this.idClazz){
	        Type genType = this.getClass().getGenericSuperclass();
	        Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
	        this.idClazz = (Class<ID>) params[0];
		}
		return this.idClazz;
    }

}
