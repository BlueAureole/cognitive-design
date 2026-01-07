package xyz.zhiwei.cognitivedesign.dao.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


/**
 * 事务组专属线程池管理器（Spring线程池版）
 * 核心：每个实例独立，为事务组创建单线程Spring线程池，兼容Spring生态
 */
public class TransactionGroupExecutorManager {
    private static final Logger log = LoggerFactory.getLogger(TransactionGroupExecutorManager.class);

    // 每个实例独立的：事务组ID → Spring单线程池（并发请求间完全隔离）
    private final Map<Integer, ThreadPoolTaskExecutor> groupExecutorMap = new ConcurrentHashMap<>();

    /**
     * 为事务组创建专属单线程Spring线程池
     * @param groupNum 事务组编号
     * @return 该组的专属Spring线程池
     */
    public ThreadPoolTaskExecutor getOrCreateGroupExecutor(int groupNum) {
        return groupExecutorMap.computeIfAbsent(groupNum, k -> {
            // ========== 核心：创建Spring单线程池 ==========
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            // 单线程配置（核心/最大线程数=1，保证事务组内串行执行）
            executor.setCorePoolSize(1);
            executor.setMaxPoolSize(1);
            executor.setQueueCapacity(20); // 小队列，避免任务堆积
            // 线程命名（实例hash+事务组，便于排查）
            executor.setThreadNamePrefix("tx-group-" + this.hashCode() + "-" + k + "-");
            // 拒绝策略：核心线程忙时等待（适配事务串行执行）
            executor.setRejectedExecutionHandler((r, executor1) -> {
                try {
                    // 等待5秒后重试，避免直接拒绝
                    executor1.getQueue().offer(r, 5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("事务组" + k + "线程池任务提交超时", e);
                }
            });
            // 优雅关闭配置
            executor.setWaitForTasksToCompleteOnShutdown(true);
            executor.setAwaitTerminationSeconds(5);
            // 初始化线程池（Spring线程池必须调用initialize）
            executor.initialize();

            log.info("【实例{}】为事务组{}创建Spring单线程池：{}",
                    this.hashCode(), k, executor.getThreadNamePrefix());
            return executor;
        });
    }

    /**
     * 关闭指定事务组的Spring线程池（优雅关闭）
     * @param groupNum 事务组编号
     */
    public void shutdownGroupExecutor(int groupNum) {
        ThreadPoolTaskExecutor executor = groupExecutorMap.remove(groupNum);
        if (executor != null) {
            try {
                // Spring线程池的优雅关闭（先关闭线程池，再等待终止）
                executor.shutdown();
                if (!executor.getThreadPoolExecutor().awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdown();
                }
                log.info("【实例{}】关闭事务组{}的Spring线程池：{}",
                        this.hashCode(), groupNum, executor.getThreadNamePrefix());
            } catch (InterruptedException e) {
                executor.shutdown();
                Thread.currentThread().interrupt();
                log.error("【实例{}】关闭事务组{}线程池时被中断", this.hashCode(), groupNum, e);
            }
        }
    }

    /**
     * 关闭所有事务组的Spring线程池（兜底）
     */
    public void shutdownAll() {
        groupExecutorMap.forEach((groupNum, executor) -> {
            try {
                executor.shutdown();
                if (!executor.getThreadPoolExecutor().awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdown();
                }
            } catch (InterruptedException e) {
                executor.shutdown();
                Thread.currentThread().interrupt();
            }
        });
        groupExecutorMap.clear();
        log.info("【实例{}】关闭所有事务组Spring线程池，共清理{}个线程池",
                this.hashCode(), groupExecutorMap.size());
    }

    /**
     * 辅助方法：获取事务组对应的线程池（用于提交任务）
     * @param groupNum 事务组编号
     * @return 线程池（null则表示未创建）
     */
    public ThreadPoolTaskExecutor getGroupExecutor(int groupNum) {
        return groupExecutorMap.get(groupNum);
    }

    /**
     * 辅助方法：获取当前实例的线程池数量（用于监控/调试）
     */
    public int getExecutorMapSize() {
        return groupExecutorMap.size();
    }
}