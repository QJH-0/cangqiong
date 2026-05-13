# 后端安全与高并发能力改造 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有 `sky-take-out` 后端中落地 JWT 无状态认证增强、AOP 权限校验、RabbitMQ 死信队列超时关单、Lua 秒杀优惠券库存扣减与 Logback 全链路业务日志。

**Architecture:** 保留现有 Spring Boot + MyBatis 分层结构，沿用 JWT 拦截器作为登录态入口，在此之上新增 AOP 权限切面。订单超时以 RabbitMQ 死信消费为主、定时任务补偿为辅；优惠券秒杀以 Redis Lua 原子扣减为入口，数据库负责最终持久化与审计。

**Tech Stack:** Spring Boot 2.7.3, Spring MVC, Spring AOP, MyBatis, Redis, RabbitMQ, Logback, JUnit

---

## File Map

- Modify: `sky-server/pom.xml`
- Modify: `sky-server/src/main/resources/application.yml`
- Modify: `sky-server/src/main/resources/application-dev.yml`
- Create: `sky-server/src/main/resources/logback-spring.xml`
- Create: `sky-server/src/main/resources/lua/coupon_seckill.lua`
- Modify: `sky-server/src/main/java/com/sky/interceptor/JwtTokenAdminInterceptor.java`
- Modify: `sky-server/src/main/java/com/sky/interceptor/JwtTokenUserInterceptor.java`
- Modify: `sky-server/src/main/java/com/sky/config/WebMvcConfiguration.java`
- Modify: 登录签发 token 的 service 实现文件
- Modify: `sky-server/src/main/java/com/sky/service/impl/OrderServiceImpl.java`
- Modify: `sky-server/src/main/java/com/sky/task/OrderTask.java`
- Create: 权限相关 `annotation/aspect/service/mapper/entity/xml`
- Create: RabbitMQ 相关 `config/mq/listener/message`
- Create: 优惠券秒杀相关 `controller/service/mapper/entity/dto/vo/xml`
- Create: 链路日志相关 `filter` 或 `interceptor`
- Create: SQL 文档 `docs\superpowers\specs\` 邻近或 `sky-server\src\main\resources`
- Create: 对应测试类 `sky-server\src\test\java\...`

### Task 1: 补齐依赖与配置骨架

**Files:**
- Modify: `sky-server/pom.xml`
- Modify: `sky-server/src/main/resources/application.yml`
- Modify: `sky-server/src/main/resources/application-dev.yml`
- Create: `sky-server/src/main/resources/logback-spring.xml`

- [ ] **Step 1: Write the failing test**

新增一个最小化 Spring 上下文测试，断言 RabbitMQ、Redis Lua 与 Logback 配置资源存在。

```java
@SpringBootTest
class InfrastructureResourceTest {

    @Test
    void shouldLoadInfrastructureResources() {
        assertNotNull(getClass().getClassLoader().getResource("logback-spring.xml"));
        assertNotNull(getClass().getClassLoader().getResource("lua/coupon_seckill.lua"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl sky-server -Dtest=InfrastructureResourceTest test`
Expected: FAIL with missing `logback-spring.xml` or `lua/coupon_seckill.lua`

- [ ] **Step 3: Write minimal implementation**

- 在 `sky-server/pom.xml` 新增 `spring-boot-starter-amqp`
- 在 `application.yml` 与 `application-dev.yml` 增加 `spring.rabbitmq`、日志目录、自定义优惠券配置
- 新建 `logback-spring.xml`
- 预留 `lua/coupon_seckill.lua`

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl sky-server -Dtest=InfrastructureResourceTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git -C sky-take-out add sky-server/pom.xml sky-server/src/main/resources/application.yml sky-server/src/main/resources/application-dev.yml sky-server/src/main/resources/logback-spring.xml sky-server/src/main/resources/lua/coupon_seckill.lua sky-server/src/test/java
git -C sky-take-out commit -m "feat: add infrastructure config for auth mq coupon logging"
```

### Task 2: 增强 JWT 上下文承载能力

**Files:**
- Modify: `sky-server/src/main/java/com/sky/interceptor/JwtTokenAdminInterceptor.java`
- Modify: `sky-server/src/main/java/com/sky/interceptor/JwtTokenUserInterceptor.java`
- Modify: 登录签发 token 的 service 实现文件
- Create: `sky-common/src/main/java/com/sky/constant/IdentityConstant.java`
- Create: `sky-common/src/main/java/com/sky/context/AuthContext.java`
- Test: `sky-server/src/test/java/com/sky/interceptor/JwtInterceptorTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void adminInterceptorShouldStoreCurrentIdentity() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("token", validAdminToken());
    MockHttpServletResponse response = new MockHttpServletResponse();

    boolean allowed = jwtTokenAdminInterceptor.preHandle(request, response, handlerMethod);

    assertTrue(allowed);
    assertEquals("ADMIN", AuthContext.getCurrentIdentity());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl sky-server -Dtest=JwtInterceptorTest#adminInterceptorShouldStoreCurrentIdentity test`
Expected: FAIL because `AuthContext` or identity storage does not exist

- [ ] **Step 3: Write minimal implementation**

- 新增 `AuthContext` 线程上下文
- 新增身份常量
- 管理员与用户拦截器在通过校验后写入身份
- 登录签发 token 时补充 `identity` claim

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl sky-server -Dtest=JwtInterceptorTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git -C sky-take-out add sky-common/src/main/java/com/sky/constant/IdentityConstant.java sky-common/src/main/java/com/sky/context/AuthContext.java sky-server/src/main/java/com/sky/interceptor/JwtTokenAdminInterceptor.java sky-server/src/main/java/com/sky/interceptor/JwtTokenUserInterceptor.java sky-server/src/test/java/com/sky/interceptor/JwtInterceptorTest.java
git -C sky-take-out commit -m "feat: enrich jwt identity context"
```

### Task 3: 新增 AOP 权限校验能力

**Files:**
- Create: `sky-server/src/main/java/com/sky/annotation/PermissionCheck.java`
- Create: `sky-server/src/main/java/com/sky/aspect/PermissionAspect.java`
- Create: `sky-server/src/main/java/com/sky/service/PermissionService.java`
- Create: `sky-server/src/main/java/com/sky/service/impl/PermissionServiceImpl.java`
- Create: `sky-server/src/main/java/com/sky/mapper/PermissionMapper.java`
- Create: `sky-server/src/main/resources/mapper/PermissionMapper.xml`
- Create: `sky-pojo/src/main/java/com/sky/entity/Permission.java`
- Create: `sky-pojo/src/main/java/com/sky/entity/Role.java`
- Create: `sky-pojo/src/main/java/com/sky/entity/EmployeeRole.java`
- Test: `sky-server/src/test/java/com/sky/aspect/PermissionAspectTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void shouldRejectWhenAdminLacksPermission() {
    AuthContext.setCurrentIdentity("ADMIN");
    BaseContext.setCurrentId(1L);
    when(permissionService.listPermissionCodes(1L)).thenReturn(Set.of("dish:view"));

    assertThrows(BaseException.class, () -> permissionAspect.check(joinPoint, permissionCheck));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl sky-server -Dtest=PermissionAspectTest test`
Expected: FAIL because annotation/aspect/service are missing

- [ ] **Step 3: Write minimal implementation**

- 新建权限注解
- 新建切面并实现管理员身份与权限码校验
- 新建权限查询服务与 Mapper
- 为关键管理端接口添加 `@PermissionCheck`

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl sky-server -Dtest=PermissionAspectTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git -C sky-take-out add sky-server/src/main/java/com/sky/annotation/PermissionCheck.java sky-server/src/main/java/com/sky/aspect/PermissionAspect.java sky-server/src/main/java/com/sky/service/PermissionService.java sky-server/src/main/java/com/sky/service/impl/PermissionServiceImpl.java sky-server/src/main/java/com/sky/mapper/PermissionMapper.java sky-server/src/main/resources/mapper/PermissionMapper.xml sky-pojo/src/main/java/com/sky/entity/Permission.java sky-pojo/src/main/java/com/sky/entity/Role.java sky-pojo/src/main/java/com/sky/entity/EmployeeRole.java sky-server/src/test/java/com/sky/aspect/PermissionAspectTest.java
git -C sky-take-out commit -m "feat: add aop permission checks"
```

### Task 4: 建立 RabbitMQ 订单超时消息链路

**Files:**
- Create: `sky-server/src/main/java/com/sky/config/RabbitMqConfiguration.java`
- Create: `sky-server/src/main/java/com/sky/message/OrderTimeoutMessage.java`
- Create: `sky-server/src/main/java/com/sky/service/OrderTimeoutPublisher.java`
- Create: `sky-server/src/main/java/com/sky/service/impl/OrderTimeoutPublisherImpl.java`
- Create: `sky-server/src/main/java/com/sky/listener/OrderTimeoutListener.java`
- Modify: `sky-server/src/main/java/com/sky/service/impl/OrderServiceImpl.java`
- Test: `sky-server/src/test/java/com/sky/listener/OrderTimeoutListenerTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void shouldCancelPendingPaymentOrderWhenTimeoutMessageConsumed() {
    Orders order = Orders.builder().id(10L).status(Orders.PENDING_PAYMENT).build();
    when(orderMapper.getById(10L)).thenReturn(order);

    listener.handle(new OrderTimeoutMessage(10L, "NO123", LocalDateTime.now()));

    verify(orderMapper).update(argThat(updated -> updated.getStatus().equals(Orders.CANCELLED)));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl sky-server -Dtest=OrderTimeoutListenerTest test`
Expected: FAIL because listener and message classes are missing

- [ ] **Step 3: Write minimal implementation**

- 新建 RabbitMQ 交换机、队列、绑定配置
- 新建延迟消息 DTO
- 在 `submitOrder` 成功创建订单后发送超时消息
- 新建死信消费者，根据订单状态幂等取消待支付订单

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl sky-server -Dtest=OrderTimeoutListenerTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git -C sky-take-out add sky-server/src/main/java/com/sky/config/RabbitMqConfiguration.java sky-server/src/main/java/com/sky/message/OrderTimeoutMessage.java sky-server/src/main/java/com/sky/service/OrderTimeoutPublisher.java sky-server/src/main/java/com/sky/service/impl/OrderTimeoutPublisherImpl.java sky-server/src/main/java/com/sky/listener/OrderTimeoutListener.java sky-server/src/main/java/com/sky/service/impl/OrderServiceImpl.java sky-server/src/test/java/com/sky/listener/OrderTimeoutListenerTest.java
git -C sky-take-out commit -m "feat: add rabbitmq dead letter order timeout flow"
```

### Task 5: 将定时任务改为补偿角色

**Files:**
- Modify: `sky-server/src/main/java/com/sky/task/OrderTask.java`
- Test: `sky-server/src/test/java/com/sky/task/OrderTaskTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void shouldOnlyCancelOrdersStillPendingPaymentInCompensationTask() {
    when(orderMapper.getByStatusAndOrderTimeLT(eq(Orders.PENDING_PAYMENT), any())).thenReturn(List.of(
        Orders.builder().id(1L).status(Orders.PENDING_PAYMENT).build()
    ));

    orderTask.processTimeoutOrder();

    verify(orderMapper).update(argThat(order -> order.getId().equals(1L) && order.getStatus().equals(Orders.CANCELLED)));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl sky-server -Dtest=OrderTaskTest test`
Expected: FAIL because task behavior or naming does not match compensation design

- [ ] **Step 3: Write minimal implementation**

- 调整日志文案，标识为补偿任务
- 保留待支付订单取消逻辑
- 只承担兜底，不再作为主设计说明

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl sky-server -Dtest=OrderTaskTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git -C sky-take-out add sky-server/src/main/java/com/sky/task/OrderTask.java sky-server/src/test/java/com/sky/task/OrderTaskTest.java
git -C sky-take-out commit -m "refactor: keep order task as compensation job"
```

### Task 6: 新增优惠券秒杀数据模型与接口骨架

**Files:**
- Create: `sky-pojo/src/main/java/com/sky/entity/Coupon.java`
- Create: `sky-pojo/src/main/java/com/sky/entity/CouponReceiveRecord.java`
- Create: `sky-pojo/src/main/java/com/sky/entity/CouponSeckillOrder.java`
- Create: `sky-pojo/src/main/java/com/sky/dto/CouponDTO.java`
- Create: `sky-pojo/src/main/java/com/sky/dto/CouponSeckillDTO.java`
- Create: `sky-pojo/src/main/java/com/sky/vo/CouponVO.java`
- Create: `sky-server/src/main/java/com/sky/controller/admin/CouponController.java`
- Create: `sky-server/src/main/java/com/sky/controller/user/CouponController.java`
- Create: `sky-server/src/main/java/com/sky/service/CouponService.java`
- Create: `sky-server/src/main/java/com/sky/service/impl/CouponServiceImpl.java`
- Create: `sky-server/src/main/java/com/sky/mapper/CouponMapper.java`
- Create: `sky-server/src/main/java/com/sky/mapper/CouponReceiveRecordMapper.java`
- Create: `sky-server/src/main/java/com/sky/mapper/CouponSeckillOrderMapper.java`
- Create: `sky-server/src/main/resources/mapper/CouponMapper.xml`
- Create: `sky-server/src/main/resources/mapper/CouponReceiveRecordMapper.xml`
- Create: `sky-server/src/main/resources/mapper/CouponSeckillOrderMapper.xml`
- Test: `sky-server/src/test/java/com/sky/service/CouponServiceStructureTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void shouldCreateCouponActivityAndWarmRedisStock() {
    CouponDTO dto = new CouponDTO();
    dto.setName("限时券");
    dto.setStock(50);
    dto.setLimitPerUser(1);

    couponService.createCoupon(dto);

    verify(couponMapper).insert(any(Coupon.class));
    verify(stringRedisTemplate).opsForValue().set("coupon:stock:1", "50");
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl sky-server -Dtest=CouponServiceStructureTest test`
Expected: FAIL because coupon domain classes are missing

- [ ] **Step 3: Write minimal implementation**

- 新建优惠券相关实体、DTO、VO、Mapper、XML、Controller、Service
- 实现创建活动与 Redis 库存预热

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl sky-server -Dtest=CouponServiceStructureTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git -C sky-take-out add sky-pojo/src/main/java/com/sky/entity/Coupon.java sky-pojo/src/main/java/com/sky/entity/CouponReceiveRecord.java sky-pojo/src/main/java/com/sky/entity/CouponSeckillOrder.java sky-pojo/src/main/java/com/sky/dto/CouponDTO.java sky-pojo/src/main/java/com/sky/dto/CouponSeckillDTO.java sky-pojo/src/main/java/com/sky/vo/CouponVO.java sky-server/src/main/java/com/sky/controller/admin/CouponController.java sky-server/src/main/java/com/sky/controller/user/CouponController.java sky-server/src/main/java/com/sky/service/CouponService.java sky-server/src/main/java/com/sky/service/impl/CouponServiceImpl.java sky-server/src/main/java/com/sky/mapper/CouponMapper.java sky-server/src/main/java/com/sky/mapper/CouponReceiveRecordMapper.java sky-server/src/main/java/com/sky/mapper/CouponSeckillOrderMapper.java sky-server/src/main/resources/mapper/CouponMapper.xml sky-server/src/main/resources/mapper/CouponReceiveRecordMapper.xml sky-server/src/main/resources/mapper/CouponSeckillOrderMapper.xml sky-server/src/test/java/com/sky/service/CouponServiceStructureTest.java
git -C sky-take-out commit -m "feat: add coupon seckill domain skeleton"
```

### Task 7: 实现 Lua 原子扣减与抢券逻辑

**Files:**
- Create: `sky-server/src/main/java/com/sky/service/CouponSeckillScriptService.java`
- Create: `sky-server/src/main/java/com/sky/service/impl/CouponSeckillScriptServiceImpl.java`
- Modify: `sky-server/src/main/java/com/sky/service/impl/CouponServiceImpl.java`
- Create: `sky-server/src/test/java/com/sky/service/CouponSeckillScriptServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void shouldReturnDuplicateWhenUserAlreadySeckilled() {
    when(redisTemplate.execute(any(), anyList(), any(), any())).thenReturn(-1L);

    int result = couponSeckillScriptService.seckill(3L, 8L, 1);

    assertEquals(-1, result);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl sky-server -Dtest=CouponSeckillScriptServiceTest test`
Expected: FAIL because script execution service does not exist

- [ ] **Step 3: Write minimal implementation**

- 新建 Lua 执行服务
- 加载 `lua/coupon_seckill.lua`
- 在抢券入口中调用脚本，根据返回值处理库存不足、重复抢券、成功三种结果
- 成功后写入秒杀流水和领券记录

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl sky-server -Dtest=CouponSeckillScriptServiceTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git -C sky-take-out add sky-server/src/main/java/com/sky/service/CouponSeckillScriptService.java sky-server/src/main/java/com/sky/service/impl/CouponSeckillScriptServiceImpl.java sky-server/src/main/java/com/sky/service/impl/CouponServiceImpl.java sky-server/src/test/java/com/sky/service/CouponSeckillScriptServiceTest.java sky-server/src/main/resources/lua/coupon_seckill.lua
git -C sky-take-out commit -m "feat: add lua based coupon seckill stock deduction"
```

### Task 8: 接入业务链路日志

**Files:**
- Create: `sky-server/src/main/java/com/sky/filter/TraceIdFilter.java`
- Create: `sky-server/src/main/java/com/sky/config/FilterConfiguration.java`
- Modify: `sky-server/src/main/java/com/sky/service/impl/OrderServiceImpl.java`
- Modify: `sky-server/src/main/java/com/sky/service/impl/CouponServiceImpl.java`
- Modify: `sky-server/src/main/java/com/sky/aspect/PermissionAspect.java`
- Modify: `sky-server/src/main/java/com/sky/handler/GlobalExceptionHandler.java`
- Test: `sky-server/src/test/java/com/sky/filter/TraceIdFilterTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void shouldPopulateTraceIdIntoMdc() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/shop/status");
    MockHttpServletResponse response = new MockHttpServletResponse();

    traceIdFilter.doFilter(request, response, filterChain);

    assertNotNull(capturedTraceId.get());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl sky-server -Dtest=TraceIdFilterTest test`
Expected: FAIL because filter is missing

- [ ] **Step 3: Write minimal implementation**

- 新建 `TraceIdFilter`
- 将 `traceId`、`userId`、`identity` 写入 `MDC`
- 在订单、优惠券、权限拒绝、异常处理处补业务日志

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl sky-server -Dtest=TraceIdFilterTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git -C sky-take-out add sky-server/src/main/java/com/sky/filter/TraceIdFilter.java sky-server/src/main/java/com/sky/config/FilterConfiguration.java sky-server/src/main/java/com/sky/service/impl/OrderServiceImpl.java sky-server/src/main/java/com/sky/service/impl/CouponServiceImpl.java sky-server/src/main/java/com/sky/aspect/PermissionAspect.java sky-server/src/main/java/com/sky/handler/GlobalExceptionHandler.java sky-server/src/test/java/com/sky/filter/TraceIdFilterTest.java
git -C sky-take-out commit -m "feat: add end to end business trace logging"
```

### Task 9: 补充 SQL 与手工验证文档

**Files:**
- Create: `sky-take-out/docs/superpowers/specs/2026-05-05-auth-order-coupon-logging-sql.md`
- Create: `sky-server/src/main/resources/sql/2026-05-05-auth-order-coupon-logging.sql`

- [ ] **Step 1: Write the failing test**

```java
@Test
void shouldExposeSqlInitializationScript() {
    assertNotNull(getClass().getClassLoader().getResource("sql/2026-05-05-auth-order-coupon-logging.sql"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl sky-server -Dtest=InfrastructureResourceTest#shouldLoadInfrastructureResources test`
Expected: FAIL because SQL resource is missing

- [ ] **Step 3: Write minimal implementation**

- 新增建表与初始化 SQL
- 写明 RabbitMQ、Redis、接口验证步骤

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl sky-server -Dtest=InfrastructureResourceTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git -C sky-take-out add docs/superpowers/specs/2026-05-05-auth-order-coupon-logging-sql.md sky-server/src/main/resources/sql/2026-05-05-auth-order-coupon-logging.sql
git -C sky-take-out commit -m "docs: add sql and verification guide for backend retrofit"
```

### Task 10: 全量验证

**Files:**
- Modify: implementation files touched above as needed
- Test: `sky-server/src/test/java/...`

- [ ] **Step 1: Write the failing test**

选择一个集成场景测试，至少覆盖“无权限拒绝”或“抢券重复失败”之一。

```java
@Test
void shouldRejectDuplicateCouponSeckillRequest() {
    when(redisTemplate.execute(any(), anyList(), any(), any())).thenReturn(1L, -1L);

    couponService.seckill(new CouponSeckillDTO(1L));

    assertThrows(BaseException.class, () -> couponService.seckill(new CouponSeckillDTO(1L)));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl sky-server -Dtest=CouponServiceIntegrationTest test`
Expected: FAIL until remaining integration wiring is complete

- [ ] **Step 3: Write minimal implementation**

- 补齐缺失 wiring
- 修正接口、Mapper、配置与日志中发现的不一致项

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl sky-server test`
Expected: PASS for all runnable tests in `sky-server`

- [ ] **Step 5: Commit**

```bash
git -C sky-take-out add sky-server
git -C sky-take-out add sky-common
git -C sky-take-out add sky-pojo
git -C sky-take-out commit -m "feat: complete backend retrofit for auth mq coupon and logging"
```

## Self-Review

- 规格覆盖检查：计划已覆盖 JWT、AOP 权限、RabbitMQ 死信、Lua 秒杀、Logback、SQL 与验证文档。
- 占位符检查：未使用 `TODO`、`TBD`、`later` 等占位语。
- 类型一致性检查：统一使用 `Coupon`、`CouponReceiveRecord`、`CouponSeckillOrder`、`OrderTimeoutMessage`、`AuthContext` 等命名。
