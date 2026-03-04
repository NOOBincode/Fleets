package org.example.fleets.mailbox.service.impl;

import org.example.fleets.mailbox.converter.MailboxConverter;
import org.example.fleets.mailbox.model.dto.MarkReadDTO;
import org.example.fleets.mailbox.model.dto.SyncMessageDTO;
import org.example.fleets.mailbox.model.entity.MailboxMessage;
import org.example.fleets.mailbox.model.entity.UserMailbox;
import org.example.fleets.mailbox.model.vo.SyncResult;
import org.example.fleets.mailbox.model.vo.UnreadCountVO;
import org.example.fleets.mailbox.repository.MailboxMessageRepository;
import org.example.fleets.mailbox.repository.UserMailboxRepository;
import org.example.fleets.cache.redis.RedisService;
import org.example.fleets.common.config.properties.FleetsProperties;
import org.example.fleets.mailbox.service.SequenceService;
import org.example.fleets.message.repository.MessageRepository;
import org.example.fleets.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 信箱服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("信箱服务单元测试")
class MailboxServiceImplTest {

    @Mock
    private UserMailboxRepository userMailboxRepository;
    @Mock
    private MailboxMessageRepository mailboxMessageRepository;
    @Mock
    private RedisService redisService;
    @Mock
    private SequenceService sequenceService;
    @Mock
    private MailboxConverter mailboxConverter;
    @Mock
    private UserMapper userMapper;
    @Mock
    private FleetsProperties fleetsProperties;
    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private MailboxServiceImpl mailboxService;

    private static final Long USER_ID = 1L;
    private static final String CONVERSATION_ID = "conv_1_2";

    @BeforeEach
    void setUp() {
        when(fleetsProperties.getRedis()).thenReturn(new FleetsProperties.RedisConfig());
        when(fleetsProperties.getMailbox()).thenReturn(new FleetsProperties.MailboxConfig());
    }

    @Test
    @DisplayName("生成序列号 - 成功")
    void testGenerateSequence_Success() {
        when(redisService.increment(anyString())).thenReturn(1L);

        Long seq = mailboxService.generateSequence(USER_ID, CONVERSATION_ID);

        assertThat(seq).isEqualTo(1L);
        verify(redisService, times(1)).increment(anyString());
    }

    @Test
    @DisplayName("获取未读消息数 - 无缓存从数据库统计")
    void testGetUnreadCount_FromDatabase() {
        when(redisService.get(anyString())).thenReturn(null);
        when(mailboxMessageRepository.countByUserIdAndStatus(USER_ID, 0)).thenReturn(0L);
        when(userMailboxRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

        UnreadCountVO expectedVo = new UnreadCountVO();
        expectedVo.setTotalUnread(0);
        expectedVo.setConversationUnread(Collections.emptyMap());
        when(mailboxConverter.toUnreadCountVO(eq(0L), anyList())).thenReturn(expectedVo);

        UnreadCountVO result = mailboxService.getUnreadCount(USER_ID);

        assertThat(result).isNotNull();
        assertThat(result.getTotalUnread()).isEqualTo(0);
        verify(redisService, times(1)).set(anyString(), any(UnreadCountVO.class), anyInt(), any());
    }

    @Test
    @DisplayName("获取会话未读数 - 成功")
    void testGetConversationUnreadCount_Success() {
        when(mailboxMessageRepository.countByUserIdAndConversationIdAndStatus(USER_ID, CONVERSATION_ID, 0))
            .thenReturn(3L);

        Integer count = mailboxService.getConversationUnreadCount(USER_ID, CONVERSATION_ID);

        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("增量同步消息 - 信箱不存在返回空结果")
    void testSyncMessages_MailboxNotFound_ReturnsEmpty() {
        when(userMailboxRepository.findByUserIdAndConversationId(USER_ID, CONVERSATION_ID))
            .thenReturn(Optional.empty());

        SyncResult empty = new SyncResult();
        empty.setCurrentSequence(0L);
        empty.setMessages(Collections.emptyList());
        empty.setHasMore(false);
        when(mailboxConverter.createEmptySyncResult()).thenReturn(empty);

        SyncMessageDTO dto = new SyncMessageDTO();
        dto.setConversationId(CONVERSATION_ID);
        dto.setFromSequence(0L);

        SyncResult result = mailboxService.syncMessages(USER_ID, dto);

        assertThat(result).isNotNull();
        assertThat(result.getCurrentSequence()).isEqualTo(0L);
        assertThat(result.getMessages()).isEmpty();
        assertThat(result.getHasMore()).isFalse();
    }
}
