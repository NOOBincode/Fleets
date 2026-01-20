package org.example.fleets.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.cache.redis.RedisService;
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
import org.example.fleets.user.model.vo.GroupingVO;
import org.example.fleets.user.service.FriendshipService;
import org.example.fleets.user.service.cache.FriendshipCacheService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 好友关系服务实现类
 * 
 * 并发安全设计：
 * 1. 使用分布式锁防止重复添加好友
 * 2. 双向关系原子性保证（事务）
 * 3. 缓存一致性保证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendshipServiceImpl implements FriendshipService {

    private final FriendshipMapper friendshipMapper;
    private final UserMapper userMapper;
    private final FriendshipCacheService friendshipCacheService;
    private final RedisService redisService;
    
    // Redis Key前缀
    private static final String ADD_FRIEND_LOCK_PREFIX = "friend:add:lock:";
    private static final int LOCK_EXPIRE_SECONDS = 10;

    /**
     * 添加好友
     * 并发安全：使用分布式锁防止重复添加
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addFriend(Long userId, FriendAddDTO addDTO) {
        log.info("添加好友，userId: {}, friendId: {}", userId, addDTO.getFriendId());
        
        // 参数校验
        if (userId == null || addDTO.getFriendId() == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "参数不能为空");
        }
        
        if (userId.equals(addDTO.getFriendId())) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "不能添加自己为好友");
        }
        
        // 分布式锁：防止并发重复添加（使用较小的userId作为锁key，保证双向一致）
        Long smallerId = Math.min(userId, addDTO.getFriendId());
        Long largerId = Math.max(userId, addDTO.getFriendId());
        String lockKey = ADD_FRIEND_LOCK_PREFIX + smallerId + ":" + largerId;
        
        Boolean locked = redisService.setIfAbsent(lockKey, "1", LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
        if (locked == null || !locked) {
            log.warn("添加好友失败，获取分布式锁失败，userId: {}, friendId: {}", userId, addDTO.getFriendId());
            throw new BusinessException(ErrorCode.FAILED, "操作过于频繁，请稍后再试");
        }
        
        try {
            // 检查对方用户是否存在
            User friendUser = userMapper.selectById(addDTO.getFriendId());
            if (friendUser == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND, "对方用户不存在");
            }
            
            if (friendUser.getStatus() != 1) {
                throw new BusinessException(ErrorCode.USER_DISABLED, "对方账号已被禁用");
            }
            
            // 检查是否已经是好友
            if (isFriend(userId, addDTO.getFriendId())) {
                throw new BusinessException(ErrorCode.VALIDATE_FAILED, "已经是好友关系");
            }
            
            // 检查是否被对方拉黑
            Friendship blockCheck = getFriendship(addDTO.getFriendId(), userId);
            if (blockCheck != null && blockCheck.getStatus() == 3) {
                throw new BusinessException(ErrorCode.VALIDATE_FAILED, "无法添加该用户");
            }
            
            Date now = new Date();
            
            // 创建双向好友关系（带验证流程）
            // 发起方：status=0（待确认），等待对方同意
            // 接收方：status=0（待确认），需要对方处理
            Friendship senderFriendship = createFriendship(
                userId, 
                addDTO.getFriendId(), 
                addDTO.getRemark(), 
                addDTO.getGroupName(), 
                0,  // 待确认
                now
            );
            
            Friendship receiverFriendship = createFriendship(
                addDTO.getFriendId(), 
                userId, 
                null,  // 对方备注为空
                "我的好友",  // 默认分组
                0,  // 待确认
                now
            );
            
            // 原子性插入双向关系
            int result1 = friendshipMapper.insert(senderFriendship);
            int result2 = friendshipMapper.insert(receiverFriendship);
            
            if (result1 <= 0 || result2 <= 0) {
                throw new BusinessException(ErrorCode.FAILED, "添加好友失败");
            }
            
            // TODO: 发送好友验证消息通知对方（需要消息模块支持）
            // messageService.sendFriendRequest(userId, addDTO.getFriendId(), addDTO.getVerifyMessage());
            
            // 清理双方的好友列表缓存
            friendshipCacheService.deleteFriendListCache(userId);
            friendshipCacheService.deleteFriendListCache(addDTO.getFriendId());
            
            // 清理好友关系缓存
            friendshipCacheService.deleteFriendRelationCache(userId, addDTO.getFriendId());
            friendshipCacheService.deleteFriendRelationCache(addDTO.getFriendId(), userId);
            
            log.info("添加好友成功，userId: {}, friendId: {}", userId, addDTO.getFriendId());
            return true;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("添加好友异常，userId: {}, friendId: {}", userId, addDTO.getFriendId(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "添加好友失败");
        } finally {
            redisService.delete(lockKey);
        }
    }

    /**
     * 删除好友
     * 双向删除，保证数据一致性
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFriend(Long userId, Long friendId) {
        log.info("删除好友，userId: {}, friendId: {}", userId, friendId);
        
        if (userId == null || friendId == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "参数不能为空");
        }
        
        try {
            // 删除双向关系（逻辑删除）
            LambdaQueryWrapper<Friendship> wrapper1 = new LambdaQueryWrapper<>();
            wrapper1.eq(Friendship::getUserId, userId)
                   .eq(Friendship::getFriendId, friendId);
            
            LambdaQueryWrapper<Friendship> wrapper2 = new LambdaQueryWrapper<>();
            wrapper2.eq(Friendship::getUserId, friendId)
                   .eq(Friendship::getFriendId, userId);
            
            int result1 = friendshipMapper.delete(wrapper1);
            int result2 = friendshipMapper.delete(wrapper2);
            
            if (result1 <= 0 && result2 <= 0) {
                throw new BusinessException(ErrorCode.VALIDATE_FAILED, "好友关系不存在");
            }
            
            // 清理缓存
            friendshipCacheService.deleteFriendListCache(userId);
            friendshipCacheService.deleteFriendListCache(friendId);
            friendshipCacheService.deleteFriendRelationCache(userId, friendId);
            friendshipCacheService.deleteFriendRelationCache(friendId, userId);
            
            log.info("删除好友成功，userId: {}, friendId: {}", userId, friendId);
            return true;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除好友异常，userId: {}, friendId: {}", userId, friendId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除好友失败");
        }
    }

    /**
     * 拉黑好友
     * 单向操作，只修改当前用户对好友的状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean blockFriend(Long userId, Long friendId) {
        log.info("拉黑好友，userId: {}, friendId: {}", userId, friendId);
        
        if (userId == null || friendId == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "参数不能为空");
        }
        
        try {
            Friendship friendship = getFriendship(userId, friendId);
            if (friendship == null) {
                throw new BusinessException(ErrorCode.VALIDATE_FAILED, "好友关系不存在");
            }
            
            if (friendship.getStatus() == 3) {
                throw new BusinessException(ErrorCode.VALIDATE_FAILED, "已经拉黑该好友");
            }
            
            // 更新状态为拉黑
            friendship.setStatus(3);
            friendship.setUpdateTime(new Date());
            
            int result = friendshipMapper.updateById(friendship);
            if (result <= 0) {
                throw new BusinessException(ErrorCode.FAILED, "拉黑好友失败");
            }
            
            // 清理缓存
            friendshipCacheService.deleteFriendListCache(userId);
            friendshipCacheService.deleteFriendRelationCache(userId, friendId);
            
            log.info("拉黑好友成功，userId: {}, friendId: {}", userId, friendId);
            return true;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("拉黑好友异常，userId: {}, friendId: {}", userId, friendId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "拉黑好友失败");
        }
    }

    /**
     * 取消拉黑
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unblockFriend(Long userId, Long friendId) {
        log.info("取消拉黑，userId: {}, friendId: {}", userId, friendId);
        
        if (userId == null || friendId == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "参数不能为空");
        }
        
        try {
            Friendship friendship = getFriendship(userId, friendId);
            if (friendship == null) {
                throw new BusinessException(ErrorCode.VALIDATE_FAILED, "好友关系不存在");
            }
            
            if (friendship.getStatus() != 3) {
                throw new BusinessException(ErrorCode.VALIDATE_FAILED, "该好友未被拉黑");
            }
            
            // 恢复为正常状态
            friendship.setStatus(1);
            friendship.setUpdateTime(new Date());
            
            int result = friendshipMapper.updateById(friendship);
            if (result <= 0) {
                throw new BusinessException(ErrorCode.FAILED, "取消拉黑失败");
            }
            
            // 清理缓存
            friendshipCacheService.deleteFriendListCache(userId);
            friendshipCacheService.deleteFriendRelationCache(userId, friendId);
            
            log.info("取消拉黑成功，userId: {}, friendId: {}", userId, friendId);
            return true;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("取消拉黑异常，userId: {}, friendId: {}", userId, friendId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "取消拉黑失败");
        }
    }

    /**
     * 更新好友备注
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRemark(Long userId, Long friendId, String remark) {
        log.info("更新好友备注，userId: {}, friendId: {}, remark: {}", userId, friendId, remark);
        
        if (userId == null || friendId == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "参数不能为空");
        }
        
        if (StringUtils.hasText(remark) && remark.length() > 50) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "备注长度不能超过50");
        }
        
        try {
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
            
            // 清理缓存
            friendshipCacheService.deleteFriendListCache(userId);
            
            log.info("更新好友备注成功，userId: {}, friendId: {}", userId, friendId);
            return true;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新好友备注异常，userId: {}, friendId: {}", userId, friendId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新备注失败");
        }
    }

    /**
     * 更新好友分组
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateGroup(Long userId, Long friendId, String groupName) {
        log.info("更新好友分组，userId: {}, friendId: {}, groupName: {}", userId, friendId, groupName);
        
        if (userId == null || friendId == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "参数不能为空");
        }
        
        if (StringUtils.hasText(groupName) && groupName.length() > 20) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "分组名称长度不能超过20");
        }
        
        try {
            Friendship friendship = getFriendship(userId, friendId);
            if (friendship == null) {
                throw new BusinessException(ErrorCode.VALIDATE_FAILED, "好友关系不存在");
            }
            
            friendship.setGroupName(groupName);
            friendship.setUpdateTime(new Date());
            
            int result = friendshipMapper.updateById(friendship);
            if (result <= 0) {
                throw new BusinessException(ErrorCode.FAILED, "更新分组失败");
            }
            
            // 清理缓存
            friendshipCacheService.deleteFriendListCache(userId);
            
            log.info("更新好友分组成功，userId: {}, friendId: {}", userId, friendId);
            return true;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新好友分组异常，userId: {}, friendId: {}", userId, friendId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新分组失败");
        }
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
                    vo.setGroupName(friendship.getGroupName());
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
                    vo.setGroupName(friendship.getGroupName());
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
    
    /**
     * 接受好友请求
     * 将双方的好友关系状态从待确认改为已确认
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean acceptFriendRequest(Long userId, Long friendId) {
        log.info("接受好友请求，userId: {}, friendId: {}", userId, friendId);
        
        if (userId == null || friendId == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "参数不能为空");
        }
        
        try {
            // 查询双向关系
            Friendship friendship1 = getFriendship(userId, friendId);
            Friendship friendship2 = getFriendship(friendId, userId);
            
            if (friendship1 == null || friendship2 == null) {
                throw new BusinessException(ErrorCode.VALIDATE_FAILED, "好友请求不存在");
            }
            
            if (friendship1.getStatus() != 0 || friendship2.getStatus() != 0) {
                throw new BusinessException(ErrorCode.VALIDATE_FAILED, "好友请求状态异常");
            }
            
            // 更新双方状态为已确认
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
            
            // 清理缓存
            friendshipCacheService.deleteFriendListCache(userId);
            friendshipCacheService.deleteFriendListCache(friendId);
            friendshipCacheService.deleteFriendRelationCache(userId, friendId);
            friendshipCacheService.deleteFriendRelationCache(friendId, userId);
            
            // TODO: 发送好友添加成功通知（需要消息模块支持）
            
            log.info("接受好友请求成功，userId: {}, friendId: {}", userId, friendId);
            return true;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("接受好友请求异常，userId: {}, friendId: {}", userId, friendId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "接受好友请求失败");
        }
    }
    
    /**
     * 拒绝好友请求
     * 将双方的好友关系状态改为已拒绝
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean rejectFriendRequest(Long userId, Long friendId) {
        log.info("拒绝好友请求，userId: {}, friendId: {}", userId, friendId);
        
        if (userId == null || friendId == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "参数不能为空");
        }
        
        try {
            // 查询双向关系
            Friendship friendship1 = getFriendship(userId, friendId);
            Friendship friendship2 = getFriendship(friendId, userId);
            
            if (friendship1 == null || friendship2 == null) {
                throw new BusinessException(ErrorCode.VALIDATE_FAILED, "好友请求不存在");
            }
            
            if (friendship1.getStatus() != 0 || friendship2.getStatus() != 0) {
                throw new BusinessException(ErrorCode.VALIDATE_FAILED, "好友请求状态异常");
            }
            
            // 更新双方状态为已拒绝
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
            
            // 清理缓存
            friendshipCacheService.deleteFriendRelationCache(userId, friendId);
            friendshipCacheService.deleteFriendRelationCache(friendId, userId);
            
            log.info("拒绝好友请求成功，userId: {}, friendId: {}", userId, friendId);
            return true;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("拒绝好友请求异常，userId: {}, friendId: {}", userId, friendId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "拒绝好友请求失败");
        }
    }
    
    /**
     * 获取待处理的好友请求列表
     */
    @Override
    public List<FriendApplyVO> getPendingFriendRequests(Long userId) {
        log.info("获取待处理的好友请求列表，userId: {}", userId);
        
        if (userId == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "用户ID不能为空");
        }
        
        try {
            // 查询待确认的好友关系（别人发给我的）
            LambdaQueryWrapper<Friendship> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Friendship::getFriendId, userId)  // 注意：这里是 friendId，表示收到的请求
                   .eq(Friendship::getStatus, 0)  // 待确认
                   .eq(Friendship::getIsDeleted, 0)
                   .orderByDesc(Friendship::getCreateTime);
            
            List<Friendship> friendships = friendshipMapper.selectList(wrapper);
            
            if (friendships == null || friendships.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 批量查询发起人信息
            List<Long> senderIds = friendships.stream()
                .map(Friendship::getUserId)  // 发起人ID
                .collect(Collectors.toList());
            
            List<User> senderUsers = userMapper.selectBatchIds(senderIds);
            Map<Long, User> userMap = senderUsers.stream()
                .collect(Collectors.toMap(User::getId, u -> u));
            
            // 组装VO
            return friendships.stream()
                .map(friendship -> {
                    FriendApplyVO vo = new FriendApplyVO();
                    vo.setId(friendship.getId());
                    vo.setUserId(friendship.getUserId());  // 发起人ID
                    vo.setFriendId(friendship.getFriendId());  // 接收人ID（当前用户）
                    vo.setStatus(friendship.getStatus());
                    vo.setCreateTime(friendship.getCreateTime());
                    
                    User senderUser = userMap.get(friendship.getUserId());
                    if (senderUser != null) {
                        vo.setUsername(senderUser.getUsername());
                        vo.setNickname(senderUser.getNickname());
                        vo.setAvatar(senderUser.getAvatar());
                    }
                    
                    // TODO: 添加验证消息字段（需要在 Friendship 实体中添加）
                    vo.setVerifyMessage(null);
                    
                    return vo;
                })
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("获取待处理的好友请求列表异常，userId: {}", userId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取好友请求列表失败");
        }
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 创建好友关系对象（封装重复代码）
     */
    private Friendship createFriendship(Long userId, Long friendId, String remark, 
                                       String groupName, Integer status, Date now) {
        Friendship friendship = new Friendship();
        friendship.setUserId(userId);
        friendship.setFriendId(friendId);
        friendship.setRemark(remark);
        friendship.setGroupName(groupName);
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
    
    /**
     * 获取用户的所有分组
     */
    @Override
    public List<GroupingVO> getUserGroups(Long userId) {
        log.info("获取用户的所有分组，userId: {}", userId);
        
        try {
            // 查询所有好友关系
            LambdaQueryWrapper<Friendship> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Friendship::getUserId, userId)
                   .eq(Friendship::getStatus, 1)
                   .eq(Friendship::getIsDeleted, 0);
            
            List<Friendship> friendships = friendshipMapper.selectList(wrapper);
            
            // 统计每个分组的数量
            Map<String, Long> groupCountMap = friendships.stream()
                    .collect(Collectors.groupingBy(
                            friendship -> StringUtils.hasText(friendship.getGroupName()) 
                                    ? friendship.getGroupName() 
                                    : "我的好友",
                            Collectors.counting()
                    ));
            
            // 转换为VO列表
            List<GroupingVO> result = new ArrayList<>();
            groupCountMap.forEach((groupName, count) -> {
                GroupingVO vo = new GroupingVO();
                vo.setGroupName(groupName);
                vo.setCount(count.intValue());
                result.add(vo);
            });
            
            // 按分组名称排序
            result.sort((a, b) -> {
                if ("我的好友".equals(a.getGroupName())) return -1;
                if ("我的好友".equals(b.getGroupName())) return 1;
                return a.getGroupName().compareTo(b.getGroupName());
            });
            
            return result;
            
        } catch (Exception e) {
            log.error("获取用户分组异常，userId: {}", userId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取分组列表失败");
        }
    }
}

