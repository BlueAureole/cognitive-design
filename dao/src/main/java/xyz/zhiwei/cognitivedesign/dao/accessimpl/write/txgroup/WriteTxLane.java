package xyz.zhiwei.cognitivedesign.dao.accessimpl.write.txgroup;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jakarta.transaction.Transaction;
import xyz.zhiwei.cognitivedesign.dao.Dao;
import xyz.zhiwei.cognitivedesign.dao.TransactionDao;
import xyz.zhiwei.cognitivedesign.dao.accessimpl.CustomWriteThreadPool;
import xyz.zhiwei.cognitivedesign.dao.accessimpl.DaoBeanCache;
import xyz.zhiwei.cognitivedesign.dao.accessimpl.TimeOutConfig;
import xyz.zhiwei.cognitivedesign.dao.accessimpl.write.WriteAccessLog;
import xyz.zhiwei.cognitivedesign.morphism.Principle;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.container.ImageLane;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.container.PrincipleImage;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.container.PrincipleImagery;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.response.ImageResponse;
import xyz.zhiwei.cognitivedesign.morphism.principle.image.response.ResponseLane;

/**
 * 事务泳道
 */
public class WriteTxLane {
    private static final Logger log = LoggerFactory.getLogger(WriteTxLane.class);
    private Integer TIMEOUT=TimeOutConfig.TIME_OUT_WRITE_UNIT;
    private final Long EMPTY_VALUE=0L;

    private DaoBeanCache daoBeanCache;
    private Executor daoWriteExecutor;
	
    public WriteTxLane(DaoBeanCache daoBeanCache,Executor daoWriteExecutor) {
    	this.daoBeanCache=daoBeanCache;
    	this.daoWriteExecutor=daoWriteExecutor;
    }
    

	/*
	 * ================================================= 泳道 ===============================================================
	 */

	/**
	 * 存储一个泳道
	 * 一个泳道内的数据分为若干批次
	 * @param transactionLane
	 * @param txGroupIndex 事务组序号
	 * @param laneIndex 泳道序号
	 * @param jtaTransaction 全局 JTA 事务
	 * @param syncCollector 事务回调收集器
	 * @param globalConnectionCollector 全局连接收集器
	 * @return
	 */
	public ResponseLane saveLane(ImageLane transactionLane, int txGroupIndex, int laneIndex, Transaction jtaTransaction, CrossThreadSyncCollector syncCollector, Queue<Connection> globalConnectionCollector) {


	        log.info("============= 事务组{} 第{}号泳道 写入开始 =============",txGroupIndex,laneIndex);
			
			List<ImageResponse> respList=new ArrayList<>();
			// 泳道级连接缓存：Key(DataSource/Dao/String) -> Connection
			// 必须线程安全，因为 saveBatch 内部会并行执行
			Map<Object, Connection> laneConnectionCache = new ConcurrentHashMap<>();
			
			try {
				//一个泳道内的数据分为若干批次，后一批数据依赖于前一批数据
				for (int i = 0; i < transactionLane.size(); i++) {
					Function<List<ImageResponse>,PrincipleImage> batchFun = transactionLane.get(i);
					PrincipleImage principleImage=batchFun.apply(respList);
					WriteAccessLog.image(txGroupIndex,laneIndex, i, principleImage);
					
					// 执行批次（内部可能并行）
					ImageResponse imageResponse = saveBatch(principleImage, jtaTransaction, laneConnectionCache, syncCollector);
					
					WriteAccessLog.resp(txGroupIndex,laneIndex, i, imageResponse);
					respList.add(imageResponse);
				}
			} finally {
				// 泳道结束，不再立即关闭连接，而是收集到全局队列，等待事务提交后统一关闭
				// closeLaneConnections(laneConnectionCache);
				if (globalConnectionCollector != null) {
					globalConnectionCollector.addAll(laneConnectionCache.values());
				}
			}
			
	        log.info("============= 事务组{}  第{}号泳道 写入结束 =============",txGroupIndex,laneIndex);
			
			return new ResponseLane(respList);
			
		
	}
    
	
	

	/*
	 * ================================================= 批次 ===============================================================
	 */

	
	
	/**
	 * 存储一个批次
	 * @param principleImage
	 * @param jtaTransaction
	 * @param laneConnectionCache
	 * @param syncCollector
	 * @return
	 */
	private ImageResponse saveBatch(PrincipleImage principleImage, Transaction jtaTransaction, Map<Object, Connection> laneConnectionCache, CrossThreadSyncCollector syncCollector) {
		ImageResponse resultImageResponse=new ImageResponse();
		if(isEmpty(principleImage)) {
			return new ImageResponse();
		}

		// 1. 分组：按 TransactionVisibilityKey 分组
		// Key -> List<Index>
		Map<Object, List<Integer>> unitGroups = new HashMap<>();
		for (int index = 0; index < principleImage.size(); index++) {
			final int i = index;
			PrincipleImagery<?> principleImagery = principleImage.get(i);
			if(isEmpty(principleImagery)) {
				resultImageResponse.put(i, EMPTY_VALUE);
				continue;
			}
			Dao<?> dao = daoBeanCache.getDaoBeanByPrincipleClass(daoBeanCache.getClassFromList(principleImagery));
			if (!(dao instanceof TransactionDao)) {
				throw new IllegalArgumentException("Dao must implement TransactionDao to support cross-thread transaction: " + dao.getClass().getName());
			}
			TransactionDao<?> txDao = (TransactionDao<?>) dao;
			Object key = txDao.getTransactionVisibilityKey();
			unitGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
		}

		// 2. 并行执行各组
		// 不同 Key 的组可以并行，相同 Key 的组内必须串行（或复用连接但需注意线程安全）
		// 由于 JDBC Connection 非线程安全，我们对同一 Key 的任务列表采用串行执行
		List<CompletableFuture<Void>> futures = new ArrayList<>();

		for (Map.Entry<Object, List<Integer>> entry : unitGroups.entrySet()) {
			//Object key = entry.getKey();
			List<Integer> indexes = entry.getValue();

			// 选择执行器：优先使用自定义线程池，否则使用默认线程池
			Executor executorToUse = this.daoWriteExecutor;
			if (!indexes.isEmpty()) {
				PrincipleImagery<?> principleImagery = principleImage.get(indexes.get(0));
				Dao<?> dao = daoBeanCache.getDaoBeanByPrincipleClass(daoBeanCache.getClassFromList(principleImagery));
				if (dao instanceof CustomWriteThreadPool) {
					Executor customExecutor = ((CustomWriteThreadPool) dao).getWriteExecutor();
					if (customExecutor != null) {
						executorToUse = customExecutor;
					}
				}
			}
			
			CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
				// Worker Thread 逻辑
				boolean success = false;
				try {
					// 初始化 TSM (欺骗 MyBatis)
					if (!TransactionSynchronizationManager.isSynchronizationActive()) {
						TransactionSynchronizationManager.initSynchronization();
					}
					TransactionSynchronizationManager.setActualTransactionActive(true);
					
					// 串行执行该组内的所有单元
					for (Integer index : indexes) {
						PrincipleImagery<?> principleImagery = principleImage.get(index);
						// 在执行前，尝试准备连接资源（利用 TransactionDao）
						prepareConnectionForUnit(principleImagery, laneConnectionCache, jtaTransaction);
						
						Long count = saveUnit(principleImagery);
						// 结果必须线程安全地放入 resultImageResponse
						synchronized (resultImageResponse) {
							resultImageResponse.put(index, count);
						}
					}
					success = true;
				} catch (Exception e) {
					throw new RuntimeException("Batch unit execution failed", e);
				} finally {
					// 区分本地资源清理回调（如MyBatis）和业务事务回调
					// 这样可以确保资源（如 SqlSession）在创建它的线程中正确关闭，同时保留业务回调供全局事务使用
					if (TransactionSynchronizationManager.isSynchronizationActive()) {
						List<TransactionSynchronization> allSyncs = TransactionSynchronizationManager.getSynchronizations();
						List<TransactionSynchronization> globalSyncs = new ArrayList<>();
						
						for (TransactionSynchronization sync : allSyncs) {
							// 识别 MyBatis 的同步回调 (它是线程绑定的，必须在当前线程执行)
							if (sync.getClass().getName().startsWith("org.mybatis.spring")) {
								try {
									// 模拟 Spring 事务提交流程触发回调
									if (success) {
										sync.beforeCommit(false);
									}
									sync.beforeCompletion();
									if (success) {
										sync.afterCommit();
									}
									int status = success ? TransactionSynchronization.STATUS_COMMITTED : TransactionSynchronization.STATUS_ROLLED_BACK;
									sync.afterCompletion(status);
								} catch (Exception e) {
									log.warn("Local synchronization failed for " + sync.getClass().getName(), e);
								}
							} else {
								// 其他回调（认为是业务回调），传递给主线程，随全局事务触发
								globalSyncs.add(sync);
							}
						}
						
						// 将业务回调传递给全局收集器
						if (!globalSyncs.isEmpty()) {
							syncCollector.addSyncs(globalSyncs);
						}

						// 清理本地 TSM (解绑 ConnectionHolder 等)
						TransactionSynchronizationManager.clear();
					}
				}
			}, executorToUse); // 使用线程池
			
			futures.add(future);
		}

		// 3. 等待所有组完成
		try {
			CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
			all.get(TIMEOUT.longValue(), TimeUnit.SECONDS);
		} catch (Exception e) {
			log.error("Batch execution failed", e);
			throw new RuntimeException(e);
		}

		return resultImageResponse;
	}
	

	/**
	 * 为单元准备连接资源
	 * 实际上，BaseRdbDaoImpl 内部也会尝试获取连接，但我们将逻辑上移到 Lane 中，
	 * 利用 TransactionDao 接口获取 DataSource，并在此处进行 Enlistment。
	 * 这样 BaseRdbDaoImpl 就可以保持纯粹，只需从 CrossThreadTxContext 获取即可。
	 */
	private <P extends Principle<?>> void prepareConnectionForUnit(PrincipleImagery<P> principleImagery, Map<Object, Connection> laneConnectionCache, Transaction jtaTransaction) throws Exception {
		Dao<P> dao = daoBeanCache.getDaoBeanByPrincipleClass(daoBeanCache.getClassFromList(principleImagery));
		
		TransactionDao<P> txDao = (TransactionDao<P>) dao;
		Object key = txDao.getTransactionVisibilityKey();
		// 获取同时实现 DataSource 和 XADataSource 的对象
		XADataSource xaDs = txDao.getXADataSource();
		
		// 检查是否已有缓存连接
		Connection conn = laneConnectionCache.get(key);
		if (conn == null) {
			// 核心变更：直接从 XADataSource 获取 XAConnection
			// 注意：由于我们使用的是 JtaPoolingDataSource，这里的 getXAConnection 实际上是从连接池获取连接并 unwrap
			XAConnection xaConn = xaDs.getXAConnection();
			conn = xaConn.getConnection(); // 获取底层应用连接
			
			// 手动 Enlist (直接从 XAConnection 获取资源)
			jtaTransaction.enlistResource(xaConn.getXAResource());
			
			
			// 缓存连接到 LaneCache
			laneConnectionCache.put(key, conn);
		}
		
		// 绑定到 TSM (供 MyBatis 使用)
		// 注意：TSM 绑定的 Key 必须是 MyBatis 使用的 DataSource 对象
		// 由于 xaDs 同时也实现了 DataSource (通过 JtaPoolingDataSource)，所以可以直接强转
		if (xaDs instanceof DataSource) {
			DataSource ds = (DataSource) xaDs;
			if (!TransactionSynchronizationManager.hasResource(ds)) {
				TransactionSynchronizationManager.bindResource(ds, new ConnectionHolder(conn));
			}
		}
		
	}

	

	/*
	 * ================================================= 单元 ===============================================================
	 */


	/**
	 * 存储子列表
	 * （借助入参套出泛型）
	 * @param <P>
	 * @param principleImagery
	 * @return
	 */
	private <P extends Principle<?>> Long saveUnit(PrincipleImagery<P> principleImagery) {

		Dao<P> relatedDao=daoBeanCache.getDaoBeanByPrincipleClass(daoBeanCache.getClassFromList(principleImagery));
		Long relateCount=relatedDao.save(principleImagery);
		if(null==relateCount) {
			return EMPTY_VALUE;
		}
		return relateCount;
	}
	

	/**
	 * @param list
	 * @return
	 */
	private boolean isEmpty(List<?> list) {

        if (null == list || list.isEmpty() || !list.stream().anyMatch(Objects::nonNull)) {
            return true;
        }
        return false;
	}

	
	
}
