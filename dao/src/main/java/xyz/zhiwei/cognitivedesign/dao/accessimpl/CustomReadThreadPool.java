package xyz.zhiwei.cognitivedesign.dao.accessimpl;

import java.util.concurrent.Executor;

/**
 * 自定义线程池接口
 * 用于提供读线程池配置
 */
public interface CustomReadThreadPool {

    /**
     * 获取读操作线程池
     * @return Executor
     */
    Executor getReadExecutor();

}
