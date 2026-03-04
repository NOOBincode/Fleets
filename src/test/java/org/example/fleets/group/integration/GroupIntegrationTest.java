package org.example.fleets.group.integration;

import org.example.fleets.common.util.PageResult;
import org.example.fleets.group.model.dto.GroupCreateDTO;
import org.example.fleets.group.model.vo.GroupVO;
import org.example.fleets.group.service.GroupService;
import org.example.fleets.user.model.dto.UserLoginDTO;
import org.example.fleets.user.model.dto.UserRegisterDTO;
import org.example.fleets.user.model.vo.UserLoginVO;
import org.example.fleets.user.model.vo.UserVO;
import org.example.fleets.user.service.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * 群组模块集成测试
 * 依赖：先注册/登录得到 userId，再执行群组创建、加入、查询、退出等流程。
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("群组模块集成测试")
class GroupIntegrationTest {

    @Autowired
    private UserService userService;
    @Autowired
    private GroupService groupService;

    private static String testUsername;
    private static Long testUserId;
    private static Long createdGroupId;

    @Test
    @Order(1)
    @DisplayName("集成测试 - 注册用户并创建群组")
    void testCreateGroupFlow() {
        testUsername = "group_test_" + System.currentTimeMillis();
        UserRegisterDTO registerDTO = new UserRegisterDTO();
        registerDTO.setUsername(testUsername);
        registerDTO.setPassword("Test@123456");
        registerDTO.setNickname("Group Test User");
        registerDTO.setPhone("13900139000");
        registerDTO.setEmail("grouptest@example.com");

        UserVO userVO = userService.register(registerDTO);
        assertThat(userVO).isNotNull();
        testUserId = userVO.getId();

        GroupCreateDTO createDTO = new GroupCreateDTO();
        createDTO.setGroupName("集成测试群");
        createDTO.setDescription("用于集成测试");
        createDTO.setMaxMembers(100);

        GroupVO groupVO = groupService.createGroup(testUserId, createDTO);
        assertThat(groupVO).isNotNull();
        assertThat(groupVO.getGroupName()).isEqualTo("集成测试群");
        assertThat(groupVO.getOwnerId()).isEqualTo(testUserId);
        assertThat(groupVO.getMemberCount()).isEqualTo(1);
        createdGroupId = groupVO.getId();
    }

    @Test
    @Order(2)
    @DisplayName("集成测试 - 获取群信息")
    void testGetGroupInfoFlow() {
        Assumptions.assumeTrue(createdGroupId != null, "需要先执行创建群组");

        GroupVO vo = groupService.getGroupInfo(createdGroupId);
        assertThat(vo).isNotNull();
        assertThat(vo.getId()).isEqualTo(createdGroupId);
        assertThat(vo.getGroupName()).isEqualTo("集成测试群");
    }

    @Test
    @Order(3)
    @DisplayName("集成测试 - 获取用户群组列表")
    void testGetUserGroupsFlow() {
        Assumptions.assumeTrue(testUserId != null, "需要先执行创建群组");

        PageResult<GroupVO> page = groupService.getUserGroups(testUserId, 1, 10);
        assertThat(page).isNotNull();
        assertThat(page.getTotal()).isGreaterThanOrEqualTo(1);
        assertThat(page.getRecords()).isNotEmpty();
        boolean hasOurGroup = page.getRecords().stream()
            .anyMatch(g -> g.getId().equals(createdGroupId));
        assertThat(hasOurGroup).isTrue();
    }

    @Test
    @Order(4)
    @DisplayName("集成测试 - 获取群成员ID列表")
    void testGetGroupMemberIdsFlow() {
        Assumptions.assumeTrue(createdGroupId != null, "需要先执行创建群组");

        List<Long> memberIds = groupService.getGroupMemberIds(createdGroupId);
        assertThat(memberIds).isNotEmpty();
        assertThat(memberIds).contains(testUserId);
    }

    @Test
    @Order(5)
    @DisplayName("集成测试 - 第二用户注册、加入群组、退出")
    void testJoinAndQuitGroupFlow() {
        Assumptions.assumeTrue(createdGroupId != null, "需要先执行创建群组");

        String user2Name = "group_join_" + System.currentTimeMillis();
        UserRegisterDTO reg2 = new UserRegisterDTO();
        reg2.setUsername(user2Name);
        reg2.setPassword("Test@123456");
        reg2.setNickname("Join Test User");
        reg2.setPhone("13900139001");
        reg2.setEmail("join@example.com");
        UserVO user2 = userService.register(reg2);
        assertThat(user2).isNotNull();
        Long user2Id = user2.getId();

        boolean joined = groupService.joinGroup(createdGroupId, user2Id);
        assertThat(joined).isTrue();

        List<Long> membersAfterJoin = groupService.getGroupMemberIds(createdGroupId);
        assertThat(membersAfterJoin).contains(user2Id);

        boolean quit = groupService.quitGroup(createdGroupId, user2Id);
        assertThat(quit).isTrue();
        List<Long> membersAfterQuit = groupService.getGroupMemberIds(createdGroupId);
        assertThat(membersAfterQuit).doesNotContain(user2Id);

        try {
            userService.deleteUser(user2Id);
        } catch (Exception ignored) { }
    }

    @Test
    @Order(6)
    @DisplayName("集成测试 - 解散群组")
    void testDismissGroupFlow() {
        Assumptions.assumeTrue(createdGroupId != null && testUserId != null, "需要先执行创建群组");

        boolean dismissed = groupService.dismissGroup(createdGroupId, testUserId);
        assertThat(dismissed).isTrue();

        assertThatThrownBy(() -> groupService.getGroupInfo(createdGroupId))
            .isInstanceOf(Exception.class);

        try {
            userService.deleteUser(testUserId);
        } catch (Exception ignored) { }
    }
}
