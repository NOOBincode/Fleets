package org.example.fleets.user.service;

import org.example.fleets.common.util.PageResult;
import org.example.fleets.user.model.dto.FriendAddDTO;
import org.example.fleets.user.model.vo.FriendVO;

import java.util.List;

/**
 * 好友关系服务接口
 */
public interface FriendshipService {
    
    /**
     * 添加好友
     */
    boolean addFriend(Long userId, FriendAddDTO addDTO);
    
    /**
     * 删除好友
     */
    boolean deleteFriend(Long userId, Long friendId);
    
    /**
     * 拉黑好友
     */
    boolean blockFriend(Long userId, Long friendId);
    
    /**
     * 取消拉黑
     */
    boolean unblockFriend(Long userId, Long friendId);
    
    /**
     * 更新好友备注
     */
    boolean updateRemark(Long userId, Long friendId, String remark);
    
    /**
     * 更新好友分组
     */
    boolean updateGroup(Long userId, Long friendId, String groupName);
    
    /**
     * 获取好友列表
     */
    List<FriendVO> getFriendList(Long userId);
    
    /**
     * 搜索好友
     */
    PageResult<FriendVO> searchFriend(Long userId, String keyword, Integer pageNum, Integer pageSize);
    
    /**
     * 检查是否是好友
     */
    boolean isFriend(Long userId, Long friendId);
}
