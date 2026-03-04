package org.example.fleets.message.controller;

import lombok.RequiredArgsConstructor;
import cn.dev33.satoken.stp.StpUtil;
import org.example.fleets.common.api.CommonResult;
import org.example.fleets.common.util.PageResult;
import org.example.fleets.message.model.dto.MessageSendDTO;
import org.example.fleets.message.model.vo.MessageVO;
import org.example.fleets.message.service.MessageService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 消息控制器
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/message")
public class MessageController {
    
    private final MessageService messageService;
    
    /**
     * 发送消息
     */
    @PostMapping("/send")
    public CommonResult<MessageVO> sendMessage(@Valid @RequestBody MessageSendDTO sendDTO) {
        Long userId = StpUtil.getLoginIdAsLong();
        MessageVO messageVO = messageService.sendMessage(userId, sendDTO);
        return CommonResult.success(messageVO);
    }
    
    /**
     * 撤回消息
     */
    @PostMapping("/recall/{messageId}")
    public CommonResult<Boolean> recallMessage(@PathVariable String messageId) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = messageService.recallMessage(messageId, userId);
        return CommonResult.success(result);
    }
    
    /**
     * 删除消息
     */
    @DeleteMapping("/{messageId}")
    public CommonResult<Boolean> deleteMessage(@PathVariable String messageId) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = messageService.deleteMessage(messageId, userId);
        return CommonResult.success(result);
    }
    
    /**
     * 标记消息已读
     */
    @PostMapping("/read/{messageId}")
    public CommonResult<Boolean> markAsRead(@PathVariable String messageId) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = messageService.markAsRead(messageId, userId);
        return CommonResult.success(result);
    }
    
    /**
     * 批量标记已读
     */
    @PostMapping("/read/batch")
    public CommonResult<Boolean> batchMarkAsRead(@RequestBody List<String> messageIds) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = messageService.batchMarkAsRead(messageIds, userId);
        return CommonResult.success(result);
    }
    
    /**
     * 获取单聊消息历史
     */
    @GetMapping("/chat/{targetUserId}")
    public CommonResult<PageResult<MessageVO>> getChatHistory(
            @PathVariable Long targetUserId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        PageResult<MessageVO> result = messageService.getChatHistory(userId, targetUserId, pageNum, pageSize);
        return CommonResult.success(result);
    }
    
    /**
     * 获取群聊消息历史
     */
    @GetMapping("/group/{groupId}")
    public CommonResult<PageResult<MessageVO>> getGroupChatHistory(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        PageResult<MessageVO> result = messageService.getGroupChatHistory(userId, groupId, pageNum, pageSize);
        return CommonResult.success(result);
    }
    
    /**
     * 搜索消息
     */
    @GetMapping("/search")
    public CommonResult<PageResult<MessageVO>> searchMessage(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        PageResult<MessageVO> result = messageService.searchMessage(userId, keyword, pageNum, pageSize);
        return CommonResult.success(result);
    }
}
