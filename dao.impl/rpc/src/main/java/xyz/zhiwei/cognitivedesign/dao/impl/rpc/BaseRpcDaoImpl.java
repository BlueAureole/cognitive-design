package xyz.zhiwei.cognitivedesign.dao.impl.rpc;

import java.util.List;

import xyz.zhiwei.cognitivedesign.dao.BaseDaoImpl;
import xyz.zhiwei.cognitivedesign.morphism.Principle;



/**
 * 
 * @updateBy zhanghaiting
 * TODO 
 *    1  添加认证信息
 *    2  实现事务支持(表记录)
 * 
 * @param <P>
 */
public class BaseRpcDaoImpl<P extends Principle<Long>> extends BaseDaoImpl<P> {

	@Override
	protected Long addList(List<P> list) {
		return null;
	}

	@Override
	protected Long updateList(List<P> list) {
		return null;
	}

	@Override
	protected Long deleteList(List<P> list) {
		return null;
	}
    
	
}
