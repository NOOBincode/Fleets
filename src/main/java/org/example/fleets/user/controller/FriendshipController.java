package org.example.fleets.user.controller;

import org.example.fleets.common.api.CommonResult;
import org.example.fleets.common.util.PageResult;
import org.example.fleets.user.model.dto.FriendAddDTO;
import org.example.fleets.user.model.vo.FriendVO;
import org.example.fleets.user.service.FriendshipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 好友关系控制器
 */
@RestController
@RequestMapping("/api/friend")
public class FriendshipController {
    
    @Autowired
    private FriendshipService friendshipService;
    
    /**
     * 添加好友
     */
    @PostMapping("/add")
    public CommonResult<Boolean> addFriend(@RequestBody FriendAddDTO addDTO, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean result = friendshipService.addFriend(userId, addDTO);
        return CommonResult.success(result);
    }
    
    /**
     * 删除好友
     */
    @DeleteMapping("/{friendId}")
    public CommonResult<Boolean> deleteFriend(@PathVariable Long friendId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean result = friendshipService.deleteFriend(userId, friendId);
        return CommonResult.success(result);
    }
    
    /**
     * 拉黑好友
     */
    @PostMapping("/block/{friendId}")
    public CommonResult<Boolean> blockFriend(@PathVariable Long friendId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean result = friendshipService.blockFriend(userId, friendId);
        return CommonResult.success(result);
    }
    
    /**
     * 取消拉黑
     */
    @PostMapping("/unblock/{friendId}")
    public CommonResult<Boolean> unblockFriend(@PathVariable Long friendId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean result = friendshipService.unblockFriend(userId, friendId);
        return CommonResult.success(result);
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
        return CommonResult.success(result);
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
}
