package org.example.fleets.common.exception;

import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
public enum ErrorCode {
    
    // 通用错误 1xxx
    SUCCESS(0, "成功"),
    BUSINESS_ERROR(1000, "业务错误"),
    PARAM_ERROR(1001, "参数错误"),
    SYSTEM_ERROR(1002, "系统错误"),
    FAILED(1003, "操作失败"),
    VALIDATE_FAILED(1004, "参数校验失败"),
    NOT_IMPLEMENTED(1005, "功能未实现"),
    UNAUTHORIZED(1006, "未授权"),
    
    // 用户模块 2xxx
    USER_NOT_FOUND(2001, "用户不存在"),
    USER_ALREADY_EXISTS(2002, "用户已存在"),
    PASSWORD_ERROR(2003, "密码错误"),
    USER_DISABLED(2004, "用户已被禁用"),
    
    // 好友模块 3xxx
    FRIENDSHIP_NOT_FOUND(3001, "好友关系不存在"),
    FRIENDSHIP_ALREADY_EXISTS(3002, "已经是好友"),
    CANNOT_ADD_SELF(3003, "不能添加自己为好友"),
    
    // 消息模块 4xxx
    MESSAGE_NOT_FOUND(4001, "消息不存在"),
    MESSAGE_SEND_FAILED(4002, "消息发送失败"),
    MESSAGE_SAVE_FAILED(4003, "消息保存失败"),
    MESSAGE_CONTENT_EMPTY(4004, "消息内容不能为空"),
    MESSAGE_CONTENT_TOO_LONG(4005, "消息内容过长"),
    NOT_FRIEND_CANNOT_SEND(4006, "不是好友，无法发送消息"),
    INVALID_MESSAGE_TYPE(4007, "无效的消息类型"),
    
    // 信箱模块 5xxx
    MAILBOX_WRITE_FAILED(5001, "信箱写入失败"),
    MAILBOX_READ_FAILED(5002, "信箱读取失败"),
    MAILBOX_NOT_FOUND(5003, "信箱不存在"),
    
    // 群组模块 6xxx
    GROUP_NOT_FOUND(6001, "群组不存在"),
    NOT_GROUP_MEMBER(6002, "不是群成员"),
    GROUP_FULL(6003, "群组人数已满"),
    
    // WebSocket 模块 7xxx
    WEBSOCKET_AUTH_FAILED(7001, "WebSocket 认证失败"),
    WEBSOCKET_CONNECT_FAILED(7002, "WebSocket 连接失败"),
    
    // 文件模块 8xxx
    FILE_UPLOAD_FAILED(8001, "文件上传失败"),
    FILE_NOT_FOUND(8002, "文件不存在"),
    FILE_TOO_LARGE(8003, "文件过大");
    
    private final int code;
    private final String message;
    
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
