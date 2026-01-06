package org.example.fleets.message.controller;

import org.example.fleets.common.api.CommonResult;
import org.example.fleets.common.util.PageResult;
import org.example.fleets.message.model.dto.MessageSendDTO;
import org.example.fleets.message.model.vo.MessageVO;
import org.example.fleets.message.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 消息控制器
 */
@RestController
@RequestMapping("/api/message")
public class MessageController {
    
    @Autowired
    private MessageService messageService;
    
    /**
     * 发送消息
     */
    @PostMapping("/send")
    public CommonResult<MessageVO> sendMessage(@RequestBody MessageSendDTO sendDTO, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        MessageVO messageVO = messageService.sendMessage(userId, sendDTO);
        return CommonResult.success(messageVO);
    }
    
    /**
     * 撤回消息
     */
    @PostMapping("/recall/{messageId}")
    public CommonResult<Boolean> recallMessage(@PathVariable String messageId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean result = messageService.recallMessage(messageId, userId);
        return CommonResult.success(result);
    }
    
    /**
     * 删除消息
     */
    @DeleteMapping("/{messageId}")
    public CommonResult<Boolean> deleteMessage(@PathVariable String messageId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean result = messageService.deleteMessage(messageId, userId);
        return CommonResult.success(result);
    }
    
    /**
     * 标记消息已读
     */
    @PostMapping("/read/{messageId}")
    public CommonResult<Boolean> markAsRead(@PathVariable String messageId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean result = messageService.markAsRead(messageId, userId);
        return CommonResult.success(result);
    }
    
    /**
     * 批量标记已读
     */
    @PostMapping("/read/batch")
    public CommonResult<Boolean> batchMarkAsRead(@RequestBody List<String> messageIds, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
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
            @RequestParam(defaultValue = "20") Integer pageSize,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
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
        PageResult<MessageVO> result = messageService.getGroupChatHistory(groupId, pageNum, pageSize);
        return CommonResult.success(result);
    }
    
    /**
     * 搜索消息
     */
    @GetMapping("/search")
    public CommonResult<PageResult<MessageVO>> searchMessage(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        PageResult<MessageVO> result = messageService.searchMessage(userId, keyword, pageNum, pageSize);
        return CommonResult.success(result);
    }
}
