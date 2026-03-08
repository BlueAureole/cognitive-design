package xyz.zhiwei.cognitivedesign.dao.daoimpl.methodcache;

/**
 * 保存的方法名前缀
 */
public enum SaveMethodPrefixEnum {
	ADD("add"),
	INSERT("insert"),
	UPDATE("update"),
	DELETE("delete");

	private final String value;

	SaveMethodPrefixEnum(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
	
	

	
	
	
    public static boolean isAddName(String desc) {
    	if(null==desc) {
    		return false;
    	}
		if(desc.startsWith(ADD.getValue())||desc.startsWith(INSERT.getValue())) {
			return true;
		}
		return false;
    }
    public static boolean isUpdateName(String desc) {
    	if(null==desc) {
    		return false;
    	}
		if(desc.startsWith(UPDATE.getValue())) {
			return true;
		}
		return false;
    }
    public static boolean isDeleteName(String desc) {
    	if(null==desc) {
    		return false;
    	}
		if(desc.startsWith(DELETE.getValue())) {
			return true;
		}
		return false;
    }
	
}
