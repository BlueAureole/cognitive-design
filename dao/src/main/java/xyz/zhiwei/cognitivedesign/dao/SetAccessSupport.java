package xyz.zhiwei.cognitivedesign.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.TransactionStatus;
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
    //@Autowired
    //private DataSource dataSource;
    
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
     * 存储相关数据集（核心：根据事务组找到专属线程池执行）
     */
    public PrincipleImageResponse saveRelatedSegments(
            PrincipleImage relatedSegments,
            Map<Integer, Integer> transactionGroupMap,
            Map<Integer, TransactionStatus> transactionMap,
            Map<Integer, ThreadPoolTaskExecutor> groupExecutorMap) {

        if (null == relatedSegments) {
            return new PrincipleImageResponse();
        }

        // 1. 定义异步任务：按事务组找专属线程执行
        CompletableFuture<Map<Integer, Long>> addFuture = CompletableFuture.supplyAsync(() ->
                handleRelatedCollection(relatedSegments.getAddition(), Dao::add, transactionGroupMap, transactionMap, groupExecutorMap),
                this.daoSaveExecutor);

        CompletableFuture<Map<Integer, Long>> modifyFuture = CompletableFuture.supplyAsync(() ->
                handleRelatedCollection(relatedSegments.getModification(), Dao::update, transactionGroupMap, transactionMap, groupExecutorMap),
                this.daoSaveExecutor);

        CompletableFuture<Map<Integer, Long>> delFuture = CompletableFuture.supplyAsync(() ->
                handleRelatedCollection(relatedSegments.getDeletion(), Dao::delete, transactionGroupMap, transactionMap, groupExecutorMap),
                this.daoSaveExecutor);

        // 2. 等待所有任务完成
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(addFuture, modifyFuture, delFuture);
        try {
            allFutures.get(timeout, TimeUnit.SECONDS);
            return new PrincipleImageResponse(addFuture.join(), modifyFuture.join(), delFuture.join());
        } catch (Exception e) {
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
     * 核心处理：数据项并行处理（有事务的在专属线程池执行，无事务的直接并行）
     */
    private Map<Integer, Long> handleRelatedCollection(
            List<List<? extends Principle<?>>> relatedCollection,
            @SuppressWarnings("rawtypes") BiFunction<Dao<?>, List, Long> handleFunction,
            Map<Integer, Integer> transactionGroupMap,
            Map<Integer, TransactionStatus> transactionMap,
            Map<Integer, ThreadPoolTaskExecutor> groupExecutorMap) {

        // 最终结果映射（保持原索引顺序）
        Map<Integer, Long> resultMap = new ConcurrentHashMap<>();
        // 用于存储所有异步任务，等待全部完成
        List<CompletableFuture<Void>> allFutures = new ArrayList<>();

        // ========== 第一步：预处理 - 分组（事务组/无事务） ==========
        // 1. 按事务组分组：key=事务组编号（null代表无事务），value=子列表+索引的集合
        Map<Integer, List<IndexedSubList>> groupToSubLists = new HashMap<>();
        for (int i = 0; i < relatedCollection.size(); i++) {
            List<? extends Principle<?>> subList = relatedCollection.get(i);
            if (null == subList || subList.isEmpty()) {
                resultMap.put(i, 0L); // 空列表直接标记0，无需处理
                continue;
            }
            // 获取子列表所属事务组
            Integer groupNum = transactionGroupMap.get(System.identityHashCode(subList));
            // 分组存储（key=null代表无事务）
            groupToSubLists.computeIfAbsent(groupNum, k -> new ArrayList<>())
                    .add(new IndexedSubList(i, subList));
        }

        // ========== 第二步：并行处理各组数据 ==========
        for (Map.Entry<Integer, List<IndexedSubList>> entry : groupToSubLists.entrySet()) {
            Integer groupNum = entry.getKey();
            List<IndexedSubList> indexedSubLists = entry.getValue();

            if (groupNum == null) {
                // ---------- 无事务组：直接并行处理 ----------
                for (IndexedSubList indexedSubList : indexedSubLists) {
                    int index = indexedSubList.getIndex();
                    List<? extends Principle<?>> subList = indexedSubList.getSubList();

                    // 无事务的子列表，用公共线程池并行执行
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            long count = handleRelatedList(subList, handleFunction);
                            resultMap.put(index, count);
                            log.debug("无事务子列表[{}]执行完成，数据量：{}", index, subList.size());
                        } catch (Exception e) {
                            log.error("无事务子列表[{}]执行失败", index, e);
                            resultMap.put(index, -1L);
                        }
                    }, this.daoSaveExecutor); // 公共线程池

                    allFutures.add(future);
                }
            } else {
                // ---------- 有事务组：在专属线程池串行执行（保证事务原子性） ----------
            	ThreadPoolTaskExecutor groupExecutor = groupExecutorMap.get(groupNum);
                TransactionStatus txStatus = transactionMap.get(groupNum);
                if (groupExecutor == null || txStatus == null) {
                    // 事务组配置异常，降级为无事务并行处理
                    for (IndexedSubList indexedSubList : indexedSubLists) {
                        int index = indexedSubList.getIndex();
                        List<? extends Principle<?>> subList = indexedSubList.getSubList();

                        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                            try {
                                long count = handleRelatedList(subList, handleFunction);
                                resultMap.put(index, count);
                            } catch (Exception e) {
                                log.error("事务组{}配置异常，子列表[{}]执行失败", groupNum, index, e);
                                resultMap.put(index, -1L);
                            }
                        }, this.daoSaveExecutor);

                        allFutures.add(future);
                    }
                    continue;
                }

                // 事务组内串行执行（避免同一事务并发问题），外层仍并行
                CompletableFuture<Void> groupFuture = CompletableFuture.runAsync(() -> {
                    // 在专属线程池中串行处理该事务组的所有子列表
                    for (IndexedSubList indexedSubList : indexedSubLists) {
                        int index = indexedSubList.getIndex();
                        List<? extends Principle<?>> subList = indexedSubList.getSubList();

                        CountDownLatch executeLatch = new CountDownLatch(1);
                        AtomicReference<Long> countRef = new AtomicReference<>(0L);
                        AtomicReference<Exception> exceptionRef = new AtomicReference<>();

                        // 提交到事务组专属线程（保证事务上下文绑定）
                        groupExecutor.submit(() -> {
                            try {
                                // 验证事务上下文是否绑定（可选，用于日志）
                                boolean hasTx = TransactionSynchronizationManager.isActualTransactionActive();
                                log.debug("事务组{}子列表[{}]在专属线程{}执行，事务绑定：{}",
                                        groupNum, index, Thread.currentThread().threadId(), hasTx);
                                // 执行存储操作（自动归属到该线程的事务上下文）
                                countRef.set(handleRelatedList(subList, handleFunction));
                            } catch (Exception e) {
                                exceptionRef.set(e);
                            } finally {
                                executeLatch.countDown();
                            }
                        });

                        // 等待当前子列表执行完成（串行关键）
                        try {
                            executeLatch.await(10, TimeUnit.SECONDS);
                            if (exceptionRef.get() != null) {
                                throw exceptionRef.get();
                            }
                            resultMap.put(index, countRef.get());
                        } catch (Exception e) {
                            log.error("事务组{}子列表[{}]执行失败", groupNum, index, e);
                            resultMap.put(index, -1L);
                            // 事务组内一个子列表失败，中断后续执行（保证原子性）
                            throw new RuntimeException("事务组" + groupNum + "执行失败，中断处理", e);
                        }
                    }
                }, this.daoSaveExecutor); // 外层并行控制

                allFutures.add(groupFuture);
            }
        }

        // ========== 第三步：等待所有任务完成 ==========
        try {
            // 等待所有异步任务完成，设置超时时间
            CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0]))
                    .get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("并行处理relatedCollection失败", e);
            // 取消所有未完成任务
            allFutures.forEach(future -> future.cancel(true));
            // 标记所有未处理的索引为失败
            for (int i = 0; i < relatedCollection.size(); i++) {
                resultMap.putIfAbsent(i, -1L);
            }
        }

        // ========== 第四步：补全空索引（保证结果顺序和原列表一致） ==========
        for (int i = 0; i < relatedCollection.size(); i++) {
            resultMap.putIfAbsent(i, 0L);
        }

        // 按索引排序返回（保证和输入列表顺序一致）
        return resultMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldVal, newVal) -> oldVal,
                        LinkedHashMap::new
                ));
    }
		
	/**
	 * 存储子列表
	 * （借助入参套出泛型）
	 * @param <R>
	 * @param relatedList
	 * @param applicationContext
	 * @return
	 */
	private <R extends Principle<?>> Long handleRelatedList(List<R> relatedList,
			@SuppressWarnings("rawtypes") BiFunction<Dao<?>, List, Long> handleFunction) {
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
	 * ================================== 事务辅助区 ==============================
	 */
	// ========== 辅助类：存储子列表+原索引（保证结果顺序） ==========
	private static class IndexedSubList {
	    private final int index;
	    private final List<? extends Principle<?>> subList;

	    public IndexedSubList(int index, List<? extends Principle<?>> subList) {
	        this.index = index;
	        this.subList = subList;
	    }

	    public int getIndex() {
	        return index;
	    }

	    public List<? extends Principle<?>> getSubList() {
	        return subList;
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
