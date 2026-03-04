package org.example.fleets.group.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.fleets.common.exception.BusinessException;
import org.example.fleets.common.util.PageResult;
import org.example.fleets.group.mapper.GroupMapper;
import org.example.fleets.group.mapper.GroupMemberMapper;
import org.example.fleets.group.model.dto.GroupCreateDTO;
import org.example.fleets.group.model.entity.Group;
import org.example.fleets.group.model.entity.GroupMember;
import org.example.fleets.group.model.vo.GroupVO;
import org.example.fleets.group.service.cache.GroupCacheService;
import org.example.fleets.user.mapper.UserMapper;
import org.example.fleets.user.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 群组服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("群组服务单元测试")
class GroupServiceImplTest {

    @Mock
    private GroupMapper groupMapper;
    @Mock
    private GroupMemberMapper groupMemberMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private GroupCacheService groupCacheService;

    @InjectMocks
    private GroupServiceImpl groupService;

    private static final Long USER_ID = 1L;
    private static final Long GROUP_ID = 100L;
    private Group group;
    private GroupCreateDTO createDTO;

    @BeforeEach
    void setUp() {
        group = new Group();
        group.setId(GROUP_ID);
        group.setGroupName("Test Group");
        group.setOwnerId(USER_ID);
        group.setMemberCount(1);
        group.setMaxMembers(200);
        group.setStatus(0);
        group.setCreateTime(new Date());
        group.setUpdateTime(new Date());

        createDTO = new GroupCreateDTO();
        createDTO.setGroupName("Test Group");
        createDTO.setMaxMembers(200);
    }

    @Test
    @DisplayName("创建群组 - 成功")
    void testCreateGroup_Success() {
        when(groupMapper.insert(any(Group.class))).thenAnswer(inv -> {
            Group g = inv.getArgument(0);
            g.setId(GROUP_ID);
            return 1;
        });
        when(groupMemberMapper.insert(any(GroupMember.class))).thenReturn(1);
        when(userMapper.selectById(USER_ID)).thenReturn(new User());

        GroupVO result = groupService.createGroup(USER_ID, createDTO);

        assertThat(result).isNotNull();
        assertThat(result.getGroupName()).isEqualTo("Test Group");
        verify(groupMapper, times(1)).insert(any(Group.class));
        verify(groupMemberMapper, atLeast(1)).insert(any(GroupMember.class));
        verify(groupCacheService, times(1)).cacheGroupInfo(any(Group.class));
    }

    @Test
    @DisplayName("创建群组 - 群组名称为空抛出异常")
    void testCreateGroup_EmptyName_Throws() {
        createDTO.setGroupName("   ");

        assertThatThrownBy(() -> groupService.createGroup(USER_ID, createDTO))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("群组名称不能为空");
        verify(groupMapper, never()).insert(any());
    }

    @Test
    @DisplayName("获取群信息 - 从缓存获取")
    void testGetGroupInfo_FromCache() {
        when(groupCacheService.getCachedGroupInfo(GROUP_ID)).thenReturn(group);
        when(userMapper.selectById(USER_ID)).thenReturn(new User());

        GroupVO result = groupService.getGroupInfo(GROUP_ID);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(GROUP_ID);
        assertThat(result.getGroupName()).isEqualTo("Test Group");
        verify(groupMapper, never()).selectById(any());
    }

    @Test
    @DisplayName("获取群信息 - 从数据库获取")
    void testGetGroupInfo_FromDatabase() {
        when(groupCacheService.getCachedGroupInfo(GROUP_ID)).thenReturn(null);
        when(groupMapper.selectById(GROUP_ID)).thenReturn(group);
        when(userMapper.selectById(USER_ID)).thenReturn(new User());

        GroupVO result = groupService.getGroupInfo(GROUP_ID);

        assertThat(result).isNotNull();
        assertThat(result.getGroupName()).isEqualTo("Test Group");
        verify(groupCacheService, times(1)).cacheGroupInfo(any(Group.class));
    }

    @Test
    @DisplayName("获取群信息 - 群组不存在抛出异常")
    void testGetGroupInfo_NotFound_Throws() {
        when(groupCacheService.getCachedGroupInfo(GROUP_ID)).thenReturn(null);
        when(groupMapper.selectById(GROUP_ID)).thenReturn(null);

        assertThatThrownBy(() -> groupService.getGroupInfo(GROUP_ID))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("解散群组 - 非群主无权限")
    void testDismissGroup_NotOwner_Throws() {
        group.setOwnerId(999L);
        when(groupMapper.selectById(GROUP_ID)).thenReturn(group);

        assertThatThrownBy(() -> groupService.dismissGroup(GROUP_ID, USER_ID))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("只有群主可以解散群组");
        verify(groupMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("加入群组 - 已是成员抛出异常")
    void testJoinGroup_AlreadyMember_Throws() {
        GroupMember member = new GroupMember();
        when(groupMapper.selectById(GROUP_ID)).thenReturn(group);
        when(groupMemberMapper.selectOne(any())).thenReturn(member);

        assertThatThrownBy(() -> groupService.joinGroup(GROUP_ID, USER_ID))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("已经是群成员");
        verify(groupMemberMapper, never()).insert(any());
    }

    @Test
    @DisplayName("退出群组 - 群主不能退出")
    void testQuitGroup_Owner_Throws() {
        when(groupMapper.selectById(GROUP_ID)).thenReturn(group);
        GroupMember ownerMember = new GroupMember();
        ownerMember.setUserId(USER_ID);
        ownerMember.setRole(2);
        when(groupMemberMapper.selectOne(any())).thenReturn(ownerMember);

        assertThatThrownBy(() -> groupService.quitGroup(GROUP_ID, USER_ID))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("群主不能退出");
    }

    @Test
    @DisplayName("获取用户群组列表 - 无群组返回空分页")
    void testGetUserGroups_Empty() {
        when(groupMemberMapper.selectList(any())).thenReturn(Collections.emptyList());

        PageResult<GroupVO> result = groupService.getUserGroups(USER_ID, 1, 10);

        assertThat(result).isNotNull();
        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getTotal()).isEqualTo(0);
    }
}
