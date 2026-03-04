package org.example.fleets.mailbox.integration;

import org.example.fleets.mailbox.model.dto.SyncMessageDTO;
import org.example.fleets.mailbox.model.vo.SyncResult;
import org.example.fleets.mailbox.model.vo.UnreadCountVO;
import org.example.fleets.mailbox.service.MailboxService;
import org.example.fleets.user.model.dto.UserRegisterDTO;
import org.example.fleets.user.model.vo.UserVO;
import org.example.fleets.user.service.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

/**
 * 信箱模块集成测试
 * 依赖：Redis、MongoDB（test 配置）。测试获取未读数、增量同步（空信箱）等。
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("信箱模块集成测试")
class MailboxIntegrationTest {

    @Autowired
    private MailboxService mailboxService;
    @Autowired
    private UserService userService;

    private static Long testUserId;

    @Test
    @Order(1)
    @DisplayName("集成测试 - 注册用户")
    void testRegisterUser() {
        String username = "mailbox_test_" + System.currentTimeMillis();
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername(username);
        dto.setPassword("Test@123456");
        dto.setNickname("Mailbox Test User");
        dto.setPhone("13900139200");
        dto.setEmail("mailboxtest@example.com");
        UserVO vo = userService.register(dto);
        assertThat(vo).isNotNull();
        testUserId = vo.getId();
    }

    @Test
    @Order(2)
    @DisplayName("集成测试 - 获取未读消息数（新用户无信箱）")
    void testGetUnreadCount_NewUser() {
        Assumptions.assumeTrue(testUserId != null, "需要先注册用户");

        UnreadCountVO vo = mailboxService.getUnreadCount(testUserId);
        assertThat(vo).isNotNull();
        assertThat(vo.getTotalUnread()).isEqualTo(0);
        assertThat(vo.getConversationUnread()).isNotNull();
    }

    @Test
    @Order(3)
    @DisplayName("集成测试 - 增量同步消息（无信箱返回空结果）")
    void testSyncMessages_NoMailbox_ReturnsEmpty() {
        Assumptions.assumeTrue(testUserId != null, "需要先注册用户");

        SyncMessageDTO dto = new SyncMessageDTO();
        dto.setConversationId("conv_1_999");
        dto.setFromSequence(0L);

        SyncResult result = mailboxService.syncMessages(testUserId, dto);
        assertThat(result).isNotNull();
        assertThat(result.getCurrentSequence()).isEqualTo(0L);
        assertThat(result.getMessages()).isEmpty();
        assertThat(result.getHasMore()).isFalse();
    }
}
