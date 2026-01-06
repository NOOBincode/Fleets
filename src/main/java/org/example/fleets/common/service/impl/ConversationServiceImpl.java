package org.example.fleets.common.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.common.mapper.ConversationMapper;
import org.example.fleets.common.model.Conversation;
import org.example.fleets.common.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 会话服务实现类
 */
@Slf4j
@Service
public class ConversationServiceImpl implements ConversationService {
    
    @Autowired
    private ConversationMapper conversationMapper;
    
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
        return false;
    }
    
    @Override
    public boolean toggleTop(String conversationId, Long userId, boolean isTop) {
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
        return false;
    }
    
    @Override
    public boolean toggleMute(String conversationId, Long userId, boolean isMute) {
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
        return false;
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
