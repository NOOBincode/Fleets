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
     * 确保会话存在（单聊 / 群聊通用）
     *
     * 使用场景示例：
     * - 从好友详情点击“发消息”时，如果还没有任何消息记录，希望直接有一个可用会话；
     * - 从群组页面点击“进入群聊”时，如果尚未有群聊消息，也希望能直接进入会话。
     *
     * 设计要点 / TODO（需要你实现 service 逻辑后再联调）：
     * 1. 当前登录用户：
     *    - 通过 StpUtil.getLoginIdAsLong() 获取 ownerId，防止越权（禁止前端传 ownerId）
     *
     * 2. type 取值约定（建议和前端统一）：
     *    - 0：单聊会话（targetId 为对方用户 ID）
     *    - 1：群聊会话（targetId 为群组 ID）
     *
     * 3. 返回值：
     *    - 返回该用户维度下的 Conversation 记录（新建或已有）
     *    - 前端可以拿 data.id 作为当前会话数据库 ID，data.conversationId 作为业务 ID
     */
    @PostMapping("/ensure")
    public CommonResult<Conversation> ensureConversation(
            @RequestParam Integer type,
            @RequestParam Long targetId) {

        Long ownerId = StpUtil.getLoginIdAsLong();

        // TODO 1: 可在这里做一层基础校验（可选）：
        //  - 校验 type 是否在允许范围内（0 / 1）
        //  - targetId > 0
        //  - 如需更严格的权限校验（例如：是否真的是该群成员 / 是否真的是该好友），
        //    建议放到 Service 或对应业务模块中实现

        Conversation conversation = conversationService.ensureConversation(ownerId, targetId, type);
        return CommonResult.success(conversation);
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
