package xyz.zhiwei.cognitivedesign.dao.impl.rdb;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;

import xyz.zhiwei.cognitivedesign.dao.BaseDaoImpl;
import xyz.zhiwei.cognitivedesign.morphism.Principle;
import xyz.zhiwei.cognitivedesign.morphism.support.qualifier.PrincipleQualifier;
import xyz.zhiwei.cognitivedesign.morphism.support.source.PrincipleSourcePage;



/**
 * 
 * @updateBy zhanghaiting
 * @param <P>
 */
public class BaseRdbDaoImpl<P extends Principle<Long>> extends BaseDaoImpl<P> {
    
    @Autowired
    protected SqlSessionFactory sqlSessionFactory;
    @Autowired 
    protected SqlSessionTemplate sqlSessionTemplate;
    
    protected final String mapperNamespace;

    //init
	public BaseRdbDaoImpl() {
        Class<?> currentClass = this.getClass();
    	this.mapperNamespace = currentClass.getName()+".";
    }


	
	//==========================================查询===========================================

	//===================指定查询===================

	protected List<P> pk(PrincipleQualifier<P> qualifier) {
        return Stream.of(pk(qualifier.getSimilar())).toList();
	}
	protected P pk(P p) {
        String statementName = mapperNamespace + "selectByPrimaryKey";
        return sqlSessionTemplate.selectOne(statementName, p);
	}

	
    protected List<P> pkList(PrincipleQualifier<P> qualifier){
    	return pkList(qualifier.getSimilarList());
	}
    protected List<P> pkList(List<P> similarList){
		if(null==similarList|| similarList.size()==0){
			return new ArrayList<>();
		}
        String statementName = mapperNamespace + "selectByPrimaryKeyList";
        return sqlSessionTemplate.selectList(statementName, similarList);
	}
	//===================条件查询===================

	protected List<P> uk(PrincipleQualifier<P> qualifier) {
        return Stream.of(uk(qualifier.getSimilar())).toList();
	}
	protected P uk(P p) {
        String statementName = mapperNamespace + "selectOneByExample";
        return sqlSessionTemplate.selectOne(statementName, p);
	}
	
	
    protected List<P> fk(PrincipleQualifier<P> qualifier) {
        return fk(qualifier.getSimilar());
    }
    protected List<P> fk(P p) {
        String statementName = mapperNamespace + "selectByExample";
        return sqlSessionTemplate.selectList(statementName, p);
    }
    
   
    protected List<P> fkList(PrincipleQualifier<P> qualifier){
        return fkList(qualifier.getSimilarList());
	}
    protected List<P> fkList(List<P> similarList){
		if(null==similarList|| similarList.size()==0){
			return new ArrayList<>();
		}
        String statementName = mapperNamespace + "selectByExampleList";
        return sqlSessionTemplate.selectList(statementName, similarList);
	}

    
	protected PrincipleSourcePage<P> page(PrincipleQualifier<P> qualifier){
		return page(qualifier,"selectByExample");
	}
	protected PrincipleSourcePage<P> page(PrincipleQualifier<P> qualifier,String sqlName){
		return page(qualifier.getSimilar(),qualifier.getPageNum(),qualifier.getPageSize(),sqlName);
	}
	protected <T> PrincipleSourcePage<P> page(T t,Integer pageNum,Integer pageSize,String sqlName){
		List<P> listInPage=null;
		PageHelper.startPage(pageNum, pageSize);
        String statementName = mapperNamespace + sqlName;
		listInPage=sqlSessionTemplate.selectList(statementName, t);
		Long total=0L;
        if(listInPage instanceof Page){
            total = ((Page<?>)listInPage).getTotal();
        } else {
            total = Long.valueOf(listInPage.size());
        }
		return new PrincipleSourcePage<P>(listInPage,total);
	}
    
	//==========================================更新类方法===========================================

    /** 批量添加 */
	@Override
	protected Long addList(List<P> list) {
    	return insertBatch(list,"insert");
    }

    /** 批量更新 */
	@Override
	protected Long updateList(List<P> list) {
    	return updateBatch(list,"updateByPrimaryKey");
    }

    /** 批量删除 */
	@Override
	protected Long deleteList(List<P> list) {
    	return deleteBatch(list,"deleteByPrimaryKey");
    }
	
	
	
    /** 批量添加 */
	protected Long insertBatch(List<P> list, String sqlName) {
        return batchExecute(list, sqlName, (sqlSession, statementName) ->
            model -> sqlSession.insert(statementName, model) 
        );
    }

    /** 批量更新 */
	protected Long updateBatch(List<P> list, String sqlName) {
        return batchExecute(list, sqlName, (sqlSession, statementName) ->
            model -> sqlSession.update(statementName, model) 
        );
    }

    /** 批量删除 */
	protected Long deleteBatch(List<P> list, String sqlName) {
        return batchExecute(list, sqlName, (sqlSession, statementName) ->
            model -> sqlSession.delete(statementName, model)
        );
    }
    
    /**
     * 通用批量执行方法
     * @param list 待操作数据列表
     * @param sqlName Mapper 中操作方法的 ID（如 "insert"/"updateById"/"deleteById"）
     * @param operateFunc 操作类型函数（insert/update/delete）
     * @return 成功操作条数
     */
    private Long batchExecute(
            List<P> list,
            String sqlName,
            BiFunction<SqlSession, String, Function<P, Integer>> operateFunc) {
    	
        Long operateCount = 0L;
        // 空列表日志直接打印 sqlName
        if (CollectionUtils.isEmpty(list)) {
            log.warn("批量操作[{}]：待操作列表为空，直接返回", sqlName);
            return operateCount;
        }

        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false);
        String statementName = mapperNamespace + sqlName;

        try {
            for (int i = 0; i < list.size(); i++) {
                P model = null;
                try {
                    model = list.get(i);
                } catch (Exception e) {
                    // 单条失败日志打印 sqlName
                    log.warn("批量操作[{}]：第{}条数据获取失败，跳过", sqlName, i, e);
                    continue;
                }

                // 内联操作函数执行核心逻辑
                operateCount += operateFunc.apply(sqlSession, statementName).apply(model);

                // 分批次刷盘日志打印 sqlName
                if ((i + 1) % 200 == 0) {
                    sqlSession.flushStatements();
                    sqlSession.clearCache();
                    log.info("批量操作[{}]：已处理{}条，触发刷盘", sqlName, i + 1);
                }
            }
            // 刷出剩余SQL
            sqlSession.flushStatements();
            // 完成日志打印 sqlName 和统计信息
            log.info("批量操作[{}]完成：共处理{}条，成功{}条", sqlName, list.size(), operateCount);
        } catch (Exception e) {
            // 异常直接打印 sqlName，无中文转换
            log.error("批量操作[{}]失败", sqlName, e);
            throw new RuntimeException("批量操作[" + sqlName + "]失败：" + e.getMessage(), e);
        } finally {
            if (sqlSession != null) {
                sqlSession.clearCache();
            }
        }
        return operateCount;
    }
    
    
	
}
