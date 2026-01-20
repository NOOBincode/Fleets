package org.example.fleets.common.constant;

/**
 * 日志常量
 * 统一日志格式和模块标识
 */
public class LogConstants {
    
    // 模块标识
    public static final String MODULE_USER = "[用户模块]";
    public static final String MODULE_MESSAGE = "[消息模块]";
    public static final String MODULE_MAILBOX = "[信箱模块]";
    public static final String MODULE_FRIENDSHIP = "[好友模块]";
    public static final String MODULE_GROUP = "[群组模块]";
    public static final String MODULE_WEBSOCKET = "[WebSocket]";
    public static final String MODULE_FILE = "[文件模块]";
    
    // 操作类型
    public static final String OP_SEND = "发送";
    public static final String OP_RECEIVE = "接收";
    public static final String OP_QUERY = "查询";
    public static final String OP_UPDATE = "更新";
    public static final String OP_DELETE = "删除";
    public static final String OP_CREATE = "创建";
    public static final String OP_CONNECT = "连接";
    public static final String OP_DISCONNECT = "断开";
    
    // 状态
    public static final String STATUS_SUCCESS = "成功";
    public static final String STATUS_FAILURE = "失败";
    public static final String STATUS_START = "开始";
    public static final String STATUS_END = "完成";
    
    /**
     * 构建日志消息
     * 格式：[模块] 操作 - 状态 - 详情
     */
    public static String buildLog(String module, String operation, String status, String detail) {
        return String.format("%s %s - %s - %s", module, operation, status, detail);
    }
    
    /**
     * 构建简单日志消息
     * 格式：[模块] 操作 - 详情
     */
    public static String buildLog(String module, String operation, String detail) {
        return String.format("%s %s - %s", module, operation, detail);
    }
}
