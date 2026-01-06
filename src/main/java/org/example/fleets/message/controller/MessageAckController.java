package org.example.fleets.message.controller;

import org.example.fleets.common.api.CommonResult;
import org.example.fleets.message.model.dto.MessageAckDTO;
import org.example.fleets.message.service.MessageAckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 消息确认控制器
 */
@RestController
@RequestMapping("/api/message/ack")
public class MessageAckController {
    
    @Autowired
    private MessageAckService messageAckService;
    
    /**
     * 送达确认
     */
    @PostMapping("/delivered/{messageId}")
    public CommonResult<Boolean> deliveredAck(
            @PathVariable String messageId,
            HttpServletRequest request) {
        
        Long userId = (Long) request.getAttribute("userId");
        messageAckService.handleDeliveredAck(userId, messageId);
        return CommonResult.success(true);
    }
    
    /**
     * 已读确认
     */
    @PostMapping("/read/{messageId}")
    public CommonResult<Boolean> readAck(
            @PathVariable String messageId,
            HttpServletRequest request) {
        
        Long userId = (Long) request.getAttribute("userId");
        messageAckService.handleReadAck(userId, messageId);
        return CommonResult.success(true);
    }
    
    /**
     * 批量已读确认
     */
    @PostMapping("/read/batch")
    public CommonResult<Boolean> batchReadAck(
            @RequestBody List<String> messageIds,
            HttpServletRequest request) {
        
        Long userId = (Long) request.getAttribute("userId");
        messageAckService.batchHandleReadAck(userId, messageIds);
        return CommonResult.success(true);
    }
}
