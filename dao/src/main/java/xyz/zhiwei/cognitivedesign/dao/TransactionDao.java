package xyz.zhiwei.cognitivedesign.dao;

import javax.sql.XADataSource;

import xyz.zhiwei.cognitivedesign.morphism.Principle;

/**
 * 本原集存取-事务并发度控制支持
 * @updateBy zhanghaiting
 */
public interface TransactionDao<P extends Principle<?>> extends Dao<P>{

	/**
	 * 事务可见范围标识键
	 * 事务中同一标识键的操作在同一线程中（使用同一个数据库连接）。以保证有相互影响的数据更新对有关联的操作可见。
	 * @return
	 * 1  如果这个表写入数据不依赖也不影响其他表的写入，那么返回当前Dao自身。（也即这个表独立成库时，不需要修改任何代码中的sql）
	 * 2  如果这个表写入数据依赖或影响其他表的写入，那么返回所在的数据源。（例如update otherDao where otherDao.id=thisDao.id）
	 */
    Object getTransactionVisibilityKey();
    
    /**
     * 获取数据源
     * 用于跨线程事务管理
     * @return XADataSource (实际运行时必须也是 DataSource)
     */
    XADataSource getXADataSource();
    
}
