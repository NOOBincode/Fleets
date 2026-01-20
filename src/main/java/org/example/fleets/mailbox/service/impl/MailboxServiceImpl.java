package org.example.fleets.mailbox.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.cache.redis.RedisService;
import org.example.fleets.common.config.properties.FleetsProperties;
import org.example.fleets.common.constant.LogConstants;
import org.example.fleets.common.exception.BusinessException;
import org.example.fleets.common.exception.ErrorCode;
import org.example.fleets.common.util.Assert;
import org.example.fleets.common.util.PageResult;
import org.example.fleets.mailbox.converter.MailboxConverter;
import org.example.fleets.mailbox.model.dto.MarkReadDTO;
import org.example.fleets.mailbox.model.dto.SyncMessageDTO;
import org.example.fleets.mailbox.model.entity.MailboxMessage;
import org.example.fleets.mailbox.model.entity.UserMailbox;
import org.example.fleets.mailbox.model.vo.SyncResult;
import org.example.fleets.mailbox.model.vo.UnreadCountVO;
import org.example.fleets.mailbox.repository.MailboxMessageRepository;
import org.example.fleets.mailbox.repository.UserMailboxRepository;
import org.example.fleets.mailbox.service.MailboxService;
import org.example.fleets.mailbox.service.SequenceService;
import org.example.fleets.message.model.entity.Message;
import org.example.fleets.message.model.vo.MessageVO;
import org.example.fleets.message.repository.MessageRepository;
import org.example.fleets.user.mapper.UserMapper;
import org.example.fleets.user.model.entity.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Mailbox服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailboxServiceImpl implements MailboxService {
    
    private final UserMailboxRepository userMailboxRepository;
    private final MailboxMessageRepository mailboxMessageRepository;
    private final RedisService redisService;
    private final SequenceService sequenceService;
    private final MailboxConverter mailboxConverter;
    private final UserMapper userMapper;
    private final FleetsProperties fleetsProperties;

    
    private static final String SEQUENCE_KEY_PREFIX = "mailbox:seq:";
    private final MessageRepository messageRepository;


    @Override
    public boolean writeMessage(Long userId, String conversationId, Message message) {
        // 参数校验
        Assert.notNull(userId, "用户ID不能为空");
        Assert.hasText(conversationId, "会话ID不能为空");
        Assert.notNull(message, "消息不能为空");
        Assert.hasText(message.getId(), "消息ID不能为空");
        
        log.info(LogConstants.buildLog(LogConstants.MODULE_MAILBOX, "写入消息", 
            String.format("userId:%s, conversationId:%s, messageId:%s", userId, conversationId, message.getId())));
        
        try {
            // 1. 获取或创建用户信箱
            UserMailbox userMailbox = userMailboxRepository
                    .findByUserIdAndConversationId(userId, conversationId)
                    .orElseGet(() -> {
                        UserMailbox newMailbox = mailboxConverter.createNewMailbox(userId, conversationId);
                        return userMailboxRepository.save(newMailbox);
                    });
            
            // 2. 生成序列号
            Long sequence = sequenceService.generateSequence(userId, conversationId);
            
            // 3. 创建MailboxMessage
            MailboxMessage mailboxMsg = mailboxConverter.toMailboxMessage(message);
            mailboxMsg.setUserId(userId);
            mailboxMsg.setConversationId(conversationId);
            mailboxMsg.setSequence(sequence);
            mailboxMessageRepository.save(mailboxMsg);
            
            // 4. 更新UserMailbox元数据
            userMailbox.setSequence(sequence);
            userMailbox.setLastMessageId(message.getId());
            userMailbox.setLastMessageTime(message.getSendTime());
            userMailbox.setUnreadCount(userMailbox.getUnreadCount() + 1);
            userMailbox.setUpdateTime(new Date());
            userMailboxRepository.save(userMailbox);
            
            // 5. 清理缓存
            clearUnreadCountCache(userId);
            
            log.info(LogConstants.buildLog(LogConstants.MODULE_MAILBOX, "写入消息", LogConstants.STATUS_SUCCESS,
                String.format("userId:%s, sequence:%s", userId, sequence)));
            return true;
        } catch (Exception e) {
            log.error("写入消息到信箱，userId: {}, conversationId: {}, messageId: {}",
                    userId, conversationId, message.getId());
            return false;
        }
    }
    
    @Override
    public boolean batchWriteMessage(List<Long> userIds, String conversationId, Message message) {
        log.info("批量写入消息到信箱，userIds: {}, conversationId: {}, messageId: {}", 
            userIds.size(), conversationId, message.getId());
        
        // 1. 批量生成序列号
        // 2. 批量创建MailboxMessage
        // 3. 批量更新UserMailbox元数据

        try {
             Map<Long,Long> seqMap = sequenceService.batchGenerateSequence(userIds,conversationId);
             List<MailboxMessage> mailboxMsgs = userIds.stream()
                     .map(userId -> {
                         MailboxMessage mailboxMsg = mailboxConverter.toMailboxMessage(message);
                         mailboxMsg.setUserId(userId);
                         mailboxMsg.setConversationId(conversationId);
                         mailboxMsg.setSequence(seqMap.get(userId));
                         return mailboxMsg;
                     })
                     .collect(Collectors.toList());
             mailboxMessageRepository.saveAll(mailboxMsgs);
            for (Long userId : userIds) {
                updateMailboxMetadata(userId, conversationId, seqMap.get(userId), message);
            }

            return true;

        } catch (Exception e) {
            log.error("批量写入消息失败", e);
            return false;
        }
    }
    
    @Override
    public List<MessageVO> pullOfflineMessages(Long userId, Long lastSequence) {
        log.info("拉取离线消息，userId: {}, lastSequence: {}", userId, lastSequence);
        
        // 1. 查询所有会话的信箱
        // 2. 查询序列号大于lastSequence的消息
        // 3. 转换为MessageVO并返回

        try {
            List<UserMailbox> mailboxes = userMailboxRepository.findByUserId(userId);
            if (mailboxes.isEmpty()){
                log.info("当前用户没有可用的信箱");
                return Collections.emptyList();
            }
            List<MessageVO> result = new ArrayList<>();
            for (UserMailbox mailbox : mailboxes) {
                Pageable pageable = PageRequest.of(0, 100, Sort.by("sequence").ascending());

            }
        }catch (Exception e) {
            log.error("拉取离线信息失败",e);
        }
        return null;
    }
    
    @Override
    public SyncResult syncMessages(Long userId, SyncMessageDTO syncDTO) {
        log.info("增量同步消息，userId: {}, conversationId: {}, fromSequence: {}", 
            userId, syncDTO.getConversationId(), syncDTO.getFromSequence());
        
        // TODO: 实现增量同步消息逻辑
        // 1. 查询该会话的信箱
        // 2. 查询fromSequence之后的所有消息
        // 3. 返回同步结果
        try {
            UserMailbox userMailbox = userMailboxRepository.findByUserIdAndConversationId(userId, syncDTO.getConversationId()).orElse(null);
            if (userMailbox == null) {
                log.error("信箱当前为空");
                return mailboxConverter.createEmptySyncResult();
            }
            Pageable pageable = PageRequest.of(0, 100, Sort.by("sequence").ascending());
            List<MailboxMessage> messages = mailboxMessageRepository
                    .findByUserIdAndConversationIdAndSequenceGreaterThan(
                            userId,
                            syncDTO.getConversationId(),
                            syncDTO.getFromSequence(),
                            pageable
                    );

            // 3. 使用MapStruct转换为MessageVO
            List<MessageVO> messageVOs = mailboxConverter.toMessageVOList(messages);

            // 4. 使用MapStruct创建同步结果
            return mailboxConverter.toSyncResult(
                    userMailbox.getSequence(),
                    messageVOs,
                    messages.size() >= 100
            );
        }catch (Exception e){
            log.error("增量同步消息失败", e);
            return mailboxConverter.createEmptySyncResult();
        }
    }
    
    @Override
    public boolean markAsRead(Long userId, MarkReadDTO markReadDTO) {
        log.info("标记消息已读，userId: {}, conversationId: {}, sequence: {}", 
            userId, markReadDTO.getConversationId(), markReadDTO.getSequence());
        
        // TODO: 实现标记消息已读逻辑
        // 1. 更新MailboxMessage状态
        // 2. 更新UserMailbox未读数
        // 3. 清理缓存
        // 4. 发送已读回执（可选）

        try {
            Optional<MailboxMessage> optional = mailboxMessageRepository
                    .findByUserIdAndConversationIdAndSequence(
                            userId,
                            markReadDTO.getConversationId(),
                            markReadDTO.getSequence()
                    );
            if (!optional.isPresent()){
                return false;
            }
            MailboxMessage message = optional.get();

            // 2. 更新状态
            if (message.getStatus() == 0) { // 只有未读消息才更新
                message.setStatus(1); // 已读
                message.setReadTime(new Date());
                mailboxMessageRepository.save(message);

                // 3. 更新信箱未读数
                decrementUnreadCount(userId, markReadDTO.getConversationId());

                // 4. 清理缓存
                clearUnreadCountCache(userId);
            }

            return true;
        } catch (Exception e) {
            log.error("标记消息已读失败，userId: {}, conversationId: {}, sequence: {}",
                    userId, markReadDTO.getConversationId(), markReadDTO.getSequence());
            return false;
        }
    }
    
    @Override
    public boolean batchMarkAsRead(Long userId, String conversationId, Long toSequence) {
        log.info("批量标记已读，userId: {}, conversationId: {}, toSequence: {}", 
            userId, conversationId, toSequence);
        
        // TODO: 实现批量标记已读逻辑

        return false;
    }

    /**
     * 获取未读消息数
     * MongoDB聚合查询
     */
    @Override
    public UnreadCountVO getUnreadCount(Long userId) {
        log.info("获取未读消息数，userId: {}", userId);

        try {
            // 1. 先查缓存
            String keyPrefix = fleetsProperties.getRedis().getUnreadCountKeyPrefix();
            String cacheKey = keyPrefix + userId;
            Object cached = redisService.get(cacheKey);
            if (cached instanceof UnreadCountVO) {
                return (UnreadCountVO) cached;
            }

            // 2. 统计总未读数
            long totalUnread = mailboxMessageRepository.countByUserIdAndStatus(userId, 0);

            // 3. 查询各会话的未读数
            List<UserMailbox> mailboxes = userMailboxRepository.findByUserId(userId);

            // 4. 使用MapStruct组装结果
            UnreadCountVO vo = mailboxConverter.toUnreadCountVO(totalUnread, mailboxes);

            // 5. 写入缓存
            int cacheMinutes = fleetsProperties.getMailbox().getUnreadCountCacheMinutes();
            redisService.set(cacheKey, vo, cacheMinutes, java.util.concurrent.TimeUnit.MINUTES);

            return vo;

        } catch (Exception e) {
            log.error("获取未读消息数失败", e);
            return new UnreadCountVO();
        }
    }

    /**
     * 获取会话未读数
     */
    @Override
    public Integer getConversationUnreadCount(Long userId, String conversationId) {
        try {
            return (int) mailboxMessageRepository
                    .countByUserIdAndConversationIdAndStatus(userId, conversationId, 0);
        } catch (Exception e) {
            log.error("获取会话未读数失败", e);
            return 0;
        }
    }

    /**
     * 清空会话消息
     */
    @Override
    public boolean clearConversation(Long userId, String conversationId) {
        log.info("清空会话消息，userId: {}, conversationId: {}", userId, conversationId);

        // TODO: 实现清空会话消息逻辑
        // 需要使用MongoTemplate进行批量删除

        return false;
    }

    /**
     * 删除消息
     */
    @Override
    public boolean deleteMessage(Long userId, String conversationId, Long sequence) {
        log.info("删除消息，userId: {}, conversationId: {}, sequence: {}",
                userId, conversationId, sequence);

        try {
            Optional<MailboxMessage> optional = mailboxMessageRepository
                    .findByUserIdAndConversationIdAndSequence(userId, conversationId, sequence);

            if (!optional.isPresent()) {
                return false;
            }

            MailboxMessage message = optional.get();
            message.setStatus(2); // 已删除
            mailboxMessageRepository.save(message);

            return true;

        } catch (Exception e) {
            log.error("删除消息失败", e);
            return false;
        }
    }
    
    @Override
    public Long generateSequence(Long userId, String conversationId) {
        // 使用Redis原子递增生成序列号
        String key = SEQUENCE_KEY_PREFIX + userId + ":" + conversationId;
        Long sequence = redisService.increment(key);
        
        log.debug("生成序列号，userId: {}, conversationId: {}, sequence: {}", 
            userId, conversationId, sequence);
        
        return sequence;
    }

    // ==================== 私有方法 ====================

    /**
     * 更新信箱元数据
     */
    private void updateMailboxMetadata(Long userId, String conversationId, Long sequence, Message message) {
        UserMailbox mailbox = userMailboxRepository
                .findByUserIdAndConversationId(userId, conversationId)
                .orElseGet(() -> mailboxConverter.createNewMailbox(userId, conversationId));

        mailbox.setSequence(sequence);
        mailbox.setLastMessageId(message.getId());
        mailbox.setLastMessageTime(message.getSendTime());
        mailbox.setUnreadCount(mailbox.getUnreadCount() + 1);
        mailbox.setUpdateTime(new Date());

        userMailboxRepository.save(mailbox);
    }

    /**
     * 减少未读数
     */
    private void decrementUnreadCount(Long userId, String conversationId) {
        UserMailbox mailbox = userMailboxRepository
                .findByUserIdAndConversationId(userId, conversationId)
                .orElse(null);

        if (mailbox != null && mailbox.getUnreadCount() > 0) {
            mailbox.setUnreadCount(mailbox.getUnreadCount() - 1);
            mailbox.setUpdateTime(new Date());
            userMailboxRepository.save(mailbox);
        }
    }

    /**
     * 从MySQL批量查询发送者信息并填充到MessageVO
     */
    private void enrichWithSenderInfo(List<MessageVO> messages) {
        if (messages.isEmpty()) {
            return;
        }

        // 提取发送者ID
        List<Long> senderIds = messages.stream()
                .map(MessageVO::getSenderId)
                .distinct()
                .collect(Collectors.toList());

        // 从MySQL批量查询用户信息
        List<User> senders = userMapper.selectBatchIds(senderIds);
        Map<Long, User> senderMap = senders.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 填充发送者信息
        for (MessageVO vo : messages) {
            User sender = senderMap.get(vo.getSenderId());
            if (sender != null) {
                vo.setSenderNickname(sender.getNickname());
                vo.setSenderAvatar(sender.getAvatar());
            }
        }
    }

    /**
     * 清理未读数缓存
     */
    private void clearUnreadCountCache(Long userId) {
        String keyPrefix = fleetsProperties.getRedis().getUnreadCountKeyPrefix();
        String cacheKey = keyPrefix + userId;
        redisService.delete(cacheKey);
    }

}
