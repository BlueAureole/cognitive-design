# RPC数据存取实现模块

## 概述

本模块实现了基于HttpClient的远程调用数据存取方案，通过HTTP协议调用远程REST API来完成数据的增删改查操作。

## 功能特性

- ✅ 基于HttpClient的远程HTTP调用
- ✅ 实现Dao接口的标准数据存取操作
- ✅ 支持JSON序列化和反序列化
- ✅ 完善的异常处理机制
- ✅ 连接池管理和请求配置
- ✅ 可配置的超时和重试机制
- ✅ 统一的响应格式处理

## 核心组件

### 1. BaseRpcDaoImpl
主要的数据存取实现类，继承Dao接口，提供以下核心方法：
- `subCollection(Principle<T> principle)` - 查询子集合
- `save(Principle<T> principle)` - 保存数据（新增/更新）
- `delete(Principle<T> principle)` - 删除数据

### 2. HttpClientUtil
HTTP客户端工具类，提供：
- 统一的HTTP请求发送
- 连接池管理
- JSON响应解析
- 异常处理

### 3. RpcException
自定义异常类，用于处理RPC调用过程中的异常情况，包含：
- HTTP状态码
- 响应体信息
- 详细错误信息

### 4. MessageResponse
响应消息包装类，统一处理远程服务返回的响应格式：
```java
public class MessageResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String code;
    private Long timestamp;
}
```

## 远程API接口规范

本模块调用的远程API应遵循以下接口规范（参考AccountController）：

### 查询接口
- `POST /queryOne.do` - 单条查询
- `POST /queryList.do` - 列表查询
- `POST /queryPage.do` - 分页查询

### 新增接口
- `POST /addOne.do` - 单条新增
- `POST /addList.do` - 批量新增

### 更新接口
- `POST /updateOne.do` - 单条更新
- `POST /updateList.do` - 批量更新

### 删除接口
- `POST /delOne.do` - 单条删除
- `POST /delList.do` - 批量删除

## 配置说明

### application-rpc.properties
```properties
# 远程服务基础URL
rpc.base.url=http://localhost:8080

# HTTP连接配置
rpc.http.connect.timeout=5000
rpc.http.socket.timeout=30000
rpc.http.connection.request.timeout=3000

# 连接池配置
rpc.http.pool.max.total=200
rpc.http.pool.max.per.route=50
rpc.http.pool.idle.timeout=60

# 重试配置
rpc.retry.enabled=true
rpc.retry.max.attempts=3
rpc.retry.delay=1000
```

## 使用示例

### 基本使用
```java
// 创建RPC DAO实例
Dao<String> rpcDao = new BaseRpcDaoImpl<>("http://localhost:8080");

// 查询操作
Principle<String> queryPrinciple = new YourPrincipleImpl();
List<String> results = rpcDao.subCollection(queryPrinciple);

// 保存操作
Principle<String> savePrinciple = new YourPrincipleImpl();
boolean saveResult = rpcDao.save(savePrinciple);

// 删除操作
Principle<String> deletePrinciple = new YourPrincipleImpl();
boolean deleteResult = rpcDao.delete(deletePrinciple);
```

### 异常处理
```java
try {
    List<String> result = rpcDao.subCollection(principle);
} catch (RpcException e) {
    System.err.println("RPC调用失败: " + e.getMessage());
    System.err.println("状态码: " + e.getStatusCode());
    System.err.println("响应体: " + e.getResponseBody());
}
```

## 依赖说明

本模块依赖以下组件：
- Apache HttpClient 4.5.13
- Jackson 2.15.2（JSON处理）
- Spring Web（可选，用于某些工具类）
- cognitive-design dao模块

## 编译和构建

```bash
# 编译模块
mvn clean compile

# 运行测试
mvn test

# 打包
mvn package
```

## 注意事项

1. **网络连接**：确保网络连接正常，远程服务可访问
2. **超时配置**：根据实际网络环境调整超时时间
3. **异常处理**：务必处理RpcException和网络异常
4. **连接池**：合理配置连接池参数，避免资源泄露
5. **JSON格式**：确保远程API返回标准的JSON格式

## 扩展说明

如需扩展功能，可以：
1. 继承BaseRpcDaoImpl实现自定义逻辑
2. 修改HttpClientUtil添加新的HTTP方法支持
3. 扩展MessageResponse支持更多响应格式
4. 添加缓存、监控等增强功能

## 版本历史

- v0.0.1-SNAPSHOT：初始版本，实现基本的RPC数据存取功能