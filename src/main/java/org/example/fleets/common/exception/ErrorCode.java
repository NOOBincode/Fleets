package org.example.fleets.common.exception;

import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
public enum ErrorCode {
    
    // ========== 通用错误 1xxx ==========
    SUCCESS(200, "操作成功"),
    FAILED(500, "操作失败"),
    VALIDATE_FAILED(400, "参数校验失败"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "没有权限"),
    NOT_FOUND(404, "资源不存在"),
    
    // ========== 用户相关 2xxx ==========
    USER_NOT_FOUND(2001, "用户不存在"),
    USER_ALREADY_EXISTS(2002, "用户已存在"),
    USERNAME_OR_PASSWORD_ERROR(2003, "用户名或密码错误"),
    USER_DISABLED(2004, "用户已被禁用"),
    TOKEN_EXPIRED(2005, "Token已过期"),
    TOKEN_INVALID(2006, "Token无效"),
    
    // ========== 好友相关 3xxx ==========
    FRIEND_NOT_FOUND(3001, "好友不存在"),
    FRIEND_ALREADY_EXISTS(3002, "已经是好友"),
    FRIEND_REQUEST_NOT_FOUND(3003, "好友请求不存在"),
    CANNOT_ADD_SELF(3004, "不能添加自己为好友"),
    FRIEND_BLOCKED(3005, "对方已将你拉黑"),
    
    // ========== 群组相关 4xxx ==========
    GROUP_NOT_FOUND(4001, "群组不存在"),
    GROUP_MEMBER_NOT_FOUND(4002, "不是群成员"),
    GROUP_FULL(4003, "群组已满"),
    NOT_GROUP_OWNER(4004, "不是群主"),
    NOT_GROUP_ADMIN(4005, "不是群管理员"),
    GROUP_MUTED(4006, "群组已被禁言"),
    MEMBER_MUTED(4007, "你已被禁言"),
    
    // ========== 消息相关 5xxx ==========
    MESSAGE_NOT_FOUND(5001, "消息不存在"),
    MESSAGE_SEND_FAILED(5002, "消息发送失败"),
    MESSAGE_RECALL_TIMEOUT(5003, "消息撤回超时"),
    MESSAGE_ALREADY_RECALLED(5004, "消息已撤回"),
    CANNOT_RECALL_OTHERS_MESSAGE(5005, "不能撤回他人消息"),
    
    // ========== 文件相关 6xxx ==========
    FILE_NOT_FOUND(6001, "文件不存在"),
    FILE_UPLOAD_FAILED(6002, "文件上传失败"),
    FILE_TOO_LARGE(6003, "文件过大"),
    FILE_TYPE_NOT_SUPPORTED(6004, "文件类型不支持"),
    
    // ========== 系统相关 9xxx ==========
    SYSTEM_ERROR(9001, "系统错误"),
    DATABASE_ERROR(9002, "数据库错误"),
    REDIS_ERROR(9003, "Redis错误"),
    MQ_ERROR(9004, "消息队列错误"),
    NETWORK_ERROR(9005, "网络错误");
    
    private final Integer code;
    private final String message;
    
    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
