package org.example.fleets.group.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.common.aop.annotation.DistributedLock;
import org.example.fleets.common.aop.annotation.OperationLog;
import org.example.fleets.common.exception.BusinessException;
import org.example.fleets.common.exception.ErrorCode;
import org.example.fleets.common.util.Assert;
import org.example.fleets.common.util.PageResult;
import org.example.fleets.group.mapper.GroupMapper;
import org.example.fleets.group.mapper.GroupMemberMapper;
import org.example.fleets.group.model.dto.GroupCreateDTO;
import org.example.fleets.group.model.entity.Group;
import org.example.fleets.group.model.entity.GroupMember;
import org.example.fleets.group.model.vo.GroupMemberItemVO;
import org.example.fleets.group.model.vo.GroupVO;
import org.example.fleets.group.service.GroupService;
import org.example.fleets.group.service.cache.GroupCacheService;
import org.example.fleets.user.mapper.UserMapper;
import org.example.fleets.user.model.entity.User;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 群组服务实现类
 */
@Slf4j
@Service
public class GroupServiceImpl implements GroupService {

    @Autowired
    private GroupMapper groupMapper;
    
    @Autowired
    private GroupMemberMapper groupMemberMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private GroupCacheService groupCacheService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = "群组模块", operation = "创建群组")
    public GroupVO createGroup(Long userId, GroupCreateDTO createDTO) {
        Assert.notNull(userId, "用户ID不能为空");
        Assert.notNull(createDTO, "创建参数不能为空");
        if (createDTO.getGroupName() == null || createDTO.getGroupName().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "群组名称不能为空");
        }
        
        Group group = new Group();
        group.setGroupName(createDTO.getGroupName());
        group.setAvatar(createDTO.getAvatar());
        group.setDescription(createDTO.getDescription());
        group.setAnnouncement(createDTO.getAnnouncement());
        group.setMaxMembers(createDTO.getMaxMembers() != null ? createDTO.getMaxMembers() : 200);
        group.setMemberCount(1);
        group.setOwnerId(userId);
        group.setStatus(0);
        group.setJoinType(createDTO.getJoinType() != null ? createDTO.getJoinType() : 0);
        group.setCreateTime(new Date());
        group.setUpdateTime(new Date());
        
        groupMapper.insert(group);
        
        GroupMember ownerMember = new GroupMember();
        ownerMember.setGroupId(group.getId());
        ownerMember.setUserId(userId);
        ownerMember.setRole(2);
        ownerMember.setMuteStatus(0);
        ownerMember.setJoinTime(new Date());
        groupMemberMapper.insert(ownerMember);
        
        if (createDTO.getMemberIds() != null && !createDTO.getMemberIds().isEmpty()) {
            for (Long memberId : createDTO.getMemberIds()) {
                if (!memberId.equals(userId)) {
                    GroupMember member = new GroupMember();
                    member.setGroupId(group.getId());
                    member.setUserId(memberId);
                    member.setRole(0);
                    member.setMuteStatus(0);
                    member.setJoinTime(new Date());
                    groupMemberMapper.insert(member);
                    
                    group.setMemberCount(group.getMemberCount() + 1);
                }
            }
            groupMapper.updateById(group);
        }
        
        groupCacheService.cacheGroupInfo(group);
        
        return convertToVO(group);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = "群组模块", operation = "解散群组")
    public boolean dismissGroup(Long groupId, Long userId) {
        Assert.notNull(groupId, "群组ID不能为空");
        Assert.notNull(userId, "用户ID不能为空");
        
        Group group = groupMapper.selectById(groupId);
        Assert.notNull(group, ErrorCode.GROUP_NOT_FOUND);
        
        if (!group.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "只有群主可以解散群组");
        }
        
        group.setStatus(2);
        group.setUpdateTime(new Date());
        groupMapper.updateById(group);
        
        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId);
        List<GroupMember> members = groupMemberMapper.selectList(wrapper);
        for (GroupMember member : members) {
            groupMemberMapper.deleteById(member.getId());
        }
        
        groupCacheService.deleteGroupInfoCache(groupId);
        groupCacheService.deleteGroupMembersCache(groupId);
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = "群组模块", operation = "更新群信息")
    public boolean updateGroupInfo(Long groupId, Long userId, GroupCreateDTO updateDTO) {
        Assert.notNull(groupId, "群组ID不能为空");
        Assert.notNull(userId, "用户ID不能为空");
        
        Group group = groupMapper.selectById(groupId);
        Assert.notNull(group, ErrorCode.GROUP_NOT_FOUND);
        
        GroupMember member = getMemberInfo(groupId, userId);
        if (member == null || member.getRole() == 0) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "只有群主或管理员可以修改群信息");
        }
        
        if (updateDTO.getGroupName() != null) {
            group.setGroupName(updateDTO.getGroupName());
        }
        if (updateDTO.getAvatar() != null) {
            group.setAvatar(updateDTO.getAvatar());
        }
        if (updateDTO.getDescription() != null) {
            group.setDescription(updateDTO.getDescription());
        }
        if (updateDTO.getAnnouncement() != null) {
            group.setAnnouncement(updateDTO.getAnnouncement());
        }
        if (updateDTO.getMaxMembers() != null) {
            group.setMaxMembers(updateDTO.getMaxMembers());
        }
        if (updateDTO.getJoinType() != null) {
            group.setJoinType(updateDTO.getJoinType());
        }
        group.setUpdateTime(new Date());
        
        groupMapper.updateById(group);
        
        groupCacheService.cacheGroupInfo(group);
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(key = "'group:join:' + #groupId + ':' + #userId")
    @OperationLog(module = "群组模块", operation = "加入群组")
    public boolean joinGroup(Long groupId, Long userId) {
        Assert.notNull(groupId, "群组ID不能为空");
        Assert.notNull(userId, "用户ID不能为空");
        
        Group group = groupMapper.selectById(groupId);
        Assert.notNull(group, ErrorCode.GROUP_NOT_FOUND);
        
        if (group.getStatus() == 2) {
            throw new BusinessException(ErrorCode.GROUP_DISMISSED, "群组已解散");
        }
        
        GroupMember existMember = getMemberInfo(groupId, userId);
        if (existMember != null) {
            throw new BusinessException(ErrorCode.ALREADY_IN_GROUP, "已经是群成员");
        }
        
        if (group.getMemberCount() >= group.getMaxMembers()) {
            throw new BusinessException(ErrorCode.GROUP_FULL, "群组人数已满");
        }
        
        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(0);
        member.setMuteStatus(0);
        member.setJoinTime(new Date());
        groupMemberMapper.insert(member);
        
        group.setMemberCount(group.getMemberCount() + 1);
        group.setUpdateTime(new Date());
        groupMapper.updateById(group);
        
        groupCacheService.deleteGroupMembersCache(groupId);
        groupCacheService.cacheGroupInfo(group);
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = "群组模块", operation = "退出群组")
    public boolean quitGroup(Long groupId, Long userId) {
        Assert.notNull(groupId, "群组ID不能为空");
        Assert.notNull(userId, "用户ID不能为空");
        
        Group group = groupMapper.selectById(groupId);
        Assert.notNull(group, ErrorCode.GROUP_NOT_FOUND);
        
        if (group.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.OWNER_CANNOT_QUIT, "群主不能退出群组,请先转让群主或解散群组");
        }
        
        GroupMember member = getMemberInfo(groupId, userId);
        Assert.notNull(member, ErrorCode.NOT_GROUP_MEMBER);
        
        groupMemberMapper.deleteById(member.getId());
        
        group.setMemberCount(group.getMemberCount() - 1);
        group.setUpdateTime(new Date());
        groupMapper.updateById(group);
        
        groupCacheService.deleteGroupMembersCache(groupId);
        groupCacheService.cacheGroupInfo(group);
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(key = "'group:kick:' + #groupId + ':' + #targetUserId")
    @OperationLog(module = "群组模块", operation = "踢出成员")
    public boolean kickMember(Long groupId, Long userId, Long targetUserId) {
        Assert.notNull(groupId, "群组ID不能为空");
        Assert.notNull(userId, "用户ID不能为空");
        Assert.notNull(targetUserId, "目标用户ID不能为空");
        
        Group group = groupMapper.selectById(groupId);
        Assert.notNull(group, ErrorCode.GROUP_NOT_FOUND);
        
        GroupMember operator = getMemberInfo(groupId, userId);
        if (operator == null || operator.getRole() == 0) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "只有群主或管理员可以踢出成员");
        }
        
        if (group.getOwnerId().equals(targetUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_KICK_OWNER, "不能踢出群主");
        }
        
        GroupMember target = getMemberInfo(groupId, targetUserId);
        Assert.notNull(target, ErrorCode.NOT_GROUP_MEMBER);
        if (operator.getRole() == 1 && target.getRole() == 1) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "管理员不能踢出其他管理员");
        }
        
        groupMemberMapper.deleteById(target.getId());
        
        group.setMemberCount(group.getMemberCount() - 1);
        group.setUpdateTime(new Date());
        groupMapper.updateById(group);
        
        groupCacheService.deleteGroupMembersCache(groupId);
        groupCacheService.cacheGroupInfo(group);
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(key = "'group:admin:' + #groupId + ':' + #targetUserId")
    @OperationLog(module = "群组模块", operation = "设置管理员")
    public boolean setAdmin(Long groupId, Long userId, Long targetUserId, boolean isAdmin) {
        Assert.notNull(groupId, "群组ID不能为空");
        Assert.notNull(userId, "用户ID不能为空");
        Assert.notNull(targetUserId, "目标用户ID不能为空");
        
        Group group = groupMapper.selectById(groupId);
        Assert.notNull(group, ErrorCode.GROUP_NOT_FOUND);
        
        if (!group.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "只有群主可以设置管理员");
        }
        
        GroupMember target = getMemberInfo(groupId, targetUserId);
        Assert.notNull(target, ErrorCode.NOT_GROUP_MEMBER);
        
        target.setRole(isAdmin ? 1 : 0);
        groupMemberMapper.updateById(target);
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(key = "'group:mute:' + #groupId + ':' + #targetUserId")
    @OperationLog(module = "群组模块", operation = "禁言成员")
    public boolean muteMember(Long groupId, Long userId, Long targetUserId, Long muteMinutes) {
        Assert.notNull(groupId, "群组ID不能为空");
        Assert.notNull(userId, "用户ID不能为空");
        Assert.notNull(targetUserId, "目标用户ID不能为空");
        
        Group group = groupMapper.selectById(groupId);
        Assert.notNull(group, ErrorCode.GROUP_NOT_FOUND);
        
        GroupMember operator = getMemberInfo(groupId, userId);
        if (operator == null || operator.getRole() == 0) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "只有群主或管理员可以禁言成员");
        }
        
        GroupMember target = getMemberInfo(groupId, targetUserId);
        Assert.notNull(target, ErrorCode.NOT_GROUP_MEMBER);
        
        if (group.getOwnerId().equals(targetUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_MUTE_OWNER, "不能禁言群主");
        }
        
        if (operator.getRole() == 1 && target.getRole() == 1) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "管理员不能禁言其他管理员");
        }
        
        if (muteMinutes > 0) {
            target.setMuteStatus(1);
            target.setMuteEndTime(new Date(System.currentTimeMillis() + muteMinutes * 60 * 1000));
        } else {
            target.setMuteStatus(0);
            target.setMuteEndTime(null);
        }
        groupMemberMapper.updateById(target);
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(key = "'group:transfer:' + #groupId")
    @OperationLog(module = "群组模块", operation = "转让群主")
    public boolean transferOwner(Long groupId, Long userId, Long newOwnerId) {
        Assert.notNull(groupId, "群组ID不能为空");
        Assert.notNull(userId, "用户ID不能为空");
        Assert.notNull(newOwnerId, "新群主ID不能为空");
        
        Group group = groupMapper.selectById(groupId);
        Assert.notNull(group, ErrorCode.GROUP_NOT_FOUND);
        
        if (!group.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "只有群主可以转让群主");
        }
        
        GroupMember newOwner = getMemberInfo(groupId, newOwnerId);
        Assert.notNull(newOwner, ErrorCode.NOT_GROUP_MEMBER);
        
        GroupMember oldOwner = getMemberInfo(groupId, userId);
        
        newOwner.setRole(2);
        groupMemberMapper.updateById(newOwner);
        
        oldOwner.setRole(0);
        groupMemberMapper.updateById(oldOwner);
        
        group.setOwnerId(newOwnerId);
        group.setUpdateTime(new Date());
        groupMapper.updateById(group);
        
        groupCacheService.cacheGroupInfo(group);
        
        return true;
    }

    @Override
    public GroupVO getGroupInfo(Long groupId) {
        Assert.notNull(groupId, "群组ID不能为空");
        
        Group group = groupCacheService.getCachedGroupInfo(groupId);
        if (group == null) {
            group = groupMapper.selectById(groupId);
            Assert.notNull(group, ErrorCode.GROUP_NOT_FOUND);
            
            groupCacheService.cacheGroupInfo(group);
        }
        
        return convertToVO(group);
    }

    @Override
    public PageResult<GroupVO> getUserGroups(Long userId, Integer pageNum, Integer pageSize) {
        Assert.notNull(userId, "用户ID不能为空");
        
        LambdaQueryWrapper<GroupMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(GroupMember::getUserId, userId);
        List<GroupMember> members = groupMemberMapper.selectList(memberWrapper);
        
        if (members.isEmpty()) {
            return PageResult.empty(pageNum, pageSize);
        }
        
        List<Long> groupIds = members.stream()
            .map(GroupMember::getGroupId)
            .collect(Collectors.toList());
        
        Page<Group> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Group> groupWrapper = new LambdaQueryWrapper<>();
        groupWrapper.in(Group::getId, groupIds);
        groupWrapper.orderByDesc(Group::getCreateTime);
        
        Page<Group> groupPage = groupMapper.selectPage(page, groupWrapper);
        
        List<GroupVO> groupVOs = groupPage.getRecords().stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
        
        return PageResult.of(groupPage.getTotal(), groupVOs, pageNum, pageSize);
    }
    
    @Override
    public List<Long> getGroupMemberIds(Long groupId) {
        Assert.notNull(groupId, "群组ID不能为空");
        
        List<Long> memberIds = groupCacheService.getCachedGroupMembers(groupId);
        if (memberIds != null) {
            return memberIds;
        }
        
        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId);
        wrapper.select(GroupMember::getUserId);
        
        List<GroupMember> members = groupMemberMapper.selectList(wrapper);
        memberIds = members.stream()
            .map(GroupMember::getUserId)
            .collect(Collectors.toList());
        
        groupCacheService.cacheGroupMembers(groupId, memberIds);
        
        return memberIds;
    }

    @Override
    public List<GroupMemberItemVO> listGroupMemberItems(Long groupId, Long viewerUserId) {
        Assert.notNull(groupId, "群组ID不能为空");
        Assert.notNull(viewerUserId, "用户ID不能为空");
        GroupMember viewer = getMemberInfo(groupId, viewerUserId);
        Assert.notNull(viewer, ErrorCode.NOT_GROUP_MEMBER);

        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId);
        List<GroupMember> members = groupMemberMapper.selectList(wrapper);
        if (members.isEmpty()) {
            return Collections.emptyList();
        }

        members.sort(Comparator.comparing(GroupMember::getRole).reversed());

        List<Long> userIds = members.stream()
                .map(GroupMember::getUserId)
                .distinct()
                .collect(Collectors.toList());
        List<User> users = userIds.isEmpty() ? Collections.emptyList() : userMapper.selectBatchIds(userIds);
        Map<Long, User> userMap = users.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault());

        List<GroupMemberItemVO> result = new ArrayList<>(members.size());
        for (GroupMember m : members) {
            GroupMemberItemVO vo = new GroupMemberItemVO();
            vo.setUserId(m.getUserId());
            vo.setRole(m.getRole());
            if (m.getJoinTime() != null) {
                vo.setJoinTime(fmt.format(m.getJoinTime().toInstant().atZone(ZoneId.systemDefault())));
            }
            User u = userMap.get(m.getUserId());
            if (u != null) {
                if (StringUtils.hasText(m.getGroupNickname())) {
                    vo.setUserName(m.getGroupNickname());
                } else if (StringUtils.hasText(u.getNickname())) {
                    vo.setUserName(u.getNickname());
                } else {
                    vo.setUserName(u.getUsername());
                }
                vo.setUserAvatar(u.getAvatar());
            } else {
                vo.setUserName("用户" + m.getUserId());
            }
            result.add(vo);
        }
        return result;
    }
    
    /**
     * 获取成员信息
     */
    private GroupMember getMemberInfo(Long groupId, Long userId) {
        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId);
        wrapper.eq(GroupMember::getUserId, userId);
        return groupMemberMapper.selectOne(wrapper);
    }
    
    /**
     * 转换为VO
     */
    private GroupVO convertToVO(Group group) {
        GroupVO vo = new GroupVO();
        BeanUtils.copyProperties(group, vo);
        
        // 查询群主昵称
        User owner = userMapper.selectById(group.getOwnerId());
        if (owner != null) {
            vo.setOwnerNickname(owner.getNickname());
        }
        
        return vo;
    }
}
