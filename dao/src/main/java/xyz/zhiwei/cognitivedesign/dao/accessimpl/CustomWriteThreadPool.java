package xyz.zhiwei.cognitivedesign.dao.accessimpl;

import java.util.concurrent.Executor;

/**
 * 自定义线程池接口
 * 用于提供写的线程池配置
 */
public interface CustomWriteThreadPool {

    /**
     * 获取写操作线程池
     * @return Executor
     */
    Executor getWriteExecutor();
}
