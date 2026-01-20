package org.example.fleets.mailbox.converter;

import org.example.fleets.mailbox.model.entity.MailboxMessage;
import org.example.fleets.mailbox.model.entity.UserMailbox;
import org.example.fleets.mailbox.model.vo.SyncResult;
import org.example.fleets.mailbox.model.vo.UnreadCountVO;
import org.example.fleets.message.model.entity.Message;
import org.example.fleets.message.model.vo.MessageVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mailbox转换器
 * 使用MapStruct进行实体转换
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MailboxConverter {
    
    /**
     * Message转MailboxMessage
     * 需要手动设置userId、conversationId、sequence等字段
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "conversationId", ignore = true)
    @Mapping(target = "sequence", ignore = true)
    @Mapping(target = "messageId", source = "id")
    @Mapping(target = "status", constant = "0")
    @Mapping(target = "createTime", expression = "java(new java.util.Date())")
    @Mapping(target = "readTime", ignore = true)
    MailboxMessage toMailboxMessage(Message message);
    
    /**
     * MailboxMessage转MessageVO
     */
    @Mapping(target = "id", source = "messageId")
    @Mapping(target = "senderNickname", ignore = true)
    @Mapping(target = "senderAvatar", ignore = true)
    @Mapping(target = "receiverId", ignore = true)
    @Mapping(target = "groupId", ignore = true)
    @Mapping(target = "extra", ignore = true)
    MessageVO toMessageVO(MailboxMessage mailboxMessage);
    
    /**
     * 批量转换MailboxMessage到MessageVO
     */
    List<MessageVO> toMessageVOList(List<MailboxMessage> mailboxMessages);
    
    /**
     * 创建新的UserMailbox
     */
    default UserMailbox createNewMailbox(Long userId, String conversationId) {
        UserMailbox mailbox = new UserMailbox();
        mailbox.setUserId(userId);
        mailbox.setConversationId(conversationId);
        mailbox.setSequence(0L);
        mailbox.setUnreadCount(0);
        Date now = new Date();
        mailbox.setCreateTime(now);
        mailbox.setUpdateTime(now);
        return mailbox;
    }
    
    /**
     * 创建UnreadCountVO
     */
    default UnreadCountVO toUnreadCountVO(long totalUnread, List<UserMailbox> mailboxes) {
        UnreadCountVO vo = new UnreadCountVO();
        vo.setTotalUnread((int) totalUnread);
        
        Map<String, Integer> conversationUnreadMap = mailboxes.stream()
            .collect(Collectors.toMap(
                UserMailbox::getConversationId,
                UserMailbox::getUnreadCount
            ));
        vo.setConversationUnread(conversationUnreadMap);
        
        return vo;
    }
    
    /**
     * 创建空的SyncResult
     */
    default SyncResult createEmptySyncResult() {
        SyncResult result = new SyncResult();
        result.setCurrentSequence(0L);
        result.setMessages(new ArrayList<>());
        result.setHasMore(false);
        return result;
    }
    
    /**
     * 创建SyncResult
     */
    default SyncResult toSyncResult(Long currentSequence, List<MessageVO> messages, boolean hasMore) {
        SyncResult result = new SyncResult();
        result.setCurrentSequence(currentSequence);
        result.setMessages(messages);
        result.setHasMore(hasMore);
        return result;
    }
}
