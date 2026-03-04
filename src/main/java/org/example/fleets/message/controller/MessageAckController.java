package org.example.fleets.message.controller;

import lombok.RequiredArgsConstructor;
import cn.dev33.satoken.stp.StpUtil;
import org.example.fleets.common.api.CommonResult;
import org.example.fleets.message.service.MessageAckService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 消息确认控制器
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/message/ack")
public class MessageAckController {
    
    private final MessageAckService messageAckService;
    
    /**
     * 送达确认
     */
    @PostMapping("/delivered/{messageId}")
    public CommonResult<Boolean> deliveredAck(@PathVariable String messageId) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        messageAckService.handleDeliveredAck(userId, messageId);
        return CommonResult.success(true);
    }
    
    /**
     * 已读确认
     */
    @PostMapping("/read/{messageId}")
    public CommonResult<Boolean> readAck(@PathVariable String messageId) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        messageAckService.handleReadAck(userId, messageId);
        return CommonResult.success(true);
    }
    
    /**
     * 批量已读确认
     */
    @PostMapping("/read/batch")
    public CommonResult<Boolean> batchReadAck(@RequestBody List<String> messageIds) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        messageAckService.batchHandleReadAck(userId, messageIds);
        return CommonResult.success(true);
    }
}
