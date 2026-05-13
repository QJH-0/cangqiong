# 后端安全与高并发能力改造设计

**目标**

在现有 `sky-take-out` 后端基础上完成四项增量改造：基于 JWT 的无状态认证增强、基于 AOP 的操作权限校验、基于 RabbitMQ 死信队列的订单超时自动取消、基于 Lua 脚本的优惠券秒杀库存原子扣减，以及基于 Logback 的全链路业务日志。

**现状**

- 项目已具备 JWT 登录签发与请求拦截，管理员和用户各有一套拦截器。
- 项目已接入 Redis，但尚未引入 RabbitMQ。
- 项目已有订单超时定时任务 `OrderTask`，当前通过数据库轮询处理待支付超时订单。
- 项目尚无权限模型、角色模型、优惠券秒杀模型和相关数据表。
- 项目已有 `Slf4j` 日志，但没有按链路、业务、异常维度做统一输出与归档。

**设计范围**

- 保留现有 MVC、MyBatis、Service、Mapper 的项目结构。
- 不引入 Spring Security，避免对当前教学型项目造成大范围侵入。
- 新增优惠券秒杀子域，不复用菜品/套餐库存。
- RabbitMQ 作为订单超时主机制，定时任务保留为兜底补偿。

## 1. JWT 无状态认证增强

### 1.1 目标

- 继续使用 JWT 承载登录态，服务端不保存会话。
- 区分管理员和普通用户身份。
- 为后续 AOP 权限校验提供最小必要身份信息。

### 1.2 方案

- 保留现有 `JwtTokenAdminInterceptor` 与 `JwtTokenUserInterceptor`。
- 统一 JWT 负载字段，至少包含：
  - `id`：当前登录主体 ID
  - `identity`：`ADMIN` 或 `USER`
- 现有管理员登录、用户登录在签发 token 时补齐 `identity` 声明。
- 拦截器在解析 token 后，将 `id` 写入 `BaseContext`，将 `identity` 写入新的线程上下文或请求属性，供权限切面读取。
- 不把完整权限集合直接放入 token，避免 token 体积膨胀与权限变更失效问题。

### 1.3 边界

- JWT 仍负责“是否登录”。
- 权限切面负责“是否有权操作资源”。

## 2. 基于 AOP 的操作权限校验

### 2.1 目标

- 以注解方式保护后台管理端关键接口。
- 支持按权限码校验，而不是仅依赖“是否是管理员”。
- 避免把权限判断散落在 Controller 和 Service 中。

### 2.2 方案

- 新增注解 `@PermissionCheck("dish:update")`。
- 新增切面 `PermissionAspect`：
  - 获取当前登录主体身份与 ID
  - 如果不是管理员，直接拒绝
  - 从权限服务获取该管理员拥有的权限码集合
  - 校验注解要求的权限是否存在
  - 校验失败抛出统一业务异常
- 新增权限相关模型：
  - `permission`
  - `role`
  - `role_permission`
  - `employee_role`
- 新增权限服务，优先从数据库读取，可预留 Redis 缓存扩展点。

### 2.3 初始保护范围

优先保护以下管理操作：

- 员工管理：新增、修改、启用/禁用、密码修改
- 菜品与套餐：新增、修改、删除、起售/停售
- 分类管理：新增、修改、删除
- 订单管理：接单、拒单、取消、派送、完成
- 优惠券管理：新增活动、上下线、调整库存

### 2.4 异常输出

- 无 token 或 token 非法：仍由拦截器返回 `401`
- 已登录但无权限：由切面抛出业务异常，返回明确的权限不足信息

## 3. RabbitMQ 死信队列处理订单超时取消

### 3.1 目标

- 替换数据库轮询为主的超时关单机制。
- 订单创建后自动进入 15 分钟待支付观察期。
- 超时未支付时自动取消订单。

### 3.2 方案

- 新增 RabbitMQ 配置：
  - 订单延迟交换机
  - 订单延迟队列
  - 订单死信交换机
  - 订单死信队列
- 创建待支付订单后发送一条消息，消息体至少包含：
  - `orderId`
  - `orderNumber`
  - `createdAt`
- 延迟队列设置 TTL 为 15 分钟，过期后路由到死信队列。
- 死信消费者收到消息后：
  - 查询订单状态
  - 如果订单仍为 `PENDING_PAYMENT`，更新为 `CANCELLED`
  - 写入取消原因“订单超时，自动取消”
  - 记录取消时间
- 用户完成支付后无需显式删除消息，消费者通过状态幂等判断跳过已支付订单。

### 3.3 兜底机制

- 保留 `OrderTask.processTimeoutOrder()`，但角色改为补偿任务。
- 补偿任务执行频率可降低，用于处理消息异常、消费者不可用等情况。

### 3.4 幂等要求

- 消费者必须按订单当前状态决定是否更新，避免重复消费导致脏写。
- 状态从 `PENDING_PAYMENT` 到 `CANCELLED` 的更新需具备条件约束。

## 4. 优惠券秒杀与 Lua 原子扣减

### 4.1 目标

- 新增优惠券秒杀活动能力。
- 在高并发场景下防止超卖。
- 控制每个用户限领次数，避免一人重复抢券。

### 4.2 数据模型

建议新增以下表：

- `coupon`
  - `id`
  - `name`
  - `description`
  - `stock`
  - `status`
  - `start_time`
  - `end_time`
  - `limit_per_user`
  - `create_time`
  - `update_time`
- `coupon_receive_record`
  - `id`
  - `coupon_id`
  - `user_id`
  - `receive_time`
  - `source`
- `coupon_seckill_order`
  - `id`
  - `coupon_id`
  - `user_id`
  - `status`
  - `create_time`

其中：

- `coupon_receive_record` 用于记录用户实际领券结果
- `coupon_seckill_order` 用于保留秒杀流水、幂等校验与审计

### 4.3 Redis 设计

- 库存 Key：`coupon:stock:{couponId}`
- 用户抢购集合 Key：`coupon:users:{couponId}`
- 活动基础信息可按需缓存：`coupon:meta:{couponId}`

### 4.4 Lua 脚本职责

单次脚本执行完成以下操作：

1. 判断库存是否存在且大于 0
2. 判断当前用户是否已抢过或是否超出限领次数
3. 扣减库存
4. 写入用户抢购标记
5. 返回明确状态码

返回值约定：

- `1`：成功
- `0`：库存不足
- `-1`：重复抢购或超限

### 4.5 业务流程

- 管理端创建优惠券活动时：
  - 入库
  - 将库存与活动基本信息预热到 Redis
- 用户抢券时：
  - 校验活动时间窗口
  - 执行 Lua 脚本
  - Lua 成功后写入 `coupon_receive_record` 和 `coupon_seckill_order`
  - 数据库写入前后需做唯一性兜底，防止重复请求或并发重试

### 4.6 一致性策略

- Redis 作为高并发抢券入口的瞬时真相。
- 数据库作为最终记录与审计真相。
- 当前阶段采用“Lua 成功后同步写库”的实现，先保证结构清晰与可验证。
- 后续如需进一步削峰，可扩展为“Lua 成功后投递 MQ 异步落库”。

## 5. Logback 全链路业务日志

### 5.1 目标

- 输出可追踪、可筛查、可审计的业务日志。
- 将访问日志、业务日志、异常日志分流。

### 5.2 方案

- 新增 `logback-spring.xml`
- 新增请求链路过滤器或拦截器，为每个请求写入 `traceId`
- 使用 `MDC` 记录：
  - `traceId`
  - `userId`
  - `identity`
  - `requestUri`
- 日志文件拆分：
  - `logs/app-info.log`
  - `logs/app-error.log`
  - `logs/business.log`

### 5.3 业务日志覆盖点

- 管理员登录
- 用户登录
- 创建订单
- 支付订单
- 取消订单
- 创建优惠券活动
- 启停优惠券活动
- 用户抢券成功/失败
- RabbitMQ 自动取消订单
- 权限校验拒绝

### 5.4 输出要求

- 关键日志必须带业务主键，如 `orderId`、`couponId`、`userId`
- 异常日志记录异常类型、请求路径、请求参数摘要、traceId
- 不输出敏感字段原文，如完整 token、密码

## 6. 代码组织

### 6.1 修改现有文件

- `sky-server/src/main/resources/application.yml`
- `sky-server/src/main/resources/application-dev.yml`
- `sky-server/pom.xml`
- `sky-server/src/main/java/com/sky/config/WebMvcConfiguration.java`
- `sky-server/src/main/java/com/sky/service/impl/OrderServiceImpl.java`
- `sky-server/src/main/java/com/sky/task/OrderTask.java`
- `sky-server/src/main/java/com/sky/interceptor/JwtTokenAdminInterceptor.java`
- `sky-server/src/main/java/com/sky/interceptor/JwtTokenUserInterceptor.java`
- 登录相关 service 或 controller 中的 token 签发逻辑

### 6.2 新增模块文件

- 权限注解、切面、权限服务、权限 mapper、权限实体与 XML
- RabbitMQ 配置、生产者、消费者、消息 DTO
- 优惠券实体、DTO、VO、service、mapper、controller、XML
- Redis Lua 脚本与执行封装
- Logback 配置、链路日志过滤器、业务日志工具类

## 7. 测试策略

- 以单元测试覆盖 Lua 脚本执行结果映射、权限切面核心分支、消息消费者幂等逻辑。
- 以集成测试覆盖：
  - 管理员登录后访问受保护接口
  - 无权限管理员访问受保护接口
  - 创建待支付订单后消息触发自动取消
  - 秒杀优惠券库存扣减与重复抢购限制
- 若本地缺少 RabbitMQ、Redis、MySQL 测试环境，则至少保证编译通过，并给出手工验证步骤。

## 8. 非目标

- 本次不引入 Spring Security
- 本次不改造前端页面
- 本次不把所有业务都重构成事件驱动
- 本次不实现复杂 RBAC 管理后台界面，仅提供后端能力与基础接口

## 9. 风险与约束

- 仓库当前已有未提交修改：`sky-server/src/main/java/com/sky/websocket/WebSocketServer.java`，实现时不得覆盖。
- 当前项目未包含现成建表脚本，需补充 SQL 或初始化文档。
- RabbitMQ、Redis 运行环境若未就绪，将限制集成验证深度。

## 10. 验收标准

- 已登录管理员与普通用户均使用 JWT 无状态访问接口。
- 管理端关键接口支持基于注解的权限校验。
- 新订单在 15 分钟未支付时，可由 RabbitMQ 死信消费者自动取消。
- 新增优惠券秒杀接口，Lua 脚本可保证库存原子扣减并阻止重复抢券。
- 系统产出结构化业务日志，能按 `traceId`、`orderId`、`couponId`、`userId` 追踪核心业务过程。
