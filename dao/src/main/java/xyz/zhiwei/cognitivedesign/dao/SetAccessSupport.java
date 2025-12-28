package xyz.zhiwei.cognitivedesign.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jakarta.annotation.PostConstruct;
import xyz.zhiwei.cognitivedesign.dao.utils.GenericDaoUtils;
import xyz.zhiwei.cognitivedesign.morphism.Principle;
import xyz.zhiwei.cognitivedesign.morphism.support.image.PrincipleImage;
import xyz.zhiwei.cognitivedesign.morphism.support.image.PrincipleImageResponse;
import xyz.zhiwei.cognitivedesign.morphism.support.qualifier.PrincipleQualifier;
import xyz.zhiwei.cognitivedesign.morphism.support.qualifier.PrincipleQualifiers;
import xyz.zhiwei.cognitivedesign.morphism.support.source.PrincipleSource;


/**
 * 本原集存取实现
 */
public class SetAccessSupport{
    private static final Logger log = LoggerFactory.getLogger(SetAccessSupport.class);
    public static final String NAME_daoQueryExecutor="daoQueryExecutor";
    public static final String NAME_daoSaveExecutor="daoSaveExecutor";
    // 注入Spring上下文（获取代理对象保证事务生效）
    @Autowired
    private ApplicationContext applicationContext;

    // 数据源
    @Autowired
    private DataSource dataSource;
    
    //查询线程池
    @Autowired
	@Qualifier(NAME_daoQueryExecutor)
    private Executor daoQueryExecutor;
    //写入线程池
    @Autowired
	@Qualifier(NAME_daoSaveExecutor)
    private Executor daoSaveExecutor;
    

    // 缓存：Class -> DaoImpl（如Account.class -> AccountDaoImpl）
    private final Map<Class<?>, Dao<?>> DAO_IMPL_CACHE = new ConcurrentHashMap<>();
    
    
    private Integer timeout=20;
    
    
    //初始化方法
    @PostConstruct
    public void init() {
    	initDaoCache(applicationContext);
    }
    

	/*
	 * =================================================接口方法区===============================================================
	 */
    

	
	/**
	 * 根据子集描述符组 返回 子集
	 * @param <P>
	 * @param qualifierGroup
	 * @return  结果容器List必不为null，至少返回空列表[]。
	 * 
     * List<BaseQualifier<?>> relatedQualifierList  ->  List<List<? extends Principle<?>>> relatedListList    
	 */
    PrincipleSource queryRelatedCollection(PrincipleQualifiers qualifierGroup) {
        // 入参空值兜底
        if (null == qualifierGroup) {
            return new PrincipleSource();
        }

        int size = qualifierGroup.size();
        // 原子数组存储结果（保证线程安全+索引对应）
        AtomicReferenceArray<List<? extends Principle<?>>> resultArray = new AtomicReferenceArray<>(size);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            int index = i; // 匿名内部类需final变量
            // 提交异步任务
            @SuppressWarnings("unchecked")
			CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                // 单个任务独立try-catch：所有异常仅在任务内处理
                try {
                    PrincipleQualifier<?> qualifier = qualifierGroup.get(index);
                    List<? extends Principle<?>> list;
                    
                    // 空值兜底逻辑（原有逻辑保留）
                    if (null == qualifier) {
                        list = new ArrayList<>();
                    } else {
                        @SuppressWarnings("rawtypes")
						Dao relatedDao = getDaoBeanByPrincipleClass(qualifier.getPrincipleClazz());
                        list = relatedDao.subCollection(qualifier);
                        // DAO返回null时兜底为空列表
                        list = Optional.ofNullable(list).orElse(new ArrayList<>());
                    }
                    
                    // 正常结果存入数组
                    resultArray.set(index, list);
                } catch (Exception e) {
                    // 核心：异常时仅记录日志，为该索引设置空结果，不影响其他任务
                    log.error("第{}个查询任务执行异常，已设置空结果", index, e);
                    resultArray.set(index, new ArrayList<>()); // 异常兜底空列表
                }
            }, this.daoQueryExecutor);
            
            futures.add(future);
        }

        // 等待所有任务完成（含超时控制）
        try {
            // 超时仅终止未完成任务，已完成任务结果保留
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            // 超时/中断时：仅取消未完成任务，已完成任务结果保留
            log.warn("查询任务超时/被中断，取消未完成任务", e);
            for (int i = 0; i < futures.size(); i++) {
                CompletableFuture<Void> future = futures.get(i);
                // 仅取消未完成的任务，并为其设置空结果
                if (!future.isDone()) {
                    future.cancel(true);
                    resultArray.set(i, new ArrayList<>()); // 超时/中断任务兜底空列表
                }
            }
        }

        // 按索引顺序组装结果（保证与入参顺序一致）
        PrincipleSource result = new PrincipleSource();
        for (int i = 0; i < size; i++) {
            // 兜底：防止极端情况（如任务未执行）数组值为null
            List<? extends Principle<?>> list = Optional.ofNullable(resultArray.get(i)).orElse(new ArrayList<>());
            result.add(list);
        }

        return result;
    }
	

	/**
	 * 存储相关数据集
     * 单条处理方法：独立事务粒度（核心改造：并行执行+事务注解）
     * 事务属性：REQUIRED（每次调用创建新事务）、READ_COMMITTED、所有异常回滚
	 * @param <P>
	 * @param relatedSegments
	 * @return
	 */
    @Transactional(propagation = Propagation.REQUIRED,
                   isolation = Isolation.READ_COMMITTED,
                   rollbackFor = Exception.class) 
    PrincipleImageResponse saveRelatedSegments(PrincipleImage relatedSegments) {
        if (null == relatedSegments) {
            return new PrincipleImageResponse();
        }

        // ========== 1. 主线程预取事务上下文（核心：只取一次，传递到所有异步） ==========
        Object mainTransaction = TransactionSynchronizationManager.getResource(dataSource);
        log.info("主线程预取事务上下文：{}，线程：{}", mainTransaction, Thread.currentThread().getName());

        // ========== 2. 第一级异步：传递mainTransaction到handleRelatedCollection ==========
        
        // 1. 定义三个操作的异步任务
        CompletableFuture<Map<Integer, Long>> addFuture = CompletableFuture.supplyAsync(() -> 
            handleRelatedCollection(relatedSegments.getAddition(), Dao::add,mainTransaction), this.daoSaveExecutor
        );
        
        CompletableFuture<Map<Integer, Long>> modifyFuture = CompletableFuture.supplyAsync(() -> 
            handleRelatedCollection(relatedSegments.getModification(), Dao::update,mainTransaction), this.daoSaveExecutor
        );
        
        CompletableFuture<Map<Integer, Long>> delFuture = CompletableFuture.supplyAsync(() -> 
            handleRelatedCollection(relatedSegments.getDeletion(), Dao::delete,mainTransaction), this.daoSaveExecutor
        );
        
        // 2. 等待所有并行任务完成
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(addFuture, modifyFuture, delFuture);
        
        try {
            // 3. 等待结果（设置超时时间，与内部保持一致30秒）
            allFutures.get(timeout, TimeUnit.SECONDS);
            
            // 4. 获取各任务结果并组装响应
            Map<Integer, Long> addCount = addFuture.join();
            Map<Integer, Long> modifyCount = modifyFuture.join();
            Map<Integer, Long> delCount = delFuture.join();
            
            return new PrincipleImageResponse(addCount, modifyCount, delCount);
        } catch (Exception e) {
            // 5. 处理异常（取消所有未完成任务）
            addFuture.cancel(true);
            modifyFuture.cancel(true);
            delFuture.cancel(true);
            
            Throwable rootCause = e instanceof CompletionException ? e.getCause() : e;
            throw new RuntimeException("并行处理新增/修改/删除操作失败", rootCause);
        }
    }

    
    
    
	/*
	 * =================================================私有方法区===============================================================
	 */
	
    
    
    /**
     * 并行批量写入/远程调用方法（基于Spring线程池，任一异常抛异常由上层回滚）
     * @param relatedCollection 待处理的批量数据
     * @param handleFunction 具体的写入/调用逻辑（Dao/HTTP客户端 + 数据列表 → 处理条数）
     * @param mainTransaction 主线程的事务上下文（新增参数，传递事务）
     * @return 每个索引对应的处理条数（仅全部成功返回；任一失败抛异常）
     * @throws RuntimeException 任一操作失败时抛出，包含失败批次、原因等上下文
     */
    private Map<Integer, Long> handleRelatedCollection(
            List<List<? extends Principle<?>>> relatedCollection,
            @SuppressWarnings("rawtypes") BiFunction<Dao<?>, List, Long> handleFunction,
            Object mainTransaction) { // 新增：接收主线程的事务上下文
        // 入参空值兜底（原有逻辑保留）
        if (null == relatedCollection || relatedCollection.isEmpty()) {
            return new HashMap<>();
        }

        int batchSize = relatedCollection.size();
        AtomicReferenceArray<Long> resultArray = new AtomicReferenceArray<>(batchSize);
        AtomicBoolean hasException = new AtomicBoolean(false);
        List<CompletableFuture<Void>> futures = new ArrayList<>(batchSize);

        try {
            for (int batchIndex = 0; batchIndex < batchSize; batchIndex++) {
                if (hasException.get()) {
                    break;
                }
                int finalIndex = batchIndex;

                // 原有异步逻辑保留，但新增事务上下文传递
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    // ========== 关键：每个批次异步线程绑定主线程的事务上下文 ==========
                    if (mainTransaction != null) {
                        TransactionSynchronizationManager.bindResource(dataSource, mainTransaction);
                        log.info("【handleRelatedCollection-批次{}】绑定主线程事务上下文，线程：{}",
                                finalIndex, Thread.currentThread().getName());
                    }

                    try {
                        // 调用executeInTransactionContext，此时已有事务上下文
                        executeInTransactionContext(() -> {
                            if (hasException.get()) {
                                return null; // 无返回值，用null占位
                            }

                            List<? extends Principle<?>> relatedList = relatedCollection.get(finalIndex);
                            try {
                                Long updateCount = 0L;
                                if (null != relatedList && relatedList.stream().anyMatch(Objects::nonNull)) {
                                    List<? extends Principle<?>> notNullList = relatedList.stream()
                                            .filter(Objects::nonNull)
                                            .collect(Collectors.toList());
                                    updateCount = handleRelatedList(notNullList, handleFunction);
                                    if (updateCount == null) {
                                        throw new IllegalStateException("第" + finalIndex + "批次处理结果为空，判定为执行失败");
                                    }
                                }
                                resultArray.set(finalIndex, updateCount);
                            } catch (Exception e) {
                                if (hasException.compareAndSet(false, true)) {
                                    String errorMsg = String.format("第%d个批次处理失败，已终止所有后续任务", finalIndex);
                                    log.error(errorMsg, e);
                                    futures.forEach(f -> f.cancel(true));
                                    throw new CompletionException(new RuntimeException(errorMsg, e));
                                }
                            }
                            return null; // 无返回值，用null占位
                        });
                    } finally {
                        // ========== 关键：每个批次异步线程解除事务上下文绑定 ==========
                        if (mainTransaction != null && TransactionSynchronizationManager.hasResource(dataSource)) {
                            TransactionSynchronizationManager.unbindResource(dataSource);
                            log.info("【handleRelatedCollection-批次{}】解除事务上下文绑定，线程：{}",
                                    finalIndex, Thread.currentThread().getName());
                        }
                    }
                }, this.daoSaveExecutor); // 仍使用原有线程池

                futures.add(future);
            }

            // 等待所有任务完成（原有逻辑保留）
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(timeout, TimeUnit.SECONDS);

            // 组装结果（原有逻辑保留）
            Map<Integer, Long> updateCountMap = new HashMap<>(batchSize);
            for (int i = 0; i < batchSize; i++) {
                updateCountMap.put(i, Optional.ofNullable(resultArray.get(i)).orElse(0L));
            }
            return updateCountMap;

        } catch (Exception e) {
            // 异常处理（原有逻辑保留）
            Throwable rootCause = e instanceof CompletionException ? e.getCause() : e;
            if (rootCause == null) {
                rootCause = e;
            }
            throw new RuntimeException("并行批量处理失败，已终止所有任务", rootCause);
        } finally {
            // 清理逻辑（原有逻辑保留）
            if (hasException.get()) {
                futures.forEach(f -> {
                    if (!f.isDone()) {
                        f.cancel(true);
                    }
                });
            }
        }
    }
	
	
	/**
	 * 存储子列表
	 * （借助入参套出泛型）
	 * @param <R>
	 * @param relatedList
	 * @param applicationContext
	 * @return
	 */
	private <R extends Principle<?>> Long handleRelatedList(List<R> relatedList,@SuppressWarnings("rawtypes") BiFunction<Dao<?>, List, Long> handleFunction) {
		if (null==relatedList || relatedList.isEmpty()) {
			return 0L;
		}
		Dao<R> relatedDao=getDaoBeanByPrincipleClass(getClassFromList(relatedList));
		Long relateCount=handleFunction.apply(relatedDao, relatedList);
		if(null==relateCount) {
			return 0L;
		}
		return relateCount;
	}
	

	/*
	 * ==================================辅助方法区: 事务支持==============================
	 */
	// 事务上下文传递工具方法
	private <T> T executeInTransactionContext(Supplier<T> supplier) {
	    // ========== 原有逻辑：执行supplier（无返回值时返回null） ==========
	    try {
	        return supplier.get();
	    } catch (Exception e) {
	        throw new RuntimeException("executeInTransactionContext执行失败", e);
	    }
	}

	
	
	/*
	 * ==================================辅助方法区: 确定类型==============================
	 */
	
	/**
	 * 获取列表的泛型
	 * @param <P>
	 * @param list
	 * @return
	 */
	private <P extends Principle<?>> Class<P> getClassFromList(List<P> list){
		if(null==list || list.isEmpty() || null==list.get(0)) {
			return null;
		}
		Class<P> clazz=findInCache(list.get(0));
		return clazz;
	}
	

    /**
     * 查找实例的类或其父类在缓存中对应的类型
     * @param instance 要检查的实例
     * @return 缓存中找到的类型，如未找到则返回null
     */
	@SuppressWarnings("unchecked")
	private <P> Class<P> findInCache(P instance) {
		
        Class<?> clazz=findInCache(instance.getClass());
        if(null==clazz) {
        	return null;
        }
        return (Class<P>) clazz;
    }

    /**
     * 查找类或其父类在缓存中对应的类型
     * @param clazz 要检查的类
     * @return 缓存中找到的类型，如未找到则返回null
     */
    private Class<?> findInCache(Class<?> clazz) {
    	
        // 检查当前类是否在缓存中
        if (DAO_IMPL_CACHE.containsKey(clazz)) {
            return clazz;
        }
        
        // 获取父类
        Class<?> superClass = clazz.getSuperclass();
        
        // 如果父类存在且不是Object类，递归查找
        if (superClass != null && superClass != Object.class) {
            return findInCache(superClass);
        }
        
        // 找不到任何匹配的类型
        return null;
    }

    /**
     * 
     * 根据实体Class获取对应的Dao实现bean
     * @param <P>
     * @param principleClass ： Demo.class
     * @param applicationContext
     * @return Dao<Demo>: DemoDaoImpl
     */
    private <P extends Principle<?>> Dao<P> getDaoBeanByPrincipleClass(Class<P> principleClass){
    	
    	Dao<?> daoImpl = DAO_IMPL_CACHE.get(principleClass);
        if (daoImpl == null) {
            throw new IllegalArgumentException("未找到实体类[" + principleClass.getName() + "]对应的Dao实现");
        }

		@SuppressWarnings("unchecked")
		Dao<P> daoBean = (Dao<P>) daoImpl;
    	return daoBean;
    }
    

	/*
	 * ==================================辅助方法区: 初始化dao 缓存==============================
	 */
	

    /**
     * 初始化缓存变量（优先解析直接实现的Dao接口泛型）
     * @param context Spring应用上下文
     */
    private void initDaoCache(ApplicationContext context) {
        if (DAO_IMPL_CACHE.isEmpty()) { // 第一次检查（无锁）
            synchronized (DAO_IMPL_CACHE) { // 双重检查锁，确保初始化唯一性
                if (DAO_IMPL_CACHE.isEmpty()) {
                    // 1. 从Spring容器获取所有Dao实现类Bean
                    @SuppressWarnings("rawtypes")
                    Map<String, Dao> daoBeans = context.getBeansOfType(Dao.class);

                    // 2. 遍历解析并缓存
                    for (Dao<?> daoImpl : daoBeans.values()) {
                        // 工具类：解AOP代理，获取原始类
                        Class<?> daoImplClass = GenericDaoUtils.getRawDaoClass(daoImpl);
                        // 工具类：解析泛型实体类（优先接口、后父类）
                        Class<?> genericEntityClass = GenericDaoUtils.resolveGenericEntityClass(daoImplClass, Dao.class);

                        if (genericEntityClass != null) {
                            DAO_IMPL_CACHE.put(genericEntityClass, daoImpl);
                        }
                    }
                }
            }
        }
        //打印容器里的内容
    	printDaoCacheContent();
    }

	/*
	 * ==================================日志 辅助工具区==============================
	 */

    /**
     * 格式化打印DAO_IMPL_CACHE中的内容（私有辅助方法）
     */
    private void printDaoCacheContent() {
        if (DAO_IMPL_CACHE.isEmpty()) {
            log.info("【Dao缓存初始化】DAO_IMPL_CACHE 容器为空，未加载任何Dao实现类");
            return;
        }

        log.info("【Dao缓存初始化】DAO_IMPL_CACHE 容器内容如下（共{}个Dao实现类）：", DAO_IMPL_CACHE.size());
        for (Map.Entry<Class<?>, Dao<?>> entry : DAO_IMPL_CACHE.entrySet()) {
            Class<?> entityClass = entry.getKey();
            Dao<?> daoImpl = entry.getValue();
            Class<?> daoImplClass = GenericDaoUtils.getRawDaoClass(daoImpl);
            
            // 详细输出：
            log.info("  实体类：{} -> Dao实现类：{}",
                        entityClass.getName(),
                        daoImplClass.getName());
        }
    }
    
    
}
