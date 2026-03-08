package xyz.zhiwei.cognitivedesign.dao.accessimpl;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * JTA 池化数据源包装器
 * 同时实现 DataSource (供 MyBatis/连接池使用) 和 XADataSource (供 JTA 事务使用)
 * 核心逻辑：
 * 1. 作为 DataSource：全权委托给底层的连接池（HikariCP/TomcatJDBC等）
 * 2. 作为 XADataSource：尝试从连接池获取的连接中解包出 XAConnection
 */
public class JtaPoolingDataSource extends DelegatingDataSource implements XADataSource {
    private static final Logger log = LoggerFactory.getLogger(JtaPoolingDataSource.class);

    public JtaPoolingDataSource(DataSource delegatePool) {
        super(delegatePool);
    }

    @Override
    public XAConnection getXAConnection() throws SQLException {
        Connection conn = getConnection();
        return wrapToXAConnection(conn);
    }

    @Override
    public XAConnection getXAConnection(String user, String password) throws SQLException {
        Connection conn = getConnection(user, password);
        return wrapToXAConnection(conn);
    }

    private XAConnection wrapToXAConnection(Connection conn) throws SQLException {
        // 尝试获取 XA 组件 (XAConnection 或 XAResource)
        Object xaComponent = findXAComponent(conn);

        if (xaComponent instanceof XAConnection) {
            return (XAConnection) xaComponent;
        } else if (xaComponent instanceof XAResource) {
            return new VirtualXAConnection(conn, (XAResource) xaComponent);
        }

        log.error("Underlying connection does not support XA unwrapping: {}", conn.getClass().getName());
        throw new SQLException("Underlying connection does not support XA unwrapping: " + conn.getClass().getName());
    }

    private Object findXAComponent(Connection conn) {
        // 1. 标准 JDBC unwrap 尝试
        Object result = tryUnwrap(conn, XAConnection.class);
        if (result != null) return result;

        result = tryUnwrap(conn, XAResource.class);
        if (result != null) return result;

        // 2. 反射尝试 (针对特定驱动/连接池的非标准实现)
        result = findXAObjectByReflection(conn);
        if (result != null) return result;

        XAResource mysqlXaResource = tryCreateMySqlXaResource(conn);
        if (mysqlXaResource != null) return mysqlXaResource;

        return null;
    }

    private <T> T tryUnwrap(Connection conn, Class<T> iface) {
        try {
            if (conn.isWrapperFor(iface)) {
                return conn.unwrap(iface);
            }
        } catch (SQLException ignored) {
            // ignore
        }
        return null;
    }

    private Object findXAObjectByReflection(Connection conn) {
        // 尝试的方法名
        String[] methodNames = {"getXAResource", "recoveryConnection", "getWrappedConnection"};
        for (String methodName : methodNames) {
            Object res = invokeMethod(conn, methodName);
            Object xa = extractXAFromObject(res);
            if (xa != null) return xa;
        }

        // 尝试的字段名
        String[] fieldNames = {"_theConnection", "_transactionalDriverXAConnectionConnection", "connection", "delegate"};
        for (String fieldName : fieldNames) {
            Object res = getFieldValue(conn, fieldName);
            Object xa = extractXAFromObject(res);
            if (xa != null) return xa;
        }
        
        return null;
    }

    private XAResource tryCreateMySqlXaResource(Connection conn) {
        Class<?> mysqlJdbcConnectionClass;
        try {
            mysqlJdbcConnectionClass = Class.forName("com.mysql.cj.jdbc.JdbcConnection");
        } catch (ClassNotFoundException e) {
            return null;
        }

        Object mysqlJdbcConnection = tryUnwrapObject(conn, mysqlJdbcConnectionClass);
        if (mysqlJdbcConnection == null) {
            Object delegate = getFieldValue(conn, "delegate");
            if (delegate instanceof Connection) {
                mysqlJdbcConnection = tryUnwrapObject((Connection) delegate, mysqlJdbcConnectionClass);
            }
        }
        if (mysqlJdbcConnection == null) return null;

        try {
            Class<?> mysqlXaConnectionClass = Class.forName("com.mysql.cj.jdbc.MysqlXAConnection");
            Object mysqlXaConn = invokeStaticMethod(mysqlXaConnectionClass, "getInstance",
                    new Class<?>[] { mysqlJdbcConnectionClass, boolean.class, boolean.class },
                    new Object[] { mysqlJdbcConnection, false, true });
            if (mysqlXaConn == null) {
                mysqlXaConn = invokeStaticMethod(mysqlXaConnectionClass, "getInstance",
                        new Class<?>[] { mysqlJdbcConnectionClass, boolean.class },
                        new Object[] { mysqlJdbcConnection, false });
            }
            if (mysqlXaConn == null) return null;

            Object xaResource = invokeMethod(mysqlXaConn, "getXAResource");
            if (xaResource instanceof XAResource) {
                return (XAResource) xaResource;
            }
            return null;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private Object extractXAFromObject(Object obj) {
        if (obj == null) return null;
        if (obj instanceof XAConnection) return obj;
        if (obj instanceof XAResource) return obj;
        
        // 递归检查：如果是 Connection，尝试 unwrap
        if (obj instanceof Connection) {
            Connection innerConn = (Connection) obj;
            try {
                if (innerConn.isWrapperFor(XAConnection.class)) return innerConn.unwrap(XAConnection.class);
                if (innerConn.isWrapperFor(XAResource.class)) return innerConn.unwrap(XAResource.class);
            } catch (SQLException ignored) {}
        }

        // 尝试调用 getXAResource 方法
        Object res = invokeMethod(obj, "getXAResource");
        if (res instanceof XAResource) return res;
        
        return null;
    }

    private Object invokeMethod(Object target, String methodName) {
        try {
            Method m = getMethod(target.getClass(), methodName);
            if (m != null) {
                m.setAccessible(true);
                return m.invoke(target);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Object invokeStaticMethod(Class<?> targetClass, String methodName, Class<?>[] parameterTypes, Object[] args) {
        try {
            Method m = getMethod(targetClass, methodName, parameterTypes);
            if (m != null) {
                m.setAccessible(true);
                return m.invoke(null, args);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Object getFieldValue(Object target, String fieldName) {
        try {
            java.lang.reflect.Field f = getField(target.getClass(), fieldName);
            if (f != null) {
                f.setAccessible(true);
                return f.get(target);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Object tryUnwrapObject(Connection conn, Class<?> iface) {
        try {
            if (conn.isWrapperFor(iface)) {
                return conn.unwrap(iface);
            }
        } catch (SQLException ignored) {}
        return null;
    }

    private java.lang.reflect.Field getField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    private Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
    
    /**
     * Virtual XAConnection that wraps an existing Connection and XAResource
     */
    private static class VirtualXAConnection implements XAConnection {
        private final Connection conn;
        private final XAResource xaResource;

        public VirtualXAConnection(Connection conn, XAResource xaResource) {
            this.conn = conn;
            this.xaResource = xaResource;
        }

        @Override
        public XAResource getXAResource() throws SQLException {
            return xaResource;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return conn;
        }

        @Override
        public void close() throws SQLException {
            conn.close();
        }

        @Override
        public void addConnectionEventListener(javax.sql.ConnectionEventListener listener) {}

        @Override
        public void removeConnectionEventListener(javax.sql.ConnectionEventListener listener) {}

        @Override
        public void addStatementEventListener(javax.sql.StatementEventListener listener) {}

        @Override
        public void removeStatementEventListener(javax.sql.StatementEventListener listener) {}
    }
}
