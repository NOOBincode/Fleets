package org.example.fleets.group.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.common.constant.LogConstants;
import org.example.fleets.common.exception.BusinessException;
import org.example.fleets.common.exception.ErrorCode;
import org.example.fleets.common.util.Assert;
import org.example.fleets.common.util.PageResult;
import org.example.fleets.group.mapper.GroupMapper;
import org.example.fleets.group.mapper.GroupMemberMapper;
import org.example.fleets.group.model.dto.GroupCreateDTO;
import org.example.fleets.group.model.entity.Group;
import org.example.fleets.group.model.entity.GroupMember;
import org.example.fleets.group.model.vo.GroupVO;
import org.example.fleets.group.service.GroupService;
import org.example.fleets.group.service.cache.GroupCacheService;
import org.example.fleets.user.mapper.UserMapper;
import org.example.fleets.user.model.entity.User;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
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
    public GroupVO createGroup(Long userId, GroupCreateDTO createDTO) {
        log.info(LogConstants.buildLog(LogConstants.MODULE_GROUP, LogConstants.OP_CREATE, 
            "用户创建群组, userId: " + userId + ", groupName: " + createDTO.getGroupName()));
        
        // 1. 参数校验
        Assert.notNull(userId, "用户ID不能为空");
        Assert.notNull(createDTO, "创建参数不能为空");
        if (createDTO.getGroupName() == null || createDTO.getGroupName().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "群组名称不能为空");
        }
        
        // 2. 创建群组
        Group group = new Group();
        group.setGroupName(createDTO.getGroupName());
        group.setAvatar(createDTO.getAvatar());
        group.setDescription(createDTO.getDescription());
        group.setAnnouncement(createDTO.getAnnouncement());
        group.setMaxMembers(createDTO.getMaxMembers() != null ? createDTO.getMaxMembers() : 200);
        group.setMemberCount(1); // 初始只有群主
        group.setOwnerId(userId);
        group.setStatus(0); // 0-正常
        group.setJoinType(createDTO.getJoinType() != null ? createDTO.getJoinType() : 0);
        group.setCreateTime(new Date());
        group.setUpdateTime(new Date());
        
        groupMapper.insert(group);
        
        // 3. 添加群主为成员
        GroupMember ownerMember = new GroupMember();
        ownerMember.setGroupId(group.getId());
        ownerMember.setUserId(userId);
        ownerMember.setRole(2); // 2-群主
        ownerMember.setMuteStatus(0);
        ownerMember.setJoinTime(new Date());
        groupMemberMapper.insert(ownerMember);
        
        // 4. 添加初始成员
        if (createDTO.getMemberIds() != null && !createDTO.getMemberIds().isEmpty()) {
            for (Long memberId : createDTO.getMemberIds()) {
                if (!memberId.equals(userId)) {
                    GroupMember member = new GroupMember();
                    member.setGroupId(group.getId());
                    member.setUserId(memberId);
                    member.setRole(0); // 0-普通成员
                    member.setMuteStatus(0);
                    member.setJoinTime(new Date());
                    groupMemberMapper.insert(member);
                    
                    // 更新成员数
                    group.setMemberCount(group.getMemberCount() + 1);
                }
            }
            groupMapper.updateById(group);
        }
        
        // 5. 缓存群组信息
        groupCacheService.cacheGroupInfo(group);
        
        log.info(LogConstants.buildLog(LogConstants.MODULE_GROUP, LogConstants.OP_CREATE, 
            LogConstants.STATUS_SUCCESS, "群组创建成功, groupId: " + group.getId()));
        
        return convertToVO(group);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean dismissGroup(Long groupId, Long userId) {
        log.info(LogConstants.buildLog(LogConstants.MODULE_GROUP, LogConstants.OP_DELETE, 
            "解散群组, groupId: " + groupId + ", userId: " + userId));
        
        // 1. 参数校验
        Assert.notNull(groupId, "群组ID不能为空");
        Assert.notNull(userId, "用户ID不能为空");
        
        // 2. 查询群组
        Group group = groupMapper.selectById(groupId);
        Assert.notNull(group, ErrorCode.GROUP_NOT_FOUND);
        
        // 3. 权限校验 - 只有群主可以解散
        if (!group.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "只有群主可以解散群组");
        }
        
        // 4. 删除群组(逻辑删除)
        group.setStatus(2); // 2-解散
        group.setUpdateTime(new Date());
        groupMapper.updateById(group);
        
        // 5. 删除所有成员(逻辑删除)
        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId);
        List<GroupMember> members = groupMemberMapper.selectList(wrapper);
        for (GroupMember member : members) {
            groupMemberMapper.deleteById(member.getId());
        }
        
        // 6. 清除缓存
        groupCacheService.deleteGroupInfoCache(groupId);
        groupCacheService.deleteGroupMembersCache(groupId);
        
        log.info(LogConstants.buildLog(LogConstants.MODULE_GROUP, LogConstants.OP_DELETE, 
            LogConstants.STATUS_SUCCESS, "群组解散成功"));
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateGroupInfo(Long groupId, Long userId, GroupCreateDTO updateDTO) {
        log.info(LogConstants.buildLog(LogConstants.MODULE_GROUP, LogConstants.OP_UPDATE, 
            "更新群信息, groupId: " + groupId + ", userId: " + userId));
        
        // 1. 参数校验
        Assert.notNull(groupId, "群组ID不能为空");
        Assert.notNull(userId, "用户ID不能为空");
        
        // 2. 查询群组
        Group group = groupMapper.selectById(groupId);
        Assert.notNull(group, ErrorCode.GROUP_NOT_FOUND);
        
        // 3. 权限校验 - 群主或管理员可以修改
        GroupMember member = getMemberInfo(groupId, userId);
        if (member == null || member.getRole() == 0) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "只有群主或管理员可以修改群信息");
        }
        
        // 4. 更新群信息
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
        
        // 5. 更新缓存
        groupCacheService.cacheGroupInfo(group);
        
        log.info(LogConstants.buildLog(LogConstants.MODULE_GROUP, LogConstants.OP_UPDATE, 
            LogConstants.STATUS_SUCCESS, "群信息更新成功"));
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean joinGroup(Long groupId, Long userId) {
        log.info(LogConstants.buildLog(LogConstants.MODULE_GROUP, "加入", 
            "用户加入群组, groupId: " + groupId + ", userId: " + userId));
        
        // 1. 参数校验
        Assert.notNull(groupId, "群组ID不能为空");
        Assert.notNull(userId, "用户ID不能为空");
        
        // 2. 查询群组
        Group group = groupMapper.selectById(groupId);
        Assert.notNull(group, ErrorCode.GROUP_NOT_FOUND);
        
        // 3. 检查群状态
        if (group.getStatus() == 2) {
            throw new BusinessException(ErrorCode.GROUP_DISMISSED, "群组已解散");
        }
        
        // 4. 检查是否已是成员
        GroupMember existMember = getMemberInfo(groupId, userId);
        if (existMember != null) {
            throw new BusinessException(ErrorCode.ALREADY_IN_GROUP, "已经是群成员");
        }
        
        // 5. 检查人数限制
        if (group.getMemberCount() >= group.getMaxMembers()) {
            throw new BusinessException(ErrorCode.GROUP_FULL, "群组人数已满");
        }
        
        // 6. 添加成员
        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(0); // 0-普通成员
        member.setMuteStatus(0);
        member.setJoinTime(new Date());
        groupMemberMapper.insert(member);
        
        // 7. 更新成员数
        group.setMemberCount(group.getMemberCount() + 1);
        group.setUpdateTime(new Date());
        groupMapper.updateById(group);
        
        // 8. 清除缓存
        groupCacheService.deleteGroupMembersCache(groupId);
        groupCacheService.cacheGroupInfo(group);
        
        log.info(LogConstants.buildLog(LogConstants.MODULE_GROUP, "加入", 
            LogConstants.STATUS_SUCCESS, "加入群组成功"));
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitGroup(Long groupId, Long userId) {
        log.info(LogConstants.buildLog(LogConstants.MODULE_GROUP, "退出", 
            "用户退出群组, groupId: " + groupId + ", userId: " + userId));
        
        // 1. 参数校验
        Assert.notNull(groupId, "群组ID不能为空");
        Assert.notNull(userId, "用户ID不能为空");
        
        // 2. 查询群组
        Group group = groupMapper.selectById(groupId);
        Assert.notNull(group, ErrorCode.GROUP_NOT_FOUND);
        
        // 3. 检查是否是群主
        if (group.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.OWNER_CANNOT_QUIT, "群主不能退出群组,请先转让群主或解散群组");
        }
        
        // 4. 查询成员信息
        GroupMember member = getMemberInfo(groupId, userId);
        Assert.notNull(member, ErrorCode.NOT_GROUP_MEMBER);
        
        // 5. 删除成员
        groupMemberMapper.deleteById(member.getId());
        
        // 6. 更新成员数
        group.setMemberCount(group.getMemberCount() - 1);
        group.setUpdateTime(new Date());
        groupMapper.updateById(group);
        
        // 7. 清除缓存
        groupCacheService.deleteGroupMembersCache(groupId);
        groupCacheService.cacheGroupInfo(group);
        
        log.info(LogConstants.buildLog(LogConstants.MODULE_GROUP, "退出", 
            LogConstants.STATUS_SUCCESS, "退出群组成功"));
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean kickMember(Long groupId, Long userId, Long targetUserId) {
        log.info(LogConstants.buildLog(LogConstants.MODULE_GROUP, "踢出", 
            "踢出成员, groupId: " + groupId + ", userId: " + userId + ", targetUserId: " + targetUserId));
        
        // 1. 参数校验
        Assert.notNull(groupId, "群组ID不能为空");
        Assert.notNull(userId, "用户ID不能为空");
        Assert.notNull(targetUserId, "目标用户ID不能为空");
        
        // 2. 查询群组
        Group group = groupMapper.selectById(groupId);
        Assert.notNull(group, ErrorCode.GROUP_NOT_FOUND);
        
        // 3. 权限校验 - 群主或管理员可以踢人
        GroupMember operator = getMemberInfo(groupId, userId);
        if (operator == null || operator.getRole() == 0) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "只有群主或管理员可以踢出成员");
        }
        
        // 4. 不能踢出群主
        if (group.getOwnerId().equals(targetUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_KICK_OWNER, "不能踢出群主");
        }
        
        // 5. 管理员不能踢管理员
        GroupMember target = getMemberInfo(groupId, targetUserId);
        Assert.notNull(target, ErrorCode.NOT_GROUP_MEMBER);
        if (operator.getRole() == 1 && target.getRole() == 1) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "管理员不能踢出其他管理员");
        }
        
        // 6. 删除成员
        groupMemberMapper.deleteById(target.getId());
        
        // 7. 更新成员数
        group.setMemberCount(group.getMemberCount() - 1);
        group.setUpdateTime(new Date());
        groupMapper.updateById(group);
        
        // 8. 清除缓存
        groupCacheService.deleteGroupMembersCache(groupId);
        groupCacheService.cacheGroupInfo(group);
        
        log.info(LogConstants.buildLog(LogConstants.MODULE_GROUP, "踢出", 
            LogConstants.STATUS_SUCCESS, "踢出成员成功"));
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setAdmin(Long groupId, Long userId, Long targetUserId, boolean isAdmin) {
        log.info(LogConstants.buildLog(LogConstants.MODULE_GROUP, "设置管理员", 
            "groupId: " + groupId + ", userId: " + userId + ", targetUserId: " + targetUserId + ", isAdmin: " + isAdmin));
        
        // 1. 参数校验
        Assert.notNull(groupId, "群组ID不能为空");
        Assert.notNull(userId, "用户ID不能为空");
        Assert.notNull(targetUserId, "目标用户ID不能为空");
        
        // 2. 查询群组
        Group group = groupMapper.selectById(groupId);
        Assert.notNull(group, ErrorCode.GROUP_NOT_FOUND);
        
        // 3. 权限校验 - 只有群主可以设置管理员
        if (!group.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "只有群主可以设置管理员");
        }
        
        // 4. 查询目标成员
        GroupMember target = getMemberInfo(groupId, targetUserId);
        Assert.notNull(target, ErrorCode.NOT_GROUP_MEMBER);
        
        // 5. 更新角色
        target.setRole(isAdmin ? 1 : 0); // 1-管理员, 0-普通成员
        groupMemberMapper.updateById(target);
        
        log.info(LogConstants.buildLog(LogConstants.MODULE_GROUP, "设置管理员", 
            LogConstants.STATUS_SUCCESS, "设置管理员成功"));
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean muteMember(Long groupId, Long userId, Long targetUserId, Long muteMinutes) {
        log.info(LogConstants.buildLog(LogConstants.MODULE_GROUP, "禁言", 
            "groupId: " + groupId + ", userId: " + userId + ", targetUserId: " + targetUserId + ", muteMinutes: " + muteMinutes));
        
        // 1. 参数校验
        Assert.notNull(groupId, "群组ID不能为空");
        Assert.notNull(userId, "用户ID不能为空");
        Assert.notNull(targetUserId, "目标用户ID不能为空");
        
        // 2. 查询群组
        Group group = groupMapper.selectById(groupId);
        Assert.notNull(group, ErrorCode.GROUP_NOT_FOUND);
        
        // 3. 权限校验 - 群主或管理员可以禁言
        GroupMember operator = getMemberInfo(groupId, userId);
        if (operator == null || operator.getRole() == 0) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "只有群主或管理员可以禁言成员");
        }
        
        // 4. 查询目标成员
        GroupMember target = getMemberInfo(groupId, targetUserId);
        Assert.notNull(target, ErrorCode.NOT_GROUP_MEMBER);
        
        // 5. 不能禁言群主
        if (group.getOwnerId().equals(targetUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_MUTE_OWNER, "不能禁言群主");
        }
        
        // 6. 管理员不能禁言管理员
        if (operator.getRole() == 1 && target.getRole() == 1) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "管理员不能禁言其他管理员");
        }
        
        // 7. 设置禁言
        if (muteMinutes > 0) {
            target.setMuteStatus(1);
            target.setMuteEndTime(new Date(System.currentTimeMillis() + muteMinutes * 60 * 1000));
        } else {
            // 取消禁言
            target.setMuteStatus(0);
            target.setMuteEndTime(null);
        }
        groupMemberMapper.updateById(target);
        
        log.info(LogConstants.buildLog(LogConstants.MODULE_GROUP, "禁言", 
            LogConstants.STATUS_SUCCESS, "禁言设置成功"));
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean transferOwner(Long groupId, Long userId, Long newOwnerId) {
        log.info(LogConstants.buildLog(LogConstants.MODULE_GROUP, "转让群主", 
            "groupId: " + groupId + ", userId: " + userId + ", newOwnerId: " + newOwnerId));
        
        // 1. 参数校验
        Assert.notNull(groupId, "群组ID不能为空");
        Assert.notNull(userId, "用户ID不能为空");
        Assert.notNull(newOwnerId, "新群主ID不能为空");
        
        // 2. 查询群组
        Group group = groupMapper.selectById(groupId);
        Assert.notNull(group, ErrorCode.GROUP_NOT_FOUND);
        
        // 3. 权限校验 - 只有群主可以转让
        if (!group.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "只有群主可以转让群主");
        }
        
        // 4. 查询新群主
        GroupMember newOwner = getMemberInfo(groupId, newOwnerId);
        Assert.notNull(newOwner, ErrorCode.NOT_GROUP_MEMBER);
        
        // 5. 查询原群主
        GroupMember oldOwner = getMemberInfo(groupId, userId);
        
        // 6. 更新角色
        newOwner.setRole(2); // 2-群主
        groupMemberMapper.updateById(newOwner);
        
        oldOwner.setRole(0); // 0-普通成员
        groupMemberMapper.updateById(oldOwner);
        
        // 7. 更新群组
        group.setOwnerId(newOwnerId);
        group.setUpdateTime(new Date());
        groupMapper.updateById(group);
        
        // 8. 更新缓存
        groupCacheService.cacheGroupInfo(group);
        
        log.info(LogConstants.buildLog(LogConstants.MODULE_GROUP, "转让群主", 
            LogConstants.STATUS_SUCCESS, "转让群主成功"));
        
        return true;
    }

    @Override
    public GroupVO getGroupInfo(Long groupId) {
        log.info(LogConstants.buildLog(LogConstants.MODULE_GROUP, LogConstants.OP_QUERY, 
            "查询群信息, groupId: " + groupId));
        
        // 1. 参数校验
        Assert.notNull(groupId, "群组ID不能为空");
        
        // 2. 先从缓存获取
        Group group = groupCacheService.getCachedGroupInfo(groupId);
        if (group == null) {
            // 3. 从数据库查询
            group = groupMapper.selectById(groupId);
            Assert.notNull(group, ErrorCode.GROUP_NOT_FOUND);
            
            // 4. 缓存
            groupCacheService.cacheGroupInfo(group);
        }
        
        return convertToVO(group);
    }

    @Override
    public PageResult<GroupVO> getUserGroups(Long userId, Integer pageNum, Integer pageSize) {
        log.info(LogConstants.buildLog(LogConstants.MODULE_GROUP, LogConstants.OP_QUERY, 
            "查询用户群组列表, userId: " + userId));
        
        // 1. 参数校验
        Assert.notNull(userId, "用户ID不能为空");
        
        // 2. 查询用户的群组成员记录
        LambdaQueryWrapper<GroupMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(GroupMember::getUserId, userId);
        List<GroupMember> members = groupMemberMapper.selectList(memberWrapper);
        
        if (members.isEmpty()) {
            return PageResult.empty(pageNum, pageSize);
        }
        
        // 3. 获取群组ID列表
        List<Long> groupIds = members.stream()
            .map(GroupMember::getGroupId)
            .collect(Collectors.toList());
        
        // 4. 分页查询群组信息
        Page<Group> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Group> groupWrapper = new LambdaQueryWrapper<>();
        groupWrapper.in(Group::getId, groupIds);
        groupWrapper.orderByDesc(Group::getCreateTime);
        
        Page<Group> groupPage = groupMapper.selectPage(page, groupWrapper);
        
        // 5. 转换为VO
        List<GroupVO> groupVOs = groupPage.getRecords().stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
        
        return PageResult.of(groupPage.getTotal(), groupVOs, pageNum, pageSize);
    }
    
    @Override
    public List<Long> getGroupMemberIds(Long groupId) {
        log.info(LogConstants.buildLog(LogConstants.MODULE_GROUP, LogConstants.OP_QUERY, 
            "查询群成员ID列表, groupId: " + groupId));
        
        // 1. 参数校验
        Assert.notNull(groupId, "群组ID不能为空");
        
        // 2. 先从缓存获取
        List<Long> memberIds = groupCacheService.getCachedGroupMembers(groupId);
        if (memberIds != null) {
            return memberIds;
        }
        
        // 3. 从数据库查询
        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId);
        wrapper.select(GroupMember::getUserId);
        
        List<GroupMember> members = groupMemberMapper.selectList(wrapper);
        memberIds = members.stream()
            .map(GroupMember::getUserId)
            .collect(Collectors.toList());
        
        // 4. 缓存
        groupCacheService.cacheGroupMembers(groupId, memberIds);
        
        return memberIds;
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
