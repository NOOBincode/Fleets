create table chat_group
(
    id           bigint auto_increment comment '主键ID'
        primary key,
    group_name   varchar(64)                          not null comment '群组名称',
    avatar       varchar(255)                         null comment '群头像',
    owner_id     bigint                               not null comment '群主ID',
    announcement varchar(255)                         null comment '群公告',
    description  varchar(255)                         null comment '群简介',
    max_members  int        default 200               null comment '最大成员数',
    member_count int        default 1                 null comment '当前成员数',
    status       tinyint(1) default 0                 null comment '群状态：0-正常 1-全员禁言 2-解散',
    join_type    tinyint(1) default 0                 null comment '加群方式：0-无需验证 1-需要验证 2-禁止加群',
    is_deleted   tinyint(1) default 0                 null comment '逻辑删除：0-未删除 1-已删除',
    create_time  datetime   default CURRENT_TIMESTAMP null comment '创建时间',
    update_time  datetime   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '群组表';

create index idx_group_owner_id
    on chat_group (owner_id);

create table conversation
(
    id                   bigint auto_increment comment '主键ID'
        primary key,
    conversation_id      varchar(64)                          not null comment '会话ID（conv_x_y / conv_group_gid）',
    type                 tinyint(1) default 0                 not null comment '会话类型：0-单聊 1-群聊',
    owner_id             bigint                               not null comment '会话所有者ID',
    target_id            bigint                               not null comment '目标ID（对方用户ID或群ID）',
    unread_count         int        default 0                 not null comment '未读消息数',
    last_message_id      varchar(64)                          null comment '最后一条消息ID',
    last_message_content varchar(255)                         null comment '最后一条消息内容',
    last_message_time    datetime                             null comment '最后一条消息时间',
    last_sender_id       bigint                               null comment '最后一条消息发送者ID',
    last_sender_name     varchar(64)                          null comment '最后一条消息发送者昵称',
    is_top               tinyint(1) default 0                 not null comment '是否置顶：0-否 1-是',
    is_mute              tinyint(1) default 0                 not null comment '是否免打扰：0-否 1-是',
    is_deleted           tinyint(1) default 0                 not null comment '逻辑删除标记',
    create_time          datetime   default CURRENT_TIMESTAMP null comment '创建时间',
    update_time          datetime   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '会话表';

create index idx_owner
    on conversation (owner_id);

create index idx_owner_conv
    on conversation (owner_id, conversation_id);

create table file
(
    id            bigint auto_increment comment '文件ID'
        primary key,
    original_name varchar(255)                         not null comment '原始文件名',
    file_name     varchar(255)                         not null comment '存储文件名',
    file_path     varchar(255)                         not null comment '文件路径',
    file_url      varchar(255)                         not null comment '文件访问 URL',
    uploader_id   bigint                               not null comment '上传用户ID',
    file_size     bigint                               not null comment '文件大小(字节)',
    file_type     varchar(64)                          not null comment '文件类型',
    file_md5      varchar(32)                          not null comment '文件MD5值',
    status        tinyint(1) default 1                 not null comment '状态：0-上传中，1-已上传，2-上传失败',
    is_deleted    tinyint(1) default 0                 not null comment '是否删除：0-否，1-是',
    create_time   datetime   default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time   datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '文件表';

create index idx_file_md5
    on file (file_md5);

create index idx_is_deleted
    on file (is_deleted);

create table friendship
(
    id          bigint auto_increment comment '关系ID'
        primary key,
    user_id     bigint                               not null comment '用户ID',
    friend_id   bigint                               not null comment '好友ID',
    remark      varchar(64)                          null comment '好友备注',
    status      tinyint(1) default 0                 not null comment '状态：0-待确认，1-已确认，2-已拒绝，3-已拉黑',
    is_deleted  tinyint(1) default 0                 not null comment '是否删除：0-否，1-是',
    create_time datetime   default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint idx_user_friend
        unique (user_id, friend_id)
)
    comment '好友关系表';

create index idx_friend_id
    on friendship (friend_id);

create index idx_is_deleted
    on friendship (is_deleted);

create table group_member
(
    id             bigint auto_increment comment '主键ID'
        primary key,
    group_id       bigint                               not null comment '群组ID',
    user_id        bigint                               not null comment '用户ID',
    group_nickname varchar(64)                          null comment '群内昵称',
    role           tinyint(1) default 0                 null comment '成员角色：0-普通成员 1-管理员 2-群主',
    mute_status    tinyint(1) default 0                 null comment '禁言状态：0-正常 1-禁言',
    mute_end_time  datetime                             null comment '禁言结束时间',
    join_time      datetime   default CURRENT_TIMESTAMP null comment '加入时间',
    is_deleted     tinyint(1) default 0                 null comment '逻辑删除：0-未删除 1-已删除',
    create_time    datetime   default CURRENT_TIMESTAMP null comment '创建时间',
    update_time    datetime   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_group_member_group_user
        unique (group_id, user_id)
)
    comment '群成员表';

create index idx_group_member_group_id
    on group_member (group_id);

create index idx_group_member_user_id
    on group_member (user_id);

create table mailbox
(
    id              bigint auto_increment comment '信箱ID'
        primary key,
    user_id         bigint                               not null comment '用户ID',
    sequence_id     bigint                               not null comment '序列ID',
    message_id      bigint                               not null comment '消息ID',
    conversation_id varchar(64)                          not null comment '会话ID',
    is_read         tinyint(1) default 0                 not null comment '是否已读：0-未读，1-已读',
    is_deleted      tinyint(1) default 0                 not null comment '是否删除：0-否，1-是',
    create_time     datetime   default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time     datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint idx_user_sequence
        unique (user_id, sequence_id)
)
    comment '信箱表';

create index idx_is_deleted
    on mailbox (is_deleted);

create index idx_message_id
    on mailbox (message_id);

create index idx_user_conversation
    on mailbox (user_id, conversation_id);

create table sequence
(
    id            bigint auto_increment comment 'ID'
        primary key,
    name          varchar(64)                        not null comment '序列名称',
    current_value bigint   default 0                 not null comment '当前值',
    create_time   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint idx_name
        unique (name)
)
    comment '消息序列表';

create table user
(
    id              bigint auto_increment comment '用户ID'
        primary key,
    username        varchar(64)                          not null comment '用户名',
    password        varchar(128)                         not null comment '密码（加密存储）',
    nickname        varchar(64)                          null comment '昵称',
    avatar          varchar(255)                         null comment '头像URL',
    phone           varchar(20)                          null comment '手机号',
    email           varchar(64)                          null comment '邮箱',
    gender          tinyint(1) default 0                 null comment '性别：0-未知，1-男，2-女',
    birth_date      date                                 null comment '出生日期',
    signature       varchar(255)                         null comment '个性签名',
    status          tinyint(1) default 1                 not null comment '状态：0-禁用，1-正常',
    last_login_time datetime                             null comment '最后登录时间',
    last_login_ip   varchar(64)                          null comment '最后登录IP',
    is_deleted      tinyint(1) default 0                 not null comment '是否删除：0-否，1-是',
    create_time     datetime   default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time     datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint idx_username
        unique (username)
)
    comment '用户表';

create index idx_email
    on user (email);

create index idx_is_deleted
    on user (is_deleted);

create index idx_phone
    on user (phone);