# 框架实例

---

## 一、包结构依赖图
<img src="https://image.zhiwei.xyz/paragraph/img/660684363226295296_1_2_UE9RW.jpg" width="80%" />

框架：
- morphism.jar： 态射包。 domain的进阶版。表象与本原定义，表象与本原间三个核心关系定义。是整个认知设计最核心的定义。（其他包都可以自行实现。）
- service.jar: 服务包。只存在于框架层。里面是标准解步骤。
- dao.jar: 存取适配包。只存在于框架层。是service调用各具体dao.impl的适配桥梁。
- dao.impl.xxx.jar 存取实现包。框架提供的数据存取基本实现。（可选）

实际项目(典型)：
- morphism.jar： 态射包。表象与本原间的函数关系集。（态射：保持某种结构不变的映射）
- api.jar: web接口之外的开放接口包，供别的项目的rpc调用。
- web.jar: web接口服务包，执行标准流程以及与morphism间的适配转换。
- dao.impl.xxx.jar 数据存取实现包。

---
## 二、核心定义
morphism.jar：
- 本原定义： Principle.java 里面只有属性id和name, 非常简单。
- 表象定义： Appearance.java 这是一个接口。定义了表象由本原构成的三个逐级明确的关系：
  1. qualifiersLanes方法：**表象所关联的本原集**。
  2. construct方法+deconstruct方法：**表象由本原集构造而成，本原集由表象拆解而来**。
  3. transforms方法：**表象变化的实质是本原集的属性变化**。

这里的定义完完全全遵照《认知设计的核心要点》，只字不差。

---
## 三、功能实现流程
![实现流程图](https://image.zhiwei.xyz/paragraph/img/660684363226295296_1_7_CCOSL.jpg)

核心流程：
- Service.java 服务流程中只执行标准的解步骤，一共6个标准步骤。已在上图中用①-⑥标出。

实现细节补充：
1. 关系1所返回的本原集定义，并非是查询条件，而是能返回查询条件的函数序列。这些函数序列能解决查询依赖问题(后一次查询条件依赖于上一次查询结果)。
2. 关系2之二返回的本原集，也只是函数序列(后一次要写入的数据依赖于前一次写入的结果)。
3. 没有依赖关系的数据查询，底层默认执行并发查询； 没有写入依赖关系的结果数据，底层默认执行并发写入。（线程池支持本原(表)粒度的读写配置）
4. 写入数据支持事务组自由编排。



## 附录
- 使用本框架的实际项目: [知识经纬](https://zhiwei.xyz)   （ [项目源码](https://image.zhiwei.xyz/paragraph/other/zhiwei-source.zip)  ）