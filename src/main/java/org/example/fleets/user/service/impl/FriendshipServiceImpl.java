package org.example.fleets.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.common.aop.annotation.DistributedLock;
import org.example.fleets.common.aop.annotation.OperationLog;
import org.example.fleets.common.cache.GenericCacheService;
import org.example.fleets.common.exception.BusinessException;
import org.example.fleets.common.exception.ErrorCode;
import org.example.fleets.common.util.PageResult;
import org.example.fleets.user.mapper.FriendshipMapper;
import org.example.fleets.user.mapper.UserMapper;
import org.example.fleets.user.model.dto.FriendAddDTO;
import org.example.fleets.user.model.entity.Friendship;
import org.example.fleets.user.model.entity.User;
import org.example.fleets.user.model.vo.FriendApplyVO;
import org.example.fleets.user.model.vo.FriendVO;
import org.example.fleets.user.model.vo.GroupingFriendVO;
import org.example.fleets.message.model.dto.NotificationDTO;
import org.example.fleets.message.producer.NotificationPublisher;
import org.example.fleets.user.service.FriendshipService;
import org.example.fleets.user.service.cache.FriendshipCacheService;
import org.example.fleets.user.service.support.FriendRequestRateLimiter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 好友关系服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendshipServiceImpl implements FriendshipService {

    private final FriendshipMapper friendshipMapper;
    private final UserMapper userMapper;
    private final FriendshipCacheService friendshipCacheService;
    private final GenericCacheService genericCacheService;
    private final FriendRequestRateLimiter friendRequestRateLimiter;
    private final NotificationPublisher notificationPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(key = "'friend:add:' + T(java.lang.Math).min(#userId, #addDTO.friendId) + ':' + T(java.lang.Math).max(#userId, #addDTO.friendId)")
    @OperationLog(module = "好友模块", operation = "添加好友")
    public boolean addFriend(Long userId, FriendAddDTO addDTO) {
        if (userId == null || addDTO.getFriendId() == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "参数不能为空");
        }
        
        if (userId.equals(addDTO.getFriendId())) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "不能添加自己为好友");
        }
        
        friendRequestRateLimiter.checkApplyAllowed(userId, addDTO.getFriendId());

        User friendUser = userMapper.selectById(addDTO.getFriendId());
        if (friendUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "对方用户不存在");
        }
        
        if (friendUser.getStatus() != 1) {
            throw new BusinessException(ErrorCode.USER_DISABLED, "对方账号已被禁用");
        }
        
        if (isFriend(userId, addDTO.getFriendId())) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "已经是好友关系");
        }
        
        Friendship blockCheck = getFriendship(addDTO.getFriendId(), userId);
        if (blockCheck != null && blockCheck.getStatus() == 3) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "无法添加该用户");
        }
        
        Date now = new Date();
        
        Friendship senderFriendship = createFriendship(
            userId, addDTO.getFriendId(), addDTO.getRemark(), 0, now, null, addDTO.getVerifyMessage(), null);

        Friendship receiverFriendship = createFriendship(
            addDTO.getFriendId(), userId, null, 0, now, null, addDTO.getVerifyMessage(), null);
        
        int result1 = friendshipMapper.insert(senderFriendship);
        int result2 = friendshipMapper.insert(receiverFriendship);
        
        if (result1 <= 0 || result2 <= 0) {
            throw new BusinessException(ErrorCode.FAILED, "添加好友失败");
        }

        friendRequestRateLimiter.recordApplyAction(userId, addDTO.getFriendId());

        Long smallerId = Math.min(userId, addDTO.getFriendId());
        Long largerId = Math.max(userId, addDTO.getFriendId());
        publishFriendApplyNotification(userId, addDTO.getFriendId(), addDTO.getVerifyMessage(), smallerId, largerId);

        friendshipCacheService.deleteFriendListCache(userId);
        friendshipCacheService.deleteFriendListCache(addDTO.getFriendId());
        friendshipCacheService.deleteFriendRelationCache(userId, addDTO.getFriendId());
        friendshipCacheService.deleteFriendRelationCache(addDTO.getFriendId(), userId);
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = "好友模块", operation = "删除好友")
    public boolean deleteFriend(Long userId, Long friendId) {
        if (userId == null || friendId == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "参数不能为空");
        }
        
        LambdaQueryWrapper<Friendship> wrapper1 = new LambdaQueryWrapper<>();
        wrapper1.eq(Friendship::getUserId, userId).eq(Friendship::getFriendId, friendId);
        
        LambdaQueryWrapper<Friendship> wrapper2 = new LambdaQueryWrapper<>();
        wrapper2.eq(Friendship::getUserId, friendId).eq(Friendship::getFriendId, userId);
        
        int result1 = friendshipMapper.delete(wrapper1);
        int result2 = friendshipMapper.delete(wrapper2);
        
        if (result1 <= 0 && result2 <= 0) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "好友关系不存在");
        }
        
        friendshipCacheService.deleteFriendListCache(userId);
        friendshipCacheService.deleteFriendListCache(friendId);
        friendshipCacheService.deleteFriendRelationCache(userId, friendId);
        friendshipCacheService.deleteFriendRelationCache(friendId, userId);
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = "好友模块", operation = "拉黑好友")
    public boolean blockFriend(Long userId, Long friendId) {
        if (userId == null || friendId == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "参数不能为空");
        }
        
        Friendship friendship = getFriendship(userId, friendId);
        if (friendship == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "好友关系不存在");
        }
        
        if (friendship.getStatus() == 3) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "已经拉黑该好友");
        }
        
        friendship.setStatus(3);
        friendship.setUpdateTime(new Date());
        
        int result = friendshipMapper.updateById(friendship);
        if (result <= 0) {
            throw new BusinessException(ErrorCode.FAILED, "拉黑好友失败");
        }
        
        friendshipCacheService.deleteFriendListCache(userId);
        friendshipCacheService.deleteFriendRelationCache(userId, friendId);
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = "好友模块", operation = "取消拉黑")
    public boolean unblockFriend(Long userId, Long friendId) {
        if (userId == null || friendId == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "参数不能为空");
        }
        
        Friendship friendship = getFriendship(userId, friendId);
        if (friendship == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "好友关系不存在");
        }
        
        if (friendship.getStatus() != 3) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "该好友未被拉黑");
        }
        
        friendship.setStatus(1);
        friendship.setUpdateTime(new Date());
        
        int result = friendshipMapper.updateById(friendship);
        if (result <= 0) {
            throw new BusinessException(ErrorCode.FAILED, "取消拉黑失败");
        }
        
        friendshipCacheService.deleteFriendListCache(userId);
        friendshipCacheService.deleteFriendRelationCache(userId, friendId);
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = "好友模块", operation = "更新好友备注")
    public boolean updateRemark(Long userId, Long friendId, String remark) {
        if (userId == null || friendId == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "参数不能为空");
        }
        
        if (StringUtils.hasText(remark) && remark.length() > 50) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "备注长度不能超过50");
        }
        
        Friendship friendship = getFriendship(userId, friendId);
        if (friendship == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "好友关系不存在");
        }
        
        friendship.setRemark(remark);
        friendship.setUpdateTime(new Date());
        
        int result = friendshipMapper.updateById(friendship);
        if (result <= 0) {
            throw new BusinessException(ErrorCode.FAILED, "更新备注失败");
        }
        
        friendshipCacheService.deleteFriendListCache(userId);
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = "好友模块", operation = "更新好友分组")
    public boolean updateGroup(Long userId, Long friendId, String groupName) {
        if (userId == null || friendId == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "参数不能为空");
        }
        
        if (StringUtils.hasText(groupName) && groupName.length() > 20) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "分组名称长度不能超过20");
        }
        
        Friendship friendship = getFriendship(userId, friendId);
        if (friendship == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "好友关系不存在");
        }

        friendship.setUpdateTime(new Date());
        
        int result = friendshipMapper.updateById(friendship);
        if (result <= 0) {
            throw new BusinessException(ErrorCode.FAILED, "更新分组失败");
        }
        
        friendshipCacheService.deleteFriendListCache(userId);
        
        return true;
    }

    /**
     * 获取好友列表
     * 高可用设计：缓存优先，缓存失效时查询数据库并重建缓存
     */
    @Override
    public List<FriendVO> getFriendList(Long userId) {
        log.info("获取好友列表，userId: {}", userId);
        
        if (userId == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "用户ID不能为空");
        }
        
        try {
            // 查询好友关系
            LambdaQueryWrapper<Friendship> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Friendship::getUserId, userId)
                   .eq(Friendship::getStatus, 1) // 只查询已确认的好友
                   .orderByDesc(Friendship::getCreateTime);
            
            List<Friendship> friendships = friendshipMapper.selectList(wrapper);
            
            if (friendships == null || friendships.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 批量查询好友用户信息
            List<Long> friendIds = friendships.stream()
                .map(Friendship::getFriendId)
                .collect(Collectors.toList());
            
            List<User> friendUsers = userMapper.selectBatchIds(friendIds);
            Map<Long, User> userMap = friendUsers.stream()
                .collect(Collectors.toMap(User::getId, u -> u));
            
            // 组装VO
            List<FriendVO> friendVOList = friendships.stream()
                .map(friendship -> {
                    FriendVO vo = new FriendVO();
                    vo.setId(friendship.getId());
                    vo.setUserId(friendship.getUserId());
                    vo.setFriendId(friendship.getFriendId());
                    vo.setRemark(friendship.getRemark());
                    vo.setStatus(friendship.getStatus());
                    vo.setCreateTime(friendship.getCreateTime());
                    
                    // 填充好友用户信息
                    User friendUser = userMap.get(friendship.getFriendId());
                    if (friendUser != null) {
                        vo.setFriendUsername(friendUser.getUsername());
                        vo.setFriendNickname(friendUser.getNickname());
                        vo.setFriendAvatar(friendUser.getAvatar());
                    }
                    
                    // TODO: 查询在线状态（需要在线状态服务支持）
                    vo.setIsOnline(false);
                    
                    return vo;
                })
                .collect(Collectors.toList());
            
            // 缓存好友ID列表
            friendshipCacheService.cacheFriendList(userId, friendIds);
            
            return friendVOList;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取好友列表异常，userId: {}", userId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取好友列表失败");
        }
    }

    /**
     * 搜索好友
     */
    @Override
    public PageResult<FriendVO> searchFriend(Long userId, String keyword, Integer pageNum, Integer pageSize) {
        log.info("搜索好友，userId: {}, keyword: {}", userId, keyword);
        
        if (userId == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "用户ID不能为空");
        }
        
        // 参数校验
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        if (pageSize > 100) {
            pageSize = 100;
        }
        
        try {
            // 先查询好友关系
            LambdaQueryWrapper<Friendship> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Friendship::getUserId, userId)
                   .eq(Friendship::getStatus, 1);
            
            List<Friendship> friendships = friendshipMapper.selectList(wrapper);
            
            if (friendships == null || friendships.isEmpty()) {
                return PageResult.of(0L, new ArrayList<>(), pageNum, pageSize);
            }
            
            List<Long> friendIds = friendships.stream()
                .map(Friendship::getFriendId)
                .collect(Collectors.toList());
            
            // 构建用户查询条件（在好友ID范围内搜索）
            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.in(User::getId, friendIds);
            
            if (StringUtils.hasText(keyword)) {
                userWrapper.and(w -> w
                    .like(User::getUsername, keyword)
                    .or()
                    .like(User::getNickname, keyword)
                );
            }
            
            // 分页查询
            Page<User> page = new Page<>(pageNum, pageSize);
            Page<User> resultPage = userMapper.selectPage(page, userWrapper);
            
            // 组装VO
            Map<Long, Friendship> friendshipMap = friendships.stream()
                .collect(Collectors.toMap(Friendship::getFriendId, f -> f));
            
            List<FriendVO> friendVOList = resultPage.getRecords().stream()
                .map(user -> {
                    Friendship friendship = friendshipMap.get(user.getId());
                    
                    FriendVO vo = new FriendVO();
                    vo.setId(friendship.getId());
                    vo.setUserId(friendship.getUserId());
                    vo.setFriendId(user.getId());
                    vo.setFriendUsername(user.getUsername());
                    vo.setFriendNickname(user.getNickname());
                    vo.setFriendAvatar(user.getAvatar());
                    vo.setRemark(friendship.getRemark());
                    vo.setStatus(friendship.getStatus());
                    vo.setCreateTime(friendship.getCreateTime());
                    vo.setIsOnline(false); // TODO: 查询在线状态
                    
                    return vo;
                })
                .collect(Collectors.toList());
            
            return PageResult.of(resultPage.getTotal(), friendVOList, pageNum, pageSize);
            
        } catch (Exception e) {
            log.error("搜索好友异常，userId: {}, keyword: {}", userId, keyword, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "搜索好友失败");
        }
    }

    /**
     * 检查是否是好友
     * 高可用设计：优先查缓存，缓存未命中时查数据库
     */
    @Override
    public boolean isFriend(Long userId, Long friendId) {
        if (userId == null || friendId == null) {
            return false;
        }
        
        try {
            // 先查缓存
            Boolean cached = friendshipCacheService.getFriendRelation(userId, friendId);
            if (cached != null) {
                return cached;
            }
            
            // 查数据库
            Friendship friendship = getFriendship(userId, friendId);
            boolean isFriend = friendship != null && friendship.getStatus() == 1;
            
            // 写入缓存
            friendshipCacheService.cacheFriendRelation(userId, friendId, isFriend);
            
            return isFriend;
            
        } catch (Exception e) {
            log.error("检查好友关系异常，userId: {}, friendId: {}", userId, friendId, e);
            // 异常时返回false，不影响业务
            return false;
        }
    }
    
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = "好友模块", operation = "接受好友请求")
    public boolean acceptFriendRequest(Long userId, Long friendId) {
        if (userId == null || friendId == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "参数不能为空");
        }
        
        Friendship friendship1 = getFriendship(userId, friendId);
        Friendship friendship2 = getFriendship(friendId, userId);
        
        if (friendship1 == null || friendship2 == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "好友请求不存在");
        }
        
        if (friendship1.getStatus() != 0 || friendship2.getStatus() != 0) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "好友请求状态异常");
        }
        
        Date now = new Date();
        friendship1.setStatus(1);
        friendship1.setUpdateTime(now);
        friendship2.setStatus(1);
        friendship2.setUpdateTime(now);
        
        int result1 = friendshipMapper.updateById(friendship1);
        int result2 = friendshipMapper.updateById(friendship2);
        
        if (result1 <= 0 || result2 <= 0) {
            throw new BusinessException(ErrorCode.FAILED, "接受好友请求失败");
        }
        
        friendshipCacheService.deleteFriendListCache(userId);
        friendshipCacheService.deleteFriendListCache(friendId);
        friendshipCacheService.deleteFriendRelationCache(userId, friendId);
        friendshipCacheService.deleteFriendRelationCache(friendId, userId);

        publishFriendAcceptedNotification(userId, friendId);

        return true;
    }
    
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = "好友模块", operation = "拒绝好友请求")
    public boolean rejectFriendRequest(Long userId, Long friendId) {
        if (userId == null || friendId == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "参数不能为空");
        }
        
        Friendship friendship1 = getFriendship(userId, friendId);
        Friendship friendship2 = getFriendship(friendId, userId);
        
        if (friendship1 == null || friendship2 == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "好友请求不存在");
        }
        
        if (friendship1.getStatus() != 0 || friendship2.getStatus() != 0) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "好友请求状态异常");
        }
        
        Date now = new Date();
        friendship1.setStatus(2);
        friendship1.setUpdateTime(now);
        friendship2.setStatus(2);
        friendship2.setUpdateTime(now);
        
        int result1 = friendshipMapper.updateById(friendship1);
        int result2 = friendshipMapper.updateById(friendship2);
        
        if (result1 <= 0 || result2 <= 0) {
            throw new BusinessException(ErrorCode.FAILED, "拒绝好友请求失败");
        }
        
        friendshipCacheService.deleteFriendRelationCache(userId, friendId);
        friendshipCacheService.deleteFriendRelationCache(friendId, userId);
        
        return true;
    }
    
    @Override
    public List<FriendApplyVO> getPendingFriendRequests(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "用户ID不能为空");
        }
        
        LambdaQueryWrapper<Friendship> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friendship::getFriendId, userId)
               .eq(Friendship::getStatus, 0)
               .eq(Friendship::getIsDeleted, 0)
               .orderByDesc(Friendship::getCreateTime);
        
        List<Friendship> friendships = friendshipMapper.selectList(wrapper);
        
        if (friendships == null || friendships.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Long> senderIds = friendships.stream()
            .map(f -> f.getApplicantUserId() != null ? f.getApplicantUserId() : f.getUserId())
            .distinct()
            .collect(Collectors.toList());
        
        List<User> senderUsers = userMapper.selectBatchIds(senderIds);
        Map<Long, User> userMap = senderUsers.stream()
            .collect(Collectors.toMap(User::getId, u -> u));
        
        return friendships.stream()
            .map(friendship -> {
                FriendApplyVO vo = new FriendApplyVO();
                vo.setListType("RECEIVED");
                vo.setId(friendship.getId());
                vo.setApplicantUserId(friendship.getApplicantUserId());
                vo.setUserId(friendship.getUserId());
                vo.setFriendId(friendship.getFriendId());
                vo.setStatus(friendship.getStatus());
                vo.setCreateTime(friendship.getCreateTime());
                vo.setLastApplyTime(friendship.getLastApplyTime());
                vo.setRemark(friendship.getRemark());
                vo.setVerifyMessage(friendship.getVerifyMessage());

                Long senderId = friendship.getApplicantUserId() != null
                        ? friendship.getApplicantUserId()
                        : friendship.getUserId();
                User senderUser = userMap.get(senderId);
                if (senderUser != null) {
                    vo.setUsername(senderUser.getUsername());
                    vo.setNickname(senderUser.getNickname());
                    vo.setAvatar(senderUser.getAvatar());
                }
                
                return vo;
            })
            .collect(Collectors.toList());
    }

    /**
     * <p><b>TODO</b>：查询 user_id=当前用户、status=0、applicant_user_id=当前用户；组装 {@link FriendApplyVO} listType=SENT，
     * 批量查对端（friend_id）用户资料；排序同收到列表。
     */
    @Override
    public List<FriendApplyVO> getSentFriendRequests(Long userId) {
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED, "getSentFriendRequests：见 FriendshipServiceImpl 方法注释 TODO");
    }

    /**
     * <p><b>TODO</b>：与 getSentFriendRequests 相同 WHERE 的 count。
     */
    @Override
    public Integer getSentPendingCount(Long userId) {
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED, "getSentPendingCount：见 FriendshipServiceImpl 方法注释 TODO");
    }

    /**
     * <p><b>TODO（发起方撤销）</b>
     * <ol>
     *   <li>加载双向行，均为 status=0。</li>
     *   <li>校验 applicant_user_id == 当前 userId。</li>
     *   <li>调用 {@link FriendRequestRateLimiter#checkCancelAllowed(long, long)}。</li>
     *   <li>双向 update status=4（已撤销），清缓存。</li>
     *   <li>{@link FriendRequestRateLimiter#recordCancelAction(long, long)}。</li>
     * </ol>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelFriendRequest(Long userId, Long friendId) {
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED, "cancelFriendRequest：见 FriendshipServiceImpl 方法注释 TODO");
    }
    
    // ==================== 私有方法 ====================

    /**
     * 好友申请：通知被申请方（走 im-notification-topic → WebSocket /user/queue/notifications）。
     */
    private void publishFriendApplyNotification(Long fromUserId, Long toUserId, String verifyMessage,
                                                long pairMin, long pairMax) {
        User from = userMapper.selectById(fromUserId);
        String name = resolveDisplayName(from, fromUserId);
        NotificationDTO dto = new NotificationDTO();
        dto.setUserId(toUserId);
        dto.setType("friend_apply");
        dto.setTitle("好友申请");
        if (StringUtils.hasText(verifyMessage)) {
            dto.setContent(name + "：" + verifyMessage);
        } else {
            dto.setContent(name + " 请求添加你为好友");
        }
        dto.setBizId(pairMin + "_" + pairMax);
        dto.setExtra("{\"fromUserId\":" + fromUserId + "}");
        notificationPublisher.publish(dto);
    }

    /**
     * 同意好友：通知发起申请方（约定 friendId 为申请人，与接口注释一致）。
     */
    private void publishFriendAcceptedNotification(Long accepterId, Long applicantId) {
        long min = Math.min(accepterId, applicantId);
        long max = Math.max(accepterId, applicantId);
        User accepter = userMapper.selectById(accepterId);
        String name = resolveDisplayName(accepter, accepterId);
        NotificationDTO dto = new NotificationDTO();
        dto.setUserId(applicantId);
        dto.setType("friend_accept");
        dto.setTitle("好友申请已通过");
        dto.setContent(name + " 已同意你的好友申请");
        dto.setBizId(min + "_" + max);
        dto.setExtra("{\"fromUserId\":" + accepterId + "}");
        notificationPublisher.publish(dto);
    }

    private static String resolveDisplayName(User user, Long userId) {
        if (user != null && StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        if (user != null && StringUtils.hasText(user.getUsername())) {
            return user.getUsername();
        }
        return String.valueOf(userId);
    }

    /**
     * 创建好友关系对象（封装重复代码）
     */
    private Friendship createFriendship(Long userId, Long friendId, String remark,
                                        Integer status, Date now,
                                        Long applicantUserId, String verifyMessage, Date lastApplyTime) {
        Friendship friendship = new Friendship();
        friendship.setUserId(userId);
        friendship.setFriendId(friendId);
        friendship.setRemark(remark);
        friendship.setApplicantUserId(applicantUserId);
        friendship.setVerifyMessage(verifyMessage);
        friendship.setLastApplyTime(lastApplyTime);
        friendship.setStatus(status);
        friendship.setCreateTime(now);
        friendship.setUpdateTime(now);
        return friendship;
    }
    
    /**
     * 获取好友关系
     */
    private Friendship getFriendship(Long userId, Long friendId) {
        LambdaQueryWrapper<Friendship> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friendship::getUserId, userId)
               .eq(Friendship::getFriendId, friendId)
               .last("LIMIT 1");
        
        return friendshipMapper.selectOne(wrapper);
    }
    
    /**
     * 获取待处理的好友请求数量
     *
     * <p><b>TODO</b>：与 {@link #getPendingFriendRequests(Long)} 使用完全相同的 WHERE，避免角标与列表不一致。
     */
    @Override
    public Integer getPendingRequestCount(Long userId) {
        log.info("获取待处理的好友请求数量，userId: {}", userId);
        
        try {
            // 查询状态为0（待确认）的好友请求数量
            LambdaQueryWrapper<Friendship> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Friendship::getFriendId, userId)
                   .eq(Friendship::getStatus, 0)
                   .eq(Friendship::getIsDeleted, 0);
            
            Long count = friendshipMapper.selectCount(wrapper);
            return count != null ? count.intValue() : 0;
            
        } catch (Exception e) {
            log.error("获取待处理的好友请求数量异常，userId: {}", userId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取待处理请求数量失败");
        }
    }
    
    /**
     * 按分组获取好友列表
     */
    @Override
    public List<GroupingFriendVO> getGroupedFriendList(Long userId) {
        log.info("按分组获取好友列表，userId: {}", userId);
        
        try {
            // 获取所有好友
            List<FriendVO> allFriends = getFriendList(userId);
            
            // 按分组分类
            Map<String, List<FriendVO>> groupedMap = allFriends.stream()
                    .collect(Collectors.groupingBy(
                            friend -> StringUtils.hasText(friend.getGroupName()) 
                                    ? friend.getGroupName() 
                                    : "我的好友"
                    ));
            
            // 转换为VO列表
            List<GroupingFriendVO> result = new ArrayList<>();
            groupedMap.forEach((groupName, friends) -> {
                GroupingFriendVO vo = new GroupingFriendVO();
                vo.setGroupName(groupName);
                vo.setFriends(friends);
                result.add(vo);
            });
            
            // 按分组名称排序（"我的好友"排在最前面）
            result.sort((a, b) -> {
                if ("我的好友".equals(a.getGroupName())) return -1;
                if ("我的好友".equals(b.getGroupName())) return 1;
                return a.getGroupName().compareTo(b.getGroupName());
            });
            
            return result;
            
        } catch (Exception e) {
            log.error("按分组获取好友列表异常，userId: {}", userId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取分组好友列表失败");
        }
    }
}

