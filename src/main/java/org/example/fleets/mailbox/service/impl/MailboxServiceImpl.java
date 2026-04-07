package org.example.fleets.mailbox.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.common.cache.GenericCacheService;
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
import org.example.fleets.user.mapper.UserMapper;
import org.example.fleets.user.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
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
    private final GenericCacheService genericCacheService;
    private final SequenceService sequenceService;
    private final MailboxConverter mailboxConverter;
    private final UserMapper userMapper;
    private final FleetsProperties fleetsProperties;

    @Override
    public boolean writeMessage(Long userId, String conversationId, Message message) {
        return writeMessage(userId, conversationId, message, true);
    }

    @Override
    public boolean writeMessage(Long userId, String conversationId, Message message, boolean incrementUnread) {
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
            if (incrementUnread) {
                userMailbox.setUnreadCount(userMailbox.getUnreadCount() + 1);
            }
            userMailbox.setUpdateTime(new Date());
            userMailboxRepository.save(userMailbox);
            
            // 5. 清理缓存
            clearUnreadCountCache(userId);
            
            log.info(LogConstants.buildLog(LogConstants.MODULE_MAILBOX, "写入消息", LogConstants.STATUS_SUCCESS,
                String.format("userId:%s, sequence:%s", userId, sequence)));
            return true;
        } catch (Exception e) {
            log.error("写入消息到信箱失败，userId: {}, conversationId: {}, messageId: {}",
                    userId, conversationId, message.getId(), e);
            throw new BusinessException(ErrorCode.MAILBOX_WRITE_FAILED, e);
        }
    }
    
    @Override
    public boolean batchWriteMessage(List<Long> userIds, String conversationId, Message message) {
        return batchWriteMessage(userIds, conversationId, message, true);
    }

    @Override
    public boolean batchWriteMessage(List<Long> userIds, String conversationId, Message message, boolean incrementUnread) {
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
                updateMailboxMetadata(userId, conversationId, seqMap.get(userId), message, incrementUnread);
            }

            return true;

        } catch (Exception e) {
            log.error("批量写入消息失败，conversationId: {}, messageId: {}, userIdsCount: {}",
                    conversationId, message.getId(), userIds.size(), e);
            throw new BusinessException(ErrorCode.MAILBOX_WRITE_FAILED, e);
        }
    }
    
    @Override
    public List<MessageVO> pullOfflineMessages(Long userId, Long lastSequence, Integer limit) {
        log.info("拉取离线消息，userId: {}, lastSequence: {}, limit: {}", userId, lastSequence, limit);

        int cap = (limit != null && limit > 0) ? Math.min(limit, 500) : fleetsProperties.getMailbox().getPullMessageLimit();
        long floor = lastSequence != null ? lastSequence : 0L;

        try {
            List<UserMailbox> mailboxes = userMailboxRepository.findByUserId(userId);
            if (mailboxes.isEmpty()) {
                log.info("当前用户没有可用的信箱");
                return Collections.emptyList();
            }
            mailboxes.sort(Comparator.comparing(
                    m -> m.getLastMessageTime() != null ? m.getLastMessageTime() : new Date(0L),
                    Comparator.reverseOrder()));

            List<MessageVO> acc = new ArrayList<>();
            for (UserMailbox mb : mailboxes) {
                if (acc.size() >= cap) {
                    break;
                }
                int need = cap - acc.size();
                Pageable pageable = PageRequest.of(0, need, Sort.by("sequence").ascending());
                List<MailboxMessage> chunk = mailboxMessageRepository.findByUserIdAndConversationIdAndSequenceGreaterThan(
                        userId, mb.getConversationId(), floor, pageable);
                acc.addAll(mailboxConverter.toMessageVOList(chunk));
            }
            acc.sort(Comparator.comparing(MessageVO::getSendTime, Comparator.nullsLast(Comparator.naturalOrder())));
            enrichWithSenderInfo(acc);
            return acc;
        } catch (Exception e) {
            log.error("拉取离线消息失败，userId: {}, lastSequence: {}", userId, lastSequence, e);
            throw new BusinessException(ErrorCode.MAILBOX_READ_FAILED, e);
        }
    }
    
    @Override
    public SyncResult syncMessages(Long userId, SyncMessageDTO syncDTO) {
        log.info("增量同步消息，userId: {}, conversationId: {}, fromSequence: {}", 
            userId, syncDTO.getConversationId(), syncDTO.getFromSequence());
        try {
            UserMailbox userMailbox = userMailboxRepository.findByUserIdAndConversationId(userId, syncDTO.getConversationId()).orElse(null);
            if (userMailbox == null) {
                log.info("信箱不存在或为空，userId: {}, conversationId: {}", userId, syncDTO.getConversationId());
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
        } catch (Exception e) {
            log.error("增量同步消息失败，userId: {}, conversationId: {}, fromSequence: {}",
                    userId, syncDTO.getConversationId(), syncDTO.getFromSequence(), e);
            throw new BusinessException(ErrorCode.MAILBOX_READ_FAILED, e);
        }
    }
    
    @Override
    public boolean markAsRead(Long userId, MarkReadDTO markReadDTO) {
        log.info("标记消息已读，userId: {}, conversationId: {}, sequence: {}",
                userId, markReadDTO.getConversationId(), markReadDTO.getSequence());

        Assert.notNull(userId, "用户ID不能为空");
        Assert.hasText(markReadDTO.getConversationId(), "会话ID不能为空");
        Assert.notNull(markReadDTO.getSequence(), "序列号不能为空");

        try {
            // 1. 轻量乐观锁：仅当 status=0（未读）时更新为已读，避免重复标记导致未读数多减
            long modified = mailboxMessageRepository.markAsReadIfUnread(
                    userId,
                    markReadDTO.getConversationId(),
                    markReadDTO.getSequence(),
                    new Date()
            );

            if (modified == 0) {
                // 消息不存在或已是已读/删除，幂等直接返回成功
                return true;
            }

            // 2. 只有真正从未读改为已读时，才原子减未读数
            userMailboxRepository.decrementUnreadCountIfPositive(userId, markReadDTO.getConversationId());

            // 3. 清理未读缓存，下次读取会重新统计
            clearUnreadCountCache(userId);

            return true;
        } catch (Exception e) {
            log.error("标记消息已读失败，userId: {}, conversationId: {}, sequence: {}",
                    userId, markReadDTO.getConversationId(), markReadDTO.getSequence(), e);
            throw new BusinessException(ErrorCode.MAILBOX_READ_FAILED, e);
        }
    }
    
    @Override
    public boolean batchMarkAsRead(Long userId, String conversationId, Long toSequence) {
        log.info("批量标记已读，userId: {}, conversationId: {}, toSequence: {}", 
            userId, conversationId, toSequence);
        
        Assert.notNull(userId, "用户ID不能为空");
        Assert.hasText(conversationId, "会话ID不能为空");
        Assert.notNull(toSequence, "序列号不能为空");

        try {
            long modified = mailboxMessageRepository.markAsReadUpToSequenceIfUnread(
                    userId,
                    conversationId,
                    toSequence,
                    new Date()
            );

            if (modified <= 0) {
                return true;
            }

            userMailboxRepository.decrementOrResetUnreadCount(userId, conversationId, modified);

            clearUnreadCountCache(userId);
            return true;
        } catch (Exception e) {
            log.error("批量标记已读失败，userId: {}, conversationId: {}, toSequence: {}",
                    userId, conversationId, toSequence, e);
            throw new BusinessException(ErrorCode.MAILBOX_READ_FAILED, e);
        }
    }

    /**
     * 获取未读消息数
     * MongoDB聚合查询
     */
    @Override
    public UnreadCountVO getUnreadCount(Long userId) {
        log.info("获取未读消息数，userId: {}", userId);

        try {
            String keyPrefix = fleetsProperties.getRedis().getUnreadCountKeyPrefix();
            String cacheKey = keyPrefix + userId;
            UnreadCountVO cached = genericCacheService.get(cacheKey);
            if (cached != null) {
                return cached;
            }

            long totalUnread = mailboxMessageRepository.countByUserIdAndStatus(userId, 0);

            List<UserMailbox> mailboxes = userMailboxRepository.findByUserId(userId);

            UnreadCountVO vo = mailboxConverter.toUnreadCountVO(totalUnread, mailboxes);

            int cacheMinutes = fleetsProperties.getMailbox().getUnreadCountCacheMinutes();
            genericCacheService.set(cacheKey, vo, cacheMinutes, TimeUnit.MINUTES);

            return vo;

        } catch (Exception e) {
            log.error("获取未读消息数失败，userId: {}", userId, e);
            throw new BusinessException(ErrorCode.MAILBOX_READ_FAILED, e);
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
            log.error("获取会话未读数失败，userId: {}, conversationId: {}", userId, conversationId, e);
            throw new BusinessException(ErrorCode.MAILBOX_READ_FAILED, e);
        }
    }

    /**
     * 清空会话消息
     */
    @Override
    public boolean clearConversation(Long userId, String conversationId) {
        log.info("清空会话消息，userId: {}, conversationId: {}", userId, conversationId);

        Assert.notNull(userId, "用户ID不能为空");
        Assert.hasText(conversationId, "会话ID不能为空");

        try {
            mailboxMessageRepository.markAsDeletedByConversation(userId, conversationId, new Date());
            userMailboxRepository.resetUnreadCountToZero(userId, conversationId);
            clearUnreadCountCache(userId);
            return true;
        } catch (Exception e) {
            log.error("清空会话消息失败，userId: {}, conversationId: {}", userId, conversationId, e);
            throw new BusinessException(ErrorCode.MAILBOX_READ_FAILED, e);
        }
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
                log.warn("删除消息失败，消息不存在，userId: {}, conversationId: {}, sequence: {}",
                        userId, conversationId, sequence);
                throw new BusinessException(ErrorCode.MESSAGE_NOT_FOUND);
            }

            MailboxMessage message = optional.get();
            message.setStatus(2); // 已删除
            mailboxMessageRepository.save(message);

            return true;

        } catch (Exception e) {
            log.error("删除消息失败，userId: {}, conversationId: {}, sequence: {}",
                    userId, conversationId, sequence, e);
            throw new BusinessException(ErrorCode.MAILBOX_READ_FAILED, e);
        }
    }

    @Override
    public boolean deleteMessageByMessageId(Long userId, String messageId) {
        Optional<MailboxMessage> opt = mailboxMessageRepository.findByUserIdAndMessageId(userId, messageId);
        if (!opt.isPresent()) {
            throw new BusinessException(ErrorCode.MESSAGE_NOT_FOUND);
        }
        MailboxMessage msg = opt.get();
        return deleteMessage(userId, msg.getConversationId(), msg.getSequence());
    }

    @Override
    public boolean markAsReadByMessageId(Long userId, String messageId) {
        Optional<MailboxMessage> opt = mailboxMessageRepository.findByUserIdAndMessageId(userId, messageId);
        if (!opt.isPresent()) {
            throw new BusinessException(ErrorCode.MESSAGE_NOT_FOUND);
        }
        MailboxMessage msg = opt.get();
        MarkReadDTO dto = new MarkReadDTO();
        dto.setConversationId(msg.getConversationId());
        dto.setSequence(msg.getSequence());
        return markAsRead(userId, dto);
    }

    @Override
    public void recallMessageByMessageId(String messageId) {
        List<MailboxMessage> list = mailboxMessageRepository.findByMessageId(messageId);
        for (MailboxMessage msg : list) {
            msg.setContent("[已撤回]");
            mailboxMessageRepository.save(msg);
        }
        log.info("撤回消息完成，messageId: {}, 更新信箱条数: {}", messageId, list.size());
    }
    
    @Override
    public PageResult<MessageVO> getConversationMessages(Long userId, String conversationId, int pageNum, int pageSize) {
        Assert.notNull(userId, "用户ID不能为空");
        Assert.hasText(conversationId, "会话ID不能为空");
        Pageable pageable = PageRequest.of(Math.max(0, pageNum - 1), Math.max(1, Math.min(pageSize, 100)),
                Sort.by("sequence").descending());
        Page<MailboxMessage> page = mailboxMessageRepository
                .findByUserIdAndConversationIdOrderBySequenceDesc(userId, conversationId, pageable);
        List<MessageVO> list = mailboxConverter.toMessageVOList(page.getContent());
        enrichWithSenderInfo(list);
        return PageResult.of(page.getTotalElements(), list, pageNum, pageSize);
    }

    @Override
    public Long getMaxMailboxSequence(Long userId) {
        List<UserMailbox> list = userMailboxRepository.findByUserId(userId);
        return list.stream()
                .map(UserMailbox::getSequence)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L);
    }

    @Override
    public PageResult<MessageVO> searchMessages(Long userId, String keyword, int pageNum, int pageSize) {
        Assert.notNull(userId, "用户ID不能为空");
        Assert.hasText(keyword, "关键词不能为空");
        int pn = Math.max(1, pageNum);
        int ps = Math.max(1, Math.min(pageSize, 100));
        Pageable pageable = PageRequest.of(pn - 1, ps, Sort.by(Sort.Direction.DESC, "sendTime"));
        Page<MailboxMessage> page = mailboxMessageRepository.searchByUserIdAndKeyword(userId, keyword.trim(), pageable);
        List<MessageVO> list = mailboxConverter.toMessageVOList(page.getContent());
        enrichWithSenderInfo(list);
        return PageResult.of(page.getTotalElements(), list, pn, ps);
    }

    @Override
    public Long generateSequence(Long userId, String conversationId) {
        return sequenceService.generateSequence(userId, conversationId);
    }

    // ==================== 私有方法 ====================

    /**
     * 更新信箱元数据
     */
    private void updateMailboxMetadata(Long userId, String conversationId, Long sequence, Message message, boolean incrementUnread) {
        UserMailbox mailbox = userMailboxRepository
                .findByUserIdAndConversationId(userId, conversationId)
                .orElseGet(() -> mailboxConverter.createNewMailbox(userId, conversationId));

        mailbox.setSequence(sequence);
        mailbox.setLastMessageId(message.getId());
        mailbox.setLastMessageTime(message.getSendTime());
        if (incrementUnread) {
            mailbox.setUnreadCount(mailbox.getUnreadCount() + 1);
        }
        mailbox.setUpdateTime(new Date());

        userMailboxRepository.save(mailbox);
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
        genericCacheService.delete(cacheKey);
    }

}
