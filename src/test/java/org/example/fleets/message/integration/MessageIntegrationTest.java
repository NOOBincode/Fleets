package org.example.fleets.message.integration;

import org.example.fleets.common.util.PageResult;
import org.example.fleets.message.model.dto.MessageSendDTO;
import org.example.fleets.message.model.vo.MessageVO;
import org.example.fleets.message.service.MessageService;
import org.example.fleets.user.model.dto.FriendAddDTO;
import org.example.fleets.user.model.dto.UserRegisterDTO;
import org.example.fleets.user.model.vo.UserVO;
import org.example.fleets.user.service.FriendshipService;
import org.example.fleets.user.service.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

/**
 * 消息模块集成测试
 * 依赖：MySQL、MongoDB、Redis、RocketMQ（test 配置）。流程：注册两用户 → 加好友并接受 → 发单聊消息 → 查历史。
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("消息模块集成测试")
class MessageIntegrationTest {

    @Autowired
    private MessageService messageService;
    @Autowired
    private UserService userService;
    @Autowired
    private FriendshipService friendshipService;

    private static Long senderId;
    private static Long receiverId;
    private static String sentMessageId;

    @Test
    @Order(1)
    @DisplayName("集成测试 - 注册两个用户")
    void testRegisterTwoUsers() {
        String base = "msg_it_" + "2";
        UserRegisterDTO dto1 = new UserRegisterDTO();
        dto1.setUsername(base + "_a");
        dto1.setPassword("Test@123456");
        dto1.setNickname("Sender");
        dto1.setPhone("13900001001");
        dto1.setEmail(base + "_a@example.com");
        UserVO vo1 = userService.register(dto1);
        assertThat(vo1).isNotNull();
        senderId = vo1.getId();

        UserRegisterDTO dto2 = new UserRegisterDTO();
        dto2.setUsername(base + "_b");
        dto2.setPassword("Test@123456");
        dto2.setNickname("Receiver");
        dto2.setPhone("13900001002");
        dto2.setEmail(base + "_b@example.com");
        UserVO vo2 = userService.register(dto2);
        assertThat(vo2).isNotNull();
        receiverId = vo2.getId();
    }

    @Test
    @Order(2)
    @DisplayName("集成测试 - 加好友并接受")
    void testAddAndAcceptFriend() {
        Assumptions.assumeTrue(senderId != null && receiverId != null, "需要先注册用户");

        FriendAddDTO addDTO = new FriendAddDTO();
        addDTO.setFriendId(receiverId);
        friendshipService.addFriend(senderId, addDTO);
        friendshipService.acceptFriendRequest(receiverId, senderId);

        assertThat(friendshipService.isFriend(senderId, receiverId)).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("集成测试 - 发送单聊消息")
    void testSendSingleChatMessage() {
        Assumptions.assumeTrue(senderId != null && receiverId != null, "需要先完成用户与好友准备");

        MessageSendDTO dto = new MessageSendDTO();
        dto.setMessageType(1);
        dto.setContentType(1);
        dto.setReceiverId(receiverId);
        dto.setContent("integration test hello");

        MessageVO vo = messageService.sendMessage(senderId, dto);
        assertThat(vo).isNotNull();
        assertThat(vo.getId()).isNotBlank();
        assertThat(vo.getContent()).isEqualTo("integration test hello");
        assertThat(vo.getSenderId()).isEqualTo(senderId);
        assertThat(vo.getReceiverId()).isEqualTo(receiverId);
        sentMessageId = vo.getId();
    }

    @Test
    @Order(4)
    @DisplayName("集成测试 - 获取单聊历史")
    void testGetChatHistory() {
        Assumptions.assumeTrue(senderId != null && receiverId != null && sentMessageId != null, "需要先发送消息");

        PageResult<MessageVO> page = messageService.getChatHistory(senderId, receiverId, 1, 10);
        assertThat(page).isNotNull();
        assertThat(page.getRecords()).isNotEmpty();
        assertThat(page.getRecords())
            .anyMatch(m -> sentMessageId.equals(m.getId()) && "integration test hello".equals(m.getContent()));
    }

    @Test
    @Order(5)
    @DisplayName("集成测试 - 标记已读")
    void testMarkAsRead() {
        Assumptions.assumeTrue(receiverId != null && sentMessageId != null, "需要先发送消息");

        boolean ok = messageService.markAsRead(sentMessageId, receiverId);
        assertThat(ok).isTrue();
    }
}
