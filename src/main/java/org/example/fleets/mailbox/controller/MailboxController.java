package org.example.fleets.mailbox.controller;

import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.example.fleets.common.api.CommonResult;
import org.example.fleets.mailbox.model.dto.MarkReadDTO;
import org.example.fleets.mailbox.model.dto.SyncMessageDTO;
import org.example.fleets.mailbox.model.vo.SyncResult;
import org.example.fleets.mailbox.model.vo.UnreadCountVO;
import org.example.fleets.mailbox.service.MailboxService;
import org.example.fleets.message.model.vo.MessageVO;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * Mailbox控制器
 */
@RestController
@RequestMapping("/api/mailbox")
@RequiredArgsConstructor
public class MailboxController {
    
    private final MailboxService mailboxService;
    
    /**
     * 拉取离线消息
     */
    @GetMapping("/pull")
    public CommonResult<List<MessageVO>> pullOfflineMessages(
            @RequestParam(defaultValue = "0") Long lastSequence) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<MessageVO> messages = mailboxService.pullOfflineMessages(userId, lastSequence);
        return CommonResult.success(messages);
    }
    
    /**
     * 增量同步消息
     */
    @PostMapping("/sync")
    public CommonResult<SyncResult> syncMessages(
            @Valid @RequestBody SyncMessageDTO syncDTO) {
        Long userId = StpUtil.getLoginIdAsLong();
        SyncResult result = mailboxService.syncMessages(userId, syncDTO);
        return CommonResult.success(result);
    }
    
    /**
     * 标记消息已读
     */
    @PostMapping("/read")
    public CommonResult<Boolean> markAsRead(
            @Valid @RequestBody MarkReadDTO markReadDTO) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = mailboxService.markAsRead(userId, markReadDTO);
        return CommonResult.success(result, "已标记为已读");
    }
    
    /**
     * 获取未读消息数
     */
    @GetMapping("/unread")
    public CommonResult<UnreadCountVO> getUnreadCount() {
        Long userId = StpUtil.getLoginIdAsLong();
        UnreadCountVO result = mailboxService.getUnreadCount(userId);
        return CommonResult.success(result);
    }
    
    /**
     * 获取会话未读数
     */
    @GetMapping("/unread/{conversationId}")
    public CommonResult<Integer> getConversationUnreadCount(
            @PathVariable String conversationId) {
        Long userId = StpUtil.getLoginIdAsLong();
        Integer count = mailboxService.getConversationUnreadCount(userId, conversationId);
        return CommonResult.success(count);
    }
    
    /**
     * 清空会话消息
     */
    @DeleteMapping("/clear/{conversationId}")
    public CommonResult<Boolean> clearConversation(
            @PathVariable String conversationId) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = mailboxService.clearConversation(userId, conversationId);
        return CommonResult.success(result, "会话已清空");
    }
    
    /**
     * 删除消息
     */
    @DeleteMapping("/{conversationId}/{sequence}")
    public CommonResult<Boolean> deleteMessage(
            @PathVariable String conversationId,
            @PathVariable Long sequence) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = mailboxService.deleteMessage(userId, conversationId, sequence);
        return CommonResult.success(result, "消息已删除");
    }
}
