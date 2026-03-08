package xyz.zhiwei.cognitivedesign.dao.impl.rpc.transaction.recorder;

import javax.sql.XADataSource;

import org.mybatis.spring.SqlSessionTemplate;

import xyz.zhiwei.cognitivedesign.dao.impl.rpc.transaction.TransactionRecordDao;
import xyz.zhiwei.cognitivedesign.dao.impl.rpc.transaction.model.TransactionRecord;

/**
* @author zhanghaiting
*/

public class TransactionRecordDaoImpl implements TransactionRecordDao {

    protected XADataSource xADataSource;
    protected SqlSessionTemplate sqlSessionTemplate;

    protected final String OP_ADD = "insert";
    protected final String OP_UPDATE_BY_TXKEY = "updateByTxKey";
    protected final String OP_DELETE_BY_TXKEY = "deleteByTxKey";
    
    
    protected final String mapperNamespace;


    

	@Override
	public XADataSource getXADataSource() {
		return xADataSource;
	}
	
    
    //init
	public TransactionRecordDaoImpl(XADataSource xADataSource,SqlSessionTemplate sqlSessionTemplate) {
        Class<?> currentClass = this.getClass();
    	this.mapperNamespace = currentClass.getName()+".";
		this.xADataSource=xADataSource;
		this.sqlSessionTemplate=sqlSessionTemplate;
	}


	@Override
	public Integer add(TransactionRecord model) {
        String statementName = mapperNamespace + OP_ADD;
        Integer cc=sqlSessionTemplate.insert(statementName, model);
        return cc;
	}


	@Override
	public Integer updateByTxKey(TransactionRecord model) {
        String statementName = mapperNamespace + OP_UPDATE_BY_TXKEY;
        Integer cc=sqlSessionTemplate.update(statementName, model);
        return cc;
	}
	

	@Override
    public Integer deleteByTxKey(TransactionRecord model) {
        String statementName = mapperNamespace + OP_DELETE_BY_TXKEY;
        Integer cc=sqlSessionTemplate.delete(statementName, model);
        return cc;
    }


	
	
}
