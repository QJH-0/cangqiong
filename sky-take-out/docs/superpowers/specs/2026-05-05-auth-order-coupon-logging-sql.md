# 后端改造 SQL 与验证说明

## SQL

执行资源文件：

- `sky-server/src/main/resources/sql/2026-05-05-auth-order-coupon-logging.sql`

该脚本包含：

- 权限模型表：`permission`、`role`、`role_permission`、`employee_role`
- 优惠券表：`coupon`、`coupon_receive_record`、`coupon_seckill_order`
- 基础权限初始化数据
- 一个示例超级管理员角色绑定

## 环境准备

1. 启动 MySQL，并确保业务库为 `sky_take_out`
2. 启动 Redis，默认 `localhost:6379`
3. 启动 RabbitMQ，默认 `localhost:5672`
4. 执行 SQL 脚本

## 验证建议

1. 管理员登录后调用 `POST /admin/employee`
   - 若员工 `id=1` 已绑定超级管理员角色，应放行
   - 未绑定权限的管理员应返回权限不足
2. 调用 `POST /admin/coupon`
   - 创建优惠券活动后，Redis 中应出现 `coupon:stock:{couponId}`
3. 调用 `POST /user/coupon/seckill`
   - 首次抢券成功
   - 同一用户重复抢券应失败
4. 创建待支付订单
   - 15 分钟后消息应进入死信队列并自动取消订单
