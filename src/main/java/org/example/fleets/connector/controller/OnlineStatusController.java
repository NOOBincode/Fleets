package org.example.fleets.connector.controller;

import org.example.fleets.common.api.CommonResult;
import org.example.fleets.connector.service.OnlineStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 在线状态控制器
 */
@RestController
@RequestMapping("/api/online")
public class OnlineStatusController {
    
    @Autowired
    private OnlineStatusService onlineStatusService;
    
    /**
     * 检查用户是否在线
     */
    @GetMapping("/check/{userId}")
    public CommonResult<Boolean> checkOnline(@PathVariable Long userId) {
        boolean online = onlineStatusService.isOnline(userId);
        return CommonResult.success(online);
    }
    
    /**
     * 批量检查用户是否在线
     */
    @PostMapping("/check/batch")
    public CommonResult<Map<Long, Boolean>> batchCheckOnline(@RequestBody List<Long> userIds) {
        Map<Long, Boolean> result = onlineStatusService.batchCheckOnline(userIds);
        return CommonResult.success(result);
    }
    
    /**
     * 获取在线用户数量
     */
    @GetMapping("/count")
    public CommonResult<Long> getOnlineUserCount() {
        long count = onlineStatusService.getOnlineUserCount();
        return CommonResult.success(count);
    }
}
