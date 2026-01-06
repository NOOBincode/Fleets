package org.example.fleets.user.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.fleets.common.util.PageResult;
import org.example.fleets.user.model.dto.FriendAddDTO;
import org.example.fleets.user.model.vo.FriendVO;
import org.example.fleets.user.service.FriendshipService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 好友关系服务实现类
 */
@Slf4j
@Service
public class FriendshipServiceImpl implements FriendshipService {

    @Override
    public boolean addFriend(Long userId, FriendAddDTO addDTO) {
        // TODO: 实现添加好友逻辑
        return false;
    }

    @Override
    public boolean deleteFriend(Long userId, Long friendId) {
        // TODO: 实现删除好友逻辑
        return false;
    }

    @Override
    public boolean blockFriend(Long userId, Long friendId) {
        // TODO: 实现拉黑好友逻辑
        return false;
    }

    @Override
    public boolean unblockFriend(Long userId, Long friendId) {
        // TODO: 实现取消拉黑逻辑
        return false;
    }

    @Override
    public boolean updateRemark(Long userId, Long friendId, String remark) {
        // TODO: 实现更新好友备注逻辑
        return false;
    }

    @Override
    public boolean updateGroup(Long userId, Long friendId, String groupName) {
        // TODO: 实现更新好友分组逻辑
        return false;
    }

    @Override
    public List<FriendVO> getFriendList(Long userId) {
        // TODO: 实现获取好友列表逻辑
        return null;
    }

    @Override
    public PageResult<FriendVO> searchFriend(Long userId, String keyword, Integer pageNum, Integer pageSize) {
        // TODO: 实现搜索好友逻辑
        return null;
    }

    @Override
    public boolean isFriend(Long userId, Long friendId) {
        // TODO: 实现检查是否是好友逻辑
        return false;
    }
}
