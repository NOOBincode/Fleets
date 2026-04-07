-- 好友申请能力扩展：已有库执行本脚本（与 docker/mysql/init.sql 中 friendship 定义对齐）
-- 执行前请备份。若列已存在，请按需删除对应语句后执行。

ALTER TABLE friendship
    ADD COLUMN applicant_user_id bigint NULL COMMENT '本次好友申请的发起人；双向两行同值；status=0/2/4 时有意义' AFTER remark,
    ADD COLUMN verify_message varchar(100) NULL COMMENT '申请附言；双向两行同值' AFTER applicant_user_id,
    ADD COLUMN last_apply_time datetime NULL COMMENT '最后一次发起/刷新申请的时间（重复 pending 时更新）' AFTER verify_message;

-- 扩展 status 语义：4=发起方撤销（与 2=接收方拒绝 区分）
ALTER TABLE friendship
    MODIFY COLUMN status tinyint(1) NOT NULL DEFAULT 0
        COMMENT '状态：0-待确认，1-已确认，2-已拒绝，3-已拉黑，4-已撤销（发起方撤回）';

ALTER TABLE friendship
    MODIFY COLUMN remark varchar(64) NULL COMMENT '我对好友的备注（仅发起方行在申请时写入；镜像行可为空）';

ALTER TABLE friendship
    MODIFY COLUMN user_id bigint NOT NULL COMMENT '用户ID（行视角：当前用户）';

ALTER TABLE friendship
    MODIFY COLUMN friend_id bigint NOT NULL COMMENT '好友ID（行视角：对端用户）';

ALTER TABLE friendship
    MODIFY COLUMN create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次建立该(user_id,friend_id)行的时间';

CREATE INDEX idx_friendship_pending_inbox ON friendship (friend_id, status, applicant_user_id);
