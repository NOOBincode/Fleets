package org.example.fleets.message.controller;

import org.example.fleets.common.api.CommonResult;
import org.example.fleets.message.model.vo.MessageVO;
import org.example.fleets.message.service.MessageSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息同步控制器
 */
@RestController
@RequestMapping("/api/message/sync")
public class MessageSyncController {
    
    @Autowired
    private MessageSyncService messageSyncService;
    
    /**
     * 拉取离线消息
     */
    @GetMapping("/pull")
    public CommonResult<List<MessageVO>> pullOfflineMessages(
            @RequestParam(required = false, defaultValue = "0") Long lastSequence,
            @RequestParam(required = false, defaultValue = "100") Integer limit,
            HttpServletRequest request) {
        
        Long userId = (Long) request.getAttribute("userId");
        List<MessageVO> messages = messageSyncService.pullOfflineMessages(userId, lastSequence, limit);
        return CommonResult.success(messages);
    }
    
    /**
     * 获取同步信息（最后序列号、未读数等）
     */
    @GetMapping("/info")
    public CommonResult<Map<String, Object>> getSyncInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        
        Map<String, Object> info = new HashMap<>();
        info.put("lastSequence", messageSyncService.getLastSequence(userId));
        info.put("unreadCount", messageSyncService.getUnreadCount(userId));
        
        return CommonResult.success(info);
    }
    
    /**
     * 更新同步序列号
     */
    @PostMapping("/update-sequence")
    public CommonResult<Boolean> updateSequence(
            @RequestParam Long sequence,
            HttpServletRequest request) {
        
        Long userId = (Long) request.getAttribute("userId");
        messageSyncService.updateLastSequence(userId, sequence);
        return CommonResult.success(true);
    }
}
