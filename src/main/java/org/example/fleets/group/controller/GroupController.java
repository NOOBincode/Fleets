package org.example.fleets.group.controller;

import lombok.RequiredArgsConstructor;
import cn.dev33.satoken.stp.StpUtil;
import org.example.fleets.common.api.CommonResult;
import org.example.fleets.common.util.PageResult;
import org.example.fleets.group.model.dto.GroupAdminDTO;
import org.example.fleets.group.model.dto.GroupCreateDTO;
import org.example.fleets.group.model.dto.GroupMuteDTO;
import org.example.fleets.group.model.dto.GroupTransferDTO;
import org.example.fleets.group.model.vo.GroupMemberItemVO;
import org.example.fleets.group.model.vo.GroupVO;
import org.example.fleets.group.service.GroupService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 群组控制器
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/group")
public class GroupController {
    
    private final GroupService groupService;
    
    /**
     * 创建群组
     */
    @PostMapping("/create")
    public CommonResult<GroupVO> createGroup(@RequestBody GroupCreateDTO createDTO) {
        Long userId = StpUtil.getLoginIdAsLong();
        GroupVO groupVO = groupService.createGroup(userId, createDTO);
        return CommonResult.success(groupVO);
    }
    
    /**
     * 解散群组
     */
    @DeleteMapping("/{groupId}")
    public CommonResult<Boolean> dismissGroup(@PathVariable Long groupId) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = groupService.dismissGroup(groupId, userId);
        return CommonResult.success(result);
    }
    
    /**
     * 更新群信息
     */
    @PutMapping("/{groupId}")
    public CommonResult<Boolean> updateGroupInfo(
            @PathVariable Long groupId,
            @RequestBody GroupCreateDTO updateDTO) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = groupService.updateGroupInfo(groupId, userId, updateDTO);
        return CommonResult.success(result);
    }
    
    /**
     * 加入群组
     */
    @PostMapping("/{groupId}/join")
    public CommonResult<Boolean> joinGroup(@PathVariable Long groupId) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = groupService.joinGroup(groupId, userId);
        return CommonResult.success(result);
    }
    
    /**
     * 退出群组
     */
    @PostMapping("/{groupId}/quit")
    public CommonResult<Boolean> quitGroup(@PathVariable Long groupId) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = groupService.quitGroup(groupId, userId);
        return CommonResult.success(result);
    }
    
    /**
     * 踢出成员
     */
    @PostMapping("/{groupId}/kick/{targetUserId}")
    public CommonResult<Boolean> kickMember(
            @PathVariable Long groupId,
            @PathVariable Long targetUserId) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = groupService.kickMember(groupId, userId, targetUserId);
        return CommonResult.success(result);
    }
    
    /**
     * 设置/取消管理员
     */
    @PostMapping("/{groupId}/admin")
    public CommonResult<Boolean> setAdmin(
            @PathVariable Long groupId,
            @RequestBody @Validated GroupAdminDTO adminDTO) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = groupService.setAdmin(groupId, userId, adminDTO.getTargetUserId(), adminDTO.getIsAdmin());
        return CommonResult.success(result);
    }
    
    /**
     * 禁言成员
     */
    @PostMapping("/{groupId}/mute")
    public CommonResult<Boolean> muteMember(
            @PathVariable Long groupId,
            @RequestBody @Validated GroupMuteDTO muteDTO) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = groupService.muteMember(groupId, userId, muteDTO.getTargetUserId(), muteDTO.getMuteMinutes());
        return CommonResult.success(result);
    }
    
    /**
     * 转让群主
     */
    @PostMapping("/{groupId}/transfer")
    public CommonResult<Boolean> transferOwner(
            @PathVariable Long groupId,
            @RequestBody @Validated GroupTransferDTO transferDTO) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = groupService.transferOwner(groupId, userId, transferDTO.getNewOwnerId());
        return CommonResult.success(result);
    }
    
    /**
     * 获取群信息
     */
    @GetMapping("/{groupId}")
    public CommonResult<GroupVO> getGroupInfo(@PathVariable Long groupId) {
        GroupVO groupVO = groupService.getGroupInfo(groupId);
        return CommonResult.success(groupVO);
    }
    
    /**
     * 获取用户的群组列表
     */
    @GetMapping("/list")
    public CommonResult<PageResult<GroupVO>> getUserGroups(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        PageResult<GroupVO> result = groupService.getUserGroups(userId, pageNum, pageSize);
        return CommonResult.success(result);
    }

    /**
     * 获取群成员ID列表
     */
    @GetMapping("/{groupId}/members")
    public CommonResult<List<Long>> getGroupMemberIds(@PathVariable Long groupId) {
        List<Long> memberIds = groupService.getGroupMemberIds(groupId);
        return CommonResult.success(memberIds);
    }

    /**
     * 获取群成员列表（头像、显示名等），仅群成员可访问
     */
    @GetMapping("/{groupId}/members/detail")
    public CommonResult<List<GroupMemberItemVO>> listGroupMemberItems(@PathVariable Long groupId) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<GroupMemberItemVO> list = groupService.listGroupMemberItems(groupId, userId);
        return CommonResult.success(list);
    }
}
