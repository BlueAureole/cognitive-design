package xyz.zhiwei.cognitivedesign.dao.accessimpl.write.txgroup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 跨线程事务回调收集器
 */
public class CrossThreadSyncCollector {
    // 线程安全的回调列表
    private final List<TransactionSynchronization> syncs = new ArrayList<>();
    private final Lock lock = new ReentrantLock();

    /**
     * 添加子线程的事务回调
     */
    public void addSyncs(List<TransactionSynchronization> newSyncs) {
        lock.lock();
        try {
            if (newSyncs != null && !newSyncs.isEmpty()) {
                syncs.addAll(newSyncs);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 将收集的回调注册到当前线程（主线程）的TransactionSynchronizationManager
     */
    public void registerToCurrentThread() {
        lock.lock();
        try {
            for (TransactionSynchronization sync : syncs) {
                // 注册到主线程的回调容器
                TransactionSynchronizationManager.registerSynchronization(sync);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 清空收集的回调（避免内存泄漏）
     */
    public void clear() {
        lock.lock();
        try {
            syncs.clear();
        } finally {
            lock.unlock();
        }
    }
}
