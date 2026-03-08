package xyz.zhiwei.cognitivedesign.dao;

import java.util.List;
import java.util.concurrent.Executor;

import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

import xyz.zhiwei.cognitivedesign.dao.accessimpl.DaoBeanCache;
import xyz.zhiwei.cognitivedesign.dao.accessimpl.read.ReadAccess;
import xyz.zhiwei.cognitivedesign.dao.accessimpl.write.WriteAccess;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.container.ImagePackage;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.response.ResponsePackage;
import xyz.zhiwei.cognitivedesign.morphism.principle.source.container.PrincipleSourceLane;
import xyz.zhiwei.cognitivedesign.morphism.principle.source.qualifier.QualifiersLane;
import xyz.zhiwei.cognitivedesign.service.PrincipleAccessInterface;


/**
 * 本原集存取实现
 */
public class PrincipleAccessImpl implements PrincipleAccessInterface{

    //====构建依赖====
    private ApplicationContext context;
    private JtaTransactionManager jtaTransactionManager;
    private Executor daoScheduleExecutor;
    private Executor daoReadExecutor;
    private Executor daoWriteExecutor;
    

    //====运行依赖====
    private DaoBeanCache daoBeanCache;
    private ReadAccess readAccess;
    private WriteAccess writeAccess;
    
    
    public PrincipleAccessImpl(ApplicationContext context,PlatformTransactionManager transactionManager,
    		Executor daoScheduleExecutor,Executor daoReadExecutor,Executor daoWriteExecutor) {
    	
    	
    	if (!(transactionManager instanceof JtaTransactionManager)) {
    		throw new IllegalArgumentException("DaoImpl requires JtaTransactionManager for distributed transaction support");
    	}
    	
    	this.context=context;
    	this.jtaTransactionManager=(JtaTransactionManager)transactionManager;
    	this.daoScheduleExecutor=daoScheduleExecutor;
    	this.daoReadExecutor=daoReadExecutor;
    	this.daoWriteExecutor=daoWriteExecutor;
    	
    	this.daoBeanCache=new DaoBeanCache(this.context);
    	this.readAccess=new ReadAccess(this.daoBeanCache,this.daoScheduleExecutor,this.daoReadExecutor);
    	this.writeAccess=new WriteAccess(this.daoBeanCache,this.jtaTransactionManager,this.daoScheduleExecutor,this.daoWriteExecutor);
    	
    }
    
    
    
    
    
    /**
     * 读取指定数据集
     * @param qualifiersLaneList
     * @return
     */
    @Override
    public List<PrincipleSourceLane> query(List<QualifiersLane> qualifiersLaneList){

    	
    	return readAccess.query(qualifiersLaneList);
    }
    

	
	/**
	 * 存储相关数据集
	 * 
	 * @param imagePackage
	 * @return
	 */
    @Override
	public ResponsePackage save(ImagePackage imagePackage) {
    	
    	return writeAccess.save(imagePackage);
    }
    
    
    
	
    
}
