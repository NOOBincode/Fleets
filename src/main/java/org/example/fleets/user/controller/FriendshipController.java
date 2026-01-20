package org.example.fleets.user.controller;

import org.example.fleets.common.api.CommonResult;
import org.example.fleets.common.util.PageResult;
import org.example.fleets.user.model.dto.FriendAddDTO;
import org.example.fleets.user.model.vo.FriendApplyVO;
import org.example.fleets.user.model.vo.FriendVO;
import org.example.fleets.user.model.vo.GroupingFriendVO;
import org.example.fleets.user.model.vo.GroupingVO;
import org.example.fleets.user.service.FriendshipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 好友关系控制器
 */
@RestController
@RequestMapping("/api/friendship")
public class FriendshipController {
    
    @Autowired
    private FriendshipService friendshipService;
    
    /**
     * 添加好友（发送好友请求）
     */
    @PostMapping("/add")
    public CommonResult<Boolean> addFriend(@RequestBody FriendAddDTO addDTO, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean result = friendshipService.addFriend(userId, addDTO);
        return CommonResult.success(result, "好友请求已发送");
    }
    
    /**
     * 接受好友请求
     */
    @PostMapping("/accept/{friendId}")
    public CommonResult<Boolean> acceptFriendRequest(@PathVariable Long friendId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean result = friendshipService.acceptFriendRequest(userId, friendId);
        return CommonResult.success(result, "已接受好友请求");
    }
    
    /**
     * 拒绝好友请求
     */
    @PostMapping("/reject/{friendId}")
    public CommonResult<Boolean> rejectFriendRequest(@PathVariable Long friendId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean result = friendshipService.rejectFriendRequest(userId, friendId);
        return CommonResult.success(result, "已拒绝好友请求");
    }
    
    /**
     * 获取待处理的好友请求列表
     */
    @GetMapping("/requests")
    public CommonResult<List<FriendApplyVO>> getPendingFriendRequests(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<FriendApplyVO> result = friendshipService.getPendingFriendRequests(userId);
        return CommonResult.success(result);
    }
    
    /**
     * 获取待处理的好友请求数量
     */
    @GetMapping("/requests/count")
    public CommonResult<Integer> getPendingRequestCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Integer count = friendshipService.getPendingRequestCount(userId);
        return CommonResult.success(count);
    }
    
    /**
     * 删除好友
     */
    @DeleteMapping("/{friendId}")
    public CommonResult<Boolean> deleteFriend(@PathVariable Long friendId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean result = friendshipService.deleteFriend(userId, friendId);
        return CommonResult.success(result, "已删除好友");
    }
    
    /**
     * 拉黑好友
     */
    @PostMapping("/block/{friendId}")
    public CommonResult<Boolean> blockFriend(@PathVariable Long friendId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean result = friendshipService.blockFriend(userId, friendId);
        return CommonResult.success(result, "已拉黑该好友");
    }
    
    /**
     * 取消拉黑
     */
    @PostMapping("/unblock/{friendId}")
    public CommonResult<Boolean> unblockFriend(@PathVariable Long friendId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean result = friendshipService.unblockFriend(userId, friendId);
        return CommonResult.success(result, "已取消拉黑");
    }
    
    /**
     * 更新好友备注
     */
    @PutMapping("/{friendId}/remark")
    public CommonResult<Boolean> updateRemark(
            @PathVariable Long friendId,
            @RequestParam String remark,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean result = friendshipService.updateRemark(userId, friendId, remark);
        return CommonResult.success(result, "备注已更新");
    }
    
    /**
     * 更新好友分组
     */
    @PutMapping("/{friendId}/group")
    public CommonResult<Boolean> updateGroup(
            @PathVariable Long friendId,
            @RequestParam String groupName,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean result = friendshipService.updateGroup(userId, friendId, groupName);
        return CommonResult.success(result, "分组已更新");
    }
    
    /**
     * 获取好友列表
     */
    @GetMapping("/list")
    public CommonResult<List<FriendVO>> getFriendList(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<FriendVO> result = friendshipService.getFriendList(userId);
        return CommonResult.success(result);
    }
    
    /**
     * 搜索好友
     */
    @GetMapping("/search")
    public CommonResult<PageResult<FriendVO>> searchFriend(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        PageResult<FriendVO> result = friendshipService.searchFriend(userId, keyword, pageNum, pageSize);
        return CommonResult.success(result);
    }
    
    /**
     * 按分组获取好友列表
     */
    @GetMapping("/list/grouped")
    public CommonResult<List<GroupingFriendVO>> getGroupedFriendList(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<GroupingFriendVO> result = friendshipService.getGroupedFriendList(userId);
        return CommonResult.success(result);
    }
    
    /**
     * 获取用户的所有分组
     */
    @GetMapping("/groups")
    public CommonResult<List<GroupingVO>> getUserGroups(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<GroupingVO> result = friendshipService.getUserGroups(userId);
        return CommonResult.success(result);
    }
}
