package xyz.zhiwei.cognitivedesign.dao.impl.rpc.feign;

/**
 * 事务ID上下文工具（ThreadLocal保证线程隔离）
 */
public class TransactionIdContext {
    // 定义ThreadLocal存储事务ID（String类型）
    private static final ThreadLocal<String> TRANSACTION_ID_HOLDER = new ThreadLocal<>();

    // 设置事务ID到当前线程
    public static void setTransactionId(String transactionId) {
        TRANSACTION_ID_HOLDER.set(transactionId);
    }

    // 从当前线程获取事务ID
    public static String getTransactionId() {
        return TRANSACTION_ID_HOLDER.get();
    }

    // 清除当前线程的事务ID（防止内存泄漏）
    public static void clear() {
        TRANSACTION_ID_HOLDER.remove();
    }
}