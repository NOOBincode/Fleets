package org.example.fleets.group.service;

import org.example.fleets.common.util.PageResult;
import org.example.fleets.group.model.dto.GroupCreateDTO;
import org.example.fleets.group.model.vo.GroupVO;

/**
 * 群组服务接口
 */
public interface GroupService {
    
    /**
     * 创建群组
     */
    GroupVO createGroup(Long userId, GroupCreateDTO createDTO);
    
    /**
     * 解散群组
     */
    boolean dismissGroup(Long groupId, Long userId);
    
    /**
     * 更新群信息
     */
    boolean updateGroupInfo(Long groupId, Long userId, GroupCreateDTO updateDTO);
    
    /**
     * 加入群组
     */
    boolean joinGroup(Long groupId, Long userId);
    
    /**
     * 退出群组
     */
    boolean quitGroup(Long groupId, Long userId);
    
    /**
     * 踢出成员
     */
    boolean kickMember(Long groupId, Long userId, Long targetUserId);
    
    /**
     * 设置管理员
     */
    boolean setAdmin(Long groupId, Long userId, Long targetUserId, boolean isAdmin);
    
    /**
     * 禁言成员
     */
    boolean muteMember(Long groupId, Long userId, Long targetUserId, Long muteMinutes);
    
    /**
     * 转让群主
     */
    boolean transferOwner(Long groupId, Long userId, Long newOwnerId);
    
    /**
     * 获取群信息
     */
    GroupVO getGroupInfo(Long groupId);
    
    /**
     * 获取用户的群组列表
     */
    PageResult<GroupVO> getUserGroups(Long userId, Integer pageNum, Integer pageSize);
    
    /**
     * 获取群成员ID列表
     */
    java.util.List<Long> getGroupMemberIds(Long groupId);
}
