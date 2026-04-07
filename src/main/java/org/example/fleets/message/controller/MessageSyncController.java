package org.example.fleets.message.controller;

import lombok.RequiredArgsConstructor;
import cn.dev33.satoken.stp.StpUtil;
import org.example.fleets.common.api.CommonResult;
import org.example.fleets.message.model.dto.UpdateSequenceRequest;
import org.example.fleets.message.model.vo.MessageVO;
import org.example.fleets.message.service.MessageSyncService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息同步控制器
 */
@RestController
@RequestMapping("/api/message/sync")
@RequiredArgsConstructor
@Validated
public class MessageSyncController {
    
    private final MessageSyncService messageSyncService;
    
    /**
     * 拉取离线消息
     */
    @GetMapping("/pull")
    public CommonResult<List<MessageVO>> pullOfflineMessages(
            @RequestParam(required = false, defaultValue = "0") Long lastSequence,
            @RequestParam(required = false, defaultValue = "100") Integer limit) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        List<MessageVO> messages = messageSyncService.pullOfflineMessages(userId, lastSequence, limit);
        return CommonResult.success(messages);
    }
    
    /**
     * 获取同步信息（最后序列号、未读数等）
     */
    @GetMapping("/info")
    public CommonResult<Map<String, Object>> getSyncInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        
        Map<String, Object> info = new HashMap<>();
        info.put("lastSequence", messageSyncService.getLastSequence(userId));
        info.put("unreadCount", messageSyncService.getUnreadCount(userId));
        
        return CommonResult.success(info);
    }
    
    /**
     * 更新同步序列号（请求体 JSON：{ "sequence": 123 }，与前端统一入参）
     */
    @PostMapping("/update-sequence")
    public CommonResult<Boolean> updateSequence(@RequestBody @Validated UpdateSequenceRequest body) {
        long sequence = body.getSequence();
        Long userId = StpUtil.getLoginIdAsLong();
        messageSyncService.updateLastSequence(userId, sequence);
        return CommonResult.success(true);
    }
}
