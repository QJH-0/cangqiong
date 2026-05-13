create table if not exists permission
(
    id          bigint primary key auto_increment,
    code        varchar(64)  not null unique,
    name        varchar(128) not null,
    create_time datetime     not null default current_timestamp
);

create table if not exists role
(
    id          bigint primary key auto_increment,
    name        varchar(64) not null unique,
    create_time datetime    not null default current_timestamp
);

create table if not exists role_permission
(
    id            bigint primary key auto_increment,
    role_id       bigint not null,
    permission_id bigint not null,
    unique key uk_role_permission (role_id, permission_id)
);

create table if not exists employee_role
(
    id          bigint primary key auto_increment,
    employee_id bigint not null,
    role_id     bigint not null,
    unique key uk_employee_role (employee_id, role_id)
);

create table if not exists coupon
(
    id             bigint primary key auto_increment,
    name           varchar(128)  not null,
    description    varchar(255)  null,
    stock          int           not null,
    status         tinyint       not null default 1,
    start_time     datetime      null,
    end_time       datetime      null,
    limit_per_user int           not null default 1,
    create_time    datetime      not null,
    update_time    datetime      not null
);

create table if not exists coupon_receive_record
(
    id           bigint primary key auto_increment,
    coupon_id    bigint       not null,
    user_id      bigint       not null,
    receive_time datetime     not null,
    source       varchar(32)  not null
);

create table if not exists coupon_seckill_order
(
    id          bigint primary key auto_increment,
    coupon_id   bigint      not null,
    user_id     bigint      not null,
    status      tinyint     not null,
    create_time datetime    not null,
    unique key uk_coupon_user (coupon_id, user_id)
);

insert ignore into permission(code, name) values
('employee:create', '创建员工'),
('employee:update', '修改员工'),
('employee:status', '调整员工状态'),
('coupon:create', '创建优惠券活动');

insert ignore into role(id, name) values
(1, 'SUPER_ADMIN');

insert ignore into role_permission(role_id, permission_id)
select 1, id from permission;

insert ignore into employee_role(employee_id, role_id) values
(1, 1);
