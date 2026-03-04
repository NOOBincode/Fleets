package org.example.fleets.common.controller;

import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;

import org.example.fleets.common.api.CommonResult;
import org.example.fleets.common.model.Conversation;
import org.example.fleets.common.service.ConversationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会话控制器
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversation")
public class ConversationController {
    
    private final ConversationService conversationService;
    
    /**
     * 获取用户的会话列表
     */
    @GetMapping("/list")
    public CommonResult<List<Conversation>> getUserConversations() {
        Long userId = StpUtil.getLoginIdAsLong();   
        List<Conversation> conversations = conversationService.getUserConversations(userId);
        return CommonResult.success(conversations);
    }
    
    /**
     * 清空会话未读数
     */
    @PostMapping("/{conversationId}/clear-unread")
    public CommonResult<Boolean> clearUnreadCount(
            @PathVariable String conversationId) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        conversationService.clearUnreadCount(conversationId, userId);
        return CommonResult.success(true);
    }
    
    /**
     * 删除会话
     */
    @DeleteMapping("/{conversationId}")
    public CommonResult<Boolean> deleteConversation(
            @PathVariable String conversationId) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = conversationService.deleteConversation(conversationId, userId);
        return CommonResult.success(result);
    }
    
    /**
     * 置顶/取消置顶会话
     */
    @PostMapping("/{conversationId}/top")
    public CommonResult<Boolean> toggleTop(
            @PathVariable String conversationId,
            @RequestParam boolean isTop) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = conversationService.toggleTop(conversationId, userId, isTop);
        return CommonResult.success(result);
    }
    
    /**
     * 免打扰/取消免打扰
     */
    @PostMapping("/{conversationId}/mute")
    public CommonResult<Boolean> toggleMute(
            @PathVariable String conversationId,
            @RequestParam boolean isMute) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = conversationService.toggleMute(conversationId, userId, isMute);
        return CommonResult.success(result);
    }
}
