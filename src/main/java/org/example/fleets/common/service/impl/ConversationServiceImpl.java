package org.example.fleets.common.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.common.exception.BusinessException;
import org.example.fleets.common.exception.ErrorCode;
import org.example.fleets.common.mapper.ConversationMapper;
import org.example.fleets.common.model.Conversation;
import org.example.fleets.common.service.ConversationService;
import org.example.fleets.common.util.Assert;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 会话服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {
    
    private final ConversationMapper conversationMapper;
    
    @Override
    public void updateConversation(Long ownerId, Long targetId, Integer type,
                                  String messageId, String content, Date messageTime,
                                  boolean incrementUnread) {
        // 1. 生成会话ID
        String conversationId = generateConversationId(type, ownerId, targetId);
        
        // 2. 截断消息内容（最多100字符）
        String truncatedContent = truncate(content, 100);
        
        // 3. 查询会话是否存在
        Conversation conversation = conversationMapper.selectOne(
            new QueryWrapper<Conversation>()
                .eq("conversation_id", conversationId)
                .eq("owner_id", ownerId)
        );
        
        if (conversation == null) {
            // 4. 创建新会话
            conversation = new Conversation();
            conversation.setConversationId(conversationId);
            conversation.setType(type);
            conversation.setOwnerId(ownerId);
            conversation.setTargetId(targetId);
            conversation.setUnreadCount(incrementUnread ? 1 : 0);
            conversation.setLastMessageId(messageId);
            conversation.setLastMessageContent(truncatedContent);
            conversation.setLastMessageTime(messageTime);
            conversation.setIsTop(0);
            conversation.setIsMute(0);
            
            conversationMapper.insert(conversation);
            log.info("创建新会话: conversationId={}, ownerId={}", conversationId, ownerId);
        } else {
            // 5. 更新已有会话（使用幂等操作）
            int updated;
            if (incrementUnread) {
                updated = conversationMapper.incrementUnreadCount(
                    conversationId, ownerId, messageId, truncatedContent, messageTime
                );
            } else {
                updated = conversationMapper.updateLastMessage(
                    conversationId, ownerId, messageId, truncatedContent, messageTime
                );
            }
            
            if (updated > 0) {
                log.info("更新会话成功: conversationId={}, ownerId={}, incrementUnread={}", 
                    conversationId, ownerId, incrementUnread);
            } else {
                log.warn("会话未更新（可能消息时间较旧）: conversationId={}, messageTime={}", 
                    conversationId, messageTime);
            }
        }
    }

    @Override
    public Conversation ensureConversation(Long ownerId, Long targetId, Integer type) {
        // 统一风格：参数校验使用项目 Assert（抛 BusinessException），不要返回 null
        Assert.notNull(ownerId, "ownerId 不能为空");
        Assert.notNull(targetId, "targetId 不能为空");
        Assert.notNull(type, "type 不能为空");
        Assert.isTrue(type == 0 || type == 1, "type 必须为 0(单聊) 或 1(群聊)");

        final String conversationId = generateConversationId(type, ownerId, targetId);

        // 先查：按业务 ID + ownerId 唯一定位（不限定 is_deleted，用于处理“已删除会话恢复”的场景）
        Conversation existing = conversationMapper.selectOne(
            new QueryWrapper<Conversation>()
                .eq("conversation_id", conversationId)
                .eq("owner_id", ownerId)
        );
        if (existing != null) {
            // 若存在但被逻辑删除，则恢复
            if (existing.getIsDeleted() != null && existing.getIsDeleted() == 1) {
                existing.setIsDeleted(0);
                // 保持 ensure 不影响业务语义：不更新 lastMessage，不主动增加未读
                // 恢复时将未读置 0，避免“已删除会话被恢复后仍残留未读”带来困惑
                existing.setUnreadCount(0);
                conversationMapper.updateById(existing);
            }
            return existing;
        }

        // 不存在则创建“空会话”（无 lastMessage）
        Conversation conversation = new Conversation();
        conversation.setConversationId(conversationId);
        conversation.setType(type);
        conversation.setOwnerId(ownerId);
        conversation.setTargetId(targetId);
        conversation.setUnreadCount(0);
        conversation.setIsTop(0);
        conversation.setIsMute(0);
        conversation.setIsDeleted(0);

        conversationMapper.insert(conversation);
        log.info("创建空会话: conversationId={}, ownerId={}", conversationId, ownerId);
        return conversation;
    }
    
    @Override
    public List<Conversation> getUserConversations(Long userId) {
        return conversationMapper.selectList(
            new QueryWrapper<Conversation>()
                .eq("owner_id", userId)
                .eq("is_deleted", 0)
                .orderByDesc("is_top")  // 置顶的在前
                .orderByDesc("last_message_time")  // 按最后消息时间排序
        );
    }
    
    @Override
    public void clearUnreadCount(String conversationId, Long userId) {
        int updated = conversationMapper.clearUnreadCount(conversationId, userId);
        if (updated > 0) {
            log.info("清空未读数成功: conversationId={}, userId={}", conversationId, userId);
        }
    }
    
    @Override
    public boolean deleteConversation(String conversationId, Long userId) {
        Assert.hasText(conversationId, "conversationId 不能为空");
        Assert.notNull(userId, "userId 不能为空");

        Conversation conversation = conversationMapper.selectOne(
            new QueryWrapper<Conversation>()
                .eq("conversation_id", conversationId)
                .eq("owner_id", userId)
        );
        
        if (conversation != null) {
            conversation.setIsDeleted(1);
            conversationMapper.updateById(conversation);
            log.info("删除会话成功: conversationId={}, userId={}", conversationId, userId);
            return true;
        }
        throw new BusinessException(ErrorCode.NOT_FOUND, "会话");
    }
    
    @Override
    public boolean toggleTop(String conversationId, Long userId, boolean isTop) {
        Assert.hasText(conversationId, "conversationId 不能为空");
        Assert.notNull(userId, "userId 不能为空");

        Conversation conversation = conversationMapper.selectOne(
            new QueryWrapper<Conversation>()
                .eq("conversation_id", conversationId)
                .eq("owner_id", userId)
        );
        
        if (conversation != null) {
            conversation.setIsTop(isTop ? 1 : 0);
            conversationMapper.updateById(conversation);
            log.info("{}置顶会话: conversationId={}, userId={}", 
                isTop ? "设置" : "取消", conversationId, userId);
            return true;
        }
        throw new BusinessException(ErrorCode.NOT_FOUND, "会话");
    }
    
    @Override
    public boolean toggleMute(String conversationId, Long userId, boolean isMute) {
        Assert.hasText(conversationId, "conversationId 不能为空");
        Assert.notNull(userId, "userId 不能为空");

        Conversation conversation = conversationMapper.selectOne(
            new QueryWrapper<Conversation>()
                .eq("conversation_id", conversationId)
                .eq("owner_id", userId)
        );
        
        if (conversation != null) {
            conversation.setIsMute(isMute ? 1 : 0);
            conversationMapper.updateById(conversation);
            log.info("{}免打扰: conversationId={}, userId={}", 
                isMute ? "开启" : "关闭", conversationId, userId);
            return true;
        }
        throw new BusinessException(ErrorCode.NOT_FOUND, "会话");
    }
    
    /**
     * 生成会话ID
     * 单聊：conv_小ID_大ID（保证双方会话ID一致）
     * 群聊：conv_group_群ID
     */
    private String generateConversationId(Integer type, Long userId1, Long userId2) {
        if (type == 0) {
            // 单聊
            long min = Math.min(userId1, userId2);
            long max = Math.max(userId1, userId2);
            return "conv_" + min + "_" + max;
        } else {
            // 群聊（userId2 是群组ID）
            return "conv_group_" + userId2;
        }
    }
    
    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }
}
