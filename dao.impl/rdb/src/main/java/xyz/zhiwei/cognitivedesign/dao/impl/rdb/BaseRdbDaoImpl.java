package xyz.zhiwei.cognitivedesign.dao.impl.rdb;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.SqlSessionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;

import xyz.zhiwei.cognitivedesign.dao.TransactionDao;
import xyz.zhiwei.cognitivedesign.dao.daoimpl.BaseDaoImpl;
import xyz.zhiwei.cognitivedesign.morphism.Principle;
import xyz.zhiwei.cognitivedesign.morphism.principle.source.container.PrinciplePage;
import xyz.zhiwei.cognitivedesign.morphism.principle.source.qualifier.PrincipleQualifier;




/**
 * 
 * @updateBy zhanghaiting
 * @param <P>
 */
public abstract class BaseRdbDaoImpl<P extends Principle<Long>> extends BaseDaoImpl<P> implements TransactionDao<P> {
	protected Logger log;
	
	
	private static final int MYBATIS_NO_ROW_COUNT = -2147482646;
	protected static int BATCH_SIZE = 200; // 批处理批次大小
    
    protected final String mapperNamespace;

    
    private enum OperateType {
        INSERT, UPDATE, DELETE
    }
    
    //init
	public BaseRdbDaoImpl() {
        Class<?> currentClass = this.getClass();
		this.log = LoggerFactory.getLogger(currentClass);
    	this.mapperNamespace = currentClass.getName()+".";
    }

	
	protected abstract SqlSessionFactory getSqlSessionFactory();
	protected abstract SqlSessionTemplate getSqlSessionTemplate();

	
	@Override
	public Object getTransactionVisibilityKey() {
		return getXADataSource();
	}

	
	//==========================================查询===========================================


	//===================指定查询===================

	protected List<P> pk(PrincipleQualifier<P> qualifier) {
        return Stream.of(pk(qualifier.getSimilar())).toList();
	}
	protected P pk(P p) {
        String statementName = mapperNamespace + "selectByPrimaryKey";
        return getSqlSessionTemplate().selectOne(statementName, p);
	}

	
    protected List<P> pkList(PrincipleQualifier<P> qualifier){
    	return pkList(qualifier.getSimilarList());
	}
    protected List<P> pkList(List<P> similarList){
		if(null==similarList|| similarList.size()==0){
			return new ArrayList<>();
		}
        String statementName = mapperNamespace + "selectByPrimaryKeyList";
        return getSqlSessionTemplate().selectList(statementName, similarList);
	}
	//===================条件查询===================

	protected List<P> uk(PrincipleQualifier<P> qualifier) {
        return Stream.of(uk(qualifier.getSimilar())).toList();
	}
	protected P uk(P p) {
        String statementName = mapperNamespace + "selectOneByExample";
        return getSqlSessionTemplate().selectOne(statementName, p);
	}
	
	
    protected List<P> fk(PrincipleQualifier<P> qualifier) {
        return fk(qualifier.getSimilar());
    }
    protected List<P> fk(P p) {
        String statementName = mapperNamespace + "selectByExample";
        return getSqlSessionTemplate().selectList(statementName, p);
    }
    
   
    protected List<P> fkList(PrincipleQualifier<P> qualifier){
        return fkList(qualifier.getSimilarList());
	}
    protected List<P> fkList(List<P> similarList){
		if(null==similarList|| similarList.size()==0){
			return new ArrayList<>();
		}
        String statementName = mapperNamespace + "selectByExampleList";
        return getSqlSessionTemplate().selectList(statementName, similarList);
	}

    
	protected PrinciplePage<P> page(PrincipleQualifier<P> qualifier){
		return page(qualifier,"selectByExample");
	}
	protected PrinciplePage<P> page(PrincipleQualifier<P> qualifier,String sqlName){
		return page(qualifier.getSimilar(),qualifier.getPageNum(),qualifier.getPageSize(),sqlName);
	}
	protected <T> PrinciplePage<P> page(T t,Integer pageNum,Integer pageSize,String sqlName){
		List<P> listInPage=null;
		PageHelper.startPage(pageNum, pageSize);
        String statementName = mapperNamespace + sqlName;
		listInPage=getSqlSessionTemplate().selectList(statementName, t);
		Long total=0L;
        if(listInPage instanceof Page){
            total = ((Page<?>)listInPage).getTotal();
        } else {
            total = Long.valueOf(listInPage.size());
        }
		return new PrinciplePage<P>(listInPage,total);
	}
	
	
	
	//==========================================更新类方法 暴露给Dao===========================================

    /** 批量添加 */
	protected Long add(List<P> list) {
    	return insertBatch(list,"insert");
    }

    /** 批量更新 */
	protected Long update(List<P> list) {
    	return updateBatch(list,"updateByPrimaryKey");
    }

    /** 批量删除 */
	protected Long delete(List<P> list) {
    	return deleteBatch(list,"deleteByPrimaryKey");
    }
	

	//==========================================更新类方法 子类可使用===========================================


	
    /** 批量添加 */
    protected Long insertBatch(List<P> list, String sqlName) {
        return batchExecute(list, sqlName,OperateType.INSERT,
                (sqlSession, statementName) -> {
                    return model -> {
                        Integer count=sqlSession.insert(statementName, model);
                        return count;
                    };
                }
        );
    }

    /** 批量更新 */
	protected Long updateBatch(List<P> list, String sqlName) {
        return batchExecute(list, sqlName, OperateType.UPDATE,
        		(sqlSession, statementName) ->{
        			return model -> {
        				Integer count=sqlSession.update(statementName, model);
        				return count;
        			};
        		}
            
        );
    }

    /** 批量删除 */
	protected Long deleteBatch(List<P> list, String sqlName) {
        return batchExecute(list, sqlName, OperateType.DELETE,
        		(sqlSession, statementName) ->{
        			return model -> {
        				Integer count=sqlSession.delete(statementName, model);
        				return count;
        			};
        		}
            
        );
    }



	//==========================================更新类方法 私有===========================================

    
    /**
     * 通用批量执行方法（核心：原生SqlSession+适配上层事务+精准统计真实影响数）
     *
     * @param list         待操作数据列表
     * @param sqlName      Mapper中操作方法的ID
     * @param operateType  操作类型
     * @param operateFunc  操作执行函数
     * @return 数据库真实影响行数（100%精准）
     */
    private Long batchExecute(
            List<P> list,
            String sqlName,
            OperateType operateType,
            BiFunction<SqlSession, String, Function<P, Integer>> operateFunc) {
        long realOperateCount = 0L;

        // 空列表直接返回0
        if (CollectionUtils.isEmpty(list)) {
            log.warn("批量操作[{}]：待操作列表为空，直接返回0", sqlName);
            return realOperateCount;
        }

        String statementName = mapperNamespace + sqlName;
        
        
        // JTA 事务相关
        //Transaction currentTransaction = null;
        Boolean isInTransactional = false;
        SqlSession sqlSession = null;
        

        
		try {    
			
			
			// 1. 获取当前线程的 JTA 事务对象
		    isInTransactional = TransactionSynchronizationManager.isActualTransactionActive();
		    log.debug("批量操作[{}][{}]：JTA 事务存在状态：{}", operateType.name(), sqlName, isInTransactional);

		    
		    // 2. 获取 SqlSession（Spring 会自动绑定 Holder，无需手动操作）
		    sqlSession = SqlSessionUtils.getSqlSession(
		        getSqlSessionFactory(),
		        ExecutorType.BATCH,
		        null
		    );

            // 循环执行批处理
            for (int i = 0; i < list.size(); i++) {
                P model = list.get(i);
                if(null==model) {
                	continue;
                }
                // 执行批处理SQL（加入队列，不立即执行）
                Function<P, Integer> sqlTempFunc = operateFunc.apply(sqlSession, statementName);
                sqlTempFunc.apply(model);

                // 分批次刷盘（每BATCH_SIZE条执行一次，获取真实行数）
                if ((i + 1) % BATCH_SIZE == 0) {
                    realOperateCount += flushAndCount(sqlSession, operateType, sqlName, i + 1);
                }
            }

            // 刷出剩余所有SQL，统计最终行数
            realOperateCount += flushAndCount(sqlSession, operateType, sqlName, list.size());

            log.debug("批量操作[{}][{}]完成：提交参数{}条，数据库真实影响行数：{}",
                    operateType.name(), sqlName, list.size(), realOperateCount);

            
		} catch (Exception e) {
		    log.error("批量操作[{}][{}]执行失败", operateType.name(), sqlName, e);
		    // 触发上层事务回滚（若有）
		    throw new RuntimeException("批量操作[" + operateType.name() + "-" + sqlName + "]失败：" + e.getMessage(), e);
		} finally {
			
			
		    if (sqlSession != null) {
		        try {
		            if (!isInTransactional) {
		                sqlSession.commit();
		            }
		            //  智能关闭 SqlSession（适配事务/非事务）
	                SqlSessionUtils.closeSqlSession(sqlSession, getSqlSessionFactory());
		        } catch (Exception closeE) {
		            log.error("批量操作[{}][{}]：关闭 SqlSession 失败", operateType.name(), sqlName, closeE);
		        }
		    }
		}

        return realOperateCount;
    }

    
    /**
     * 刷盘并统计真实影响行数（核心工具方法）
     */
    private long flushAndCount(SqlSession sqlSession, OperateType operateType, String sqlName, int processedNum) {
        List<BatchResult> batchResults = sqlSession.flushStatements();
        long batchRealCount = getRealAffectRows(batchResults);
        
        log.debug("批量操作[{}][{}]：已处理{}条参数，本次刷盘真实影响行数：{}",
                operateType.name(), sqlName, processedNum, batchRealCount);
        
        // 清空缓存，避免内存溢出
        sqlSession.clearCache();
        return batchRealCount;
    }

    /**
     * 提取真实影响行数（100%精准，无过滤偏差）
     */
    private long getRealAffectRows(List<BatchResult> batchResults) {
        if (CollectionUtils.isEmpty(batchResults)) {
            return 0L;
        }

        long realCount = 0L;
        for (BatchResult result : batchResults) {
            int[] updateCounts = result.getUpdateCounts();
            if (updateCounts == null || updateCounts.length == 0) {
                continue;
            }

            // 累加每条SQL的真实影响行数（过滤MyBatis无意义标记值）
            updateCounts = result.getUpdateCounts();
            if (updateCounts == null || updateCounts.length == 0) {
                continue;
            }

            // 累加每条SQL的真实影响行数（过滤MyBatis无意义标记值）
            for (int count : updateCounts) {
                if (count != MYBATIS_NO_ROW_COUNT) {
                    realCount += count; // 直接累加（update/delete可能为0，insert必为1）
                }
            }
        }
        return realCount;
    }


    

}
