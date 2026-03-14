# cognitive-design（认知设计框架包）

## 认知程序设计

认知设计是一个软件设计新思路，设计目的是降解应用系统的复杂度。实现结果超越了设计预期：通用业务域竟也能和声明式一样：只需描述结果，实现过程完全交给框架。下面列出了框架实例的重要指标表现，详情可见<a href="docs/performance-characteristics.md" target="_blank" rel="noopener noreferrer">《表现特性》</a>。

复杂度表现

<img src="https://image.zhiwei.xyz/paragraph/img/660683057505905664_1_0_P553Q.jpg" width="80%" />

代码可读性

<img src="https://image.zhiwei.xyz/paragraph/img/660683057505905664_1_8_ZMSA2.jpg" width="80%" />

性能表现

<img src="https://image.zhiwei.xyz/paragraph/img/660683057505905664_1_9_93T02.jpg" width="80%" />


## 节点导读

### <a href="docs/design-origin.md" target="_blank" rel="noopener noreferrer">设计起点</a>
新思路设计的起点是语言在认知中的作用，以及哲学中对世界认知的观点：“表象由本原构成”。据此引出认知设计的核心要点：表象由本原构成的三个逐级明确的关系。后续的框架设计完全由这三个关系推导确定。

### <a href="docs/design-details.md" target="_blank" rel="noopener noreferrer">设计详解</a>
<设计起点>其实已经完成了所有的设计确定性。然而它过于简单粗暴，显得有些空洞。《设计详解》节点描述了与现有设计最核心的差异点：没有过程的概念。

实际上现有设计一直都在努力的把过程剔除出去，比如函数式编程、DDD设计，但均未彻底（未能像声明式编程那样只需描述结果，而把过程实现完全交给框架）。现有设计其实无法破局：编排的对象是功能，而功能自身就蕴含着过程。破局需要一个新概念：此概念既能指引编排，又不蕴含过程。而认知设计恰好提供了这一概念。

**程序设计一直努力驱逐的过程顺序，它从一开始就没在认知设计的世界里**。

### <a href="docs/framework-example.md" target="_blank" rel="noopener noreferrer">框架实例</a>
包模块依赖图、核心类定义、以及功能实现流程图。

### <a href="docs/code-organization.md" target="_blank" rel="noopener noreferrer">代码组织</a>
当前的代码组织以功能为中心，直观表现为功能流程图。认知设计以构成关系为中心，直观表现为表象构成图。实质上是把流程图分离为标准流程图和表象构成图。构成图是比流程图更纯粹的业务逻辑描述。

### <a href="docs/performance-characteristics.md" target="_blank" rel="noopener noreferrer">表现特性</a>
复杂度，代码可读性，性能。