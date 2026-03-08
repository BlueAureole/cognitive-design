package xyz.zhiwei.cognitivedesign.dao.impl.rpc.transaction;

import javax.sql.XADataSource;

import xyz.zhiwei.cognitivedesign.dao.impl.rpc.transaction.model.TransactionRecord;

/**
* @author zhanghaiting
*/

public interface TransactionRecordDao {
	

    XADataSource getXADataSource();

    Integer add(TransactionRecord model);

    Integer updateByTxKey(TransactionRecord model);

    Integer deleteByTxKey(TransactionRecord model);
    
}
