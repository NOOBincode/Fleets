package org.example.fleets.message.service;

import org.example.fleets.message.model.vo.MessageVO;

import java.util.List;

/**
 * 消息同步服务接口
 */
public interface MessageSyncService {
    
    /**
     * 用户上线时同步消息
     * 
     * @param userId 用户ID
     */
    void syncMessagesOnLogin(Long userId);
    
    /**
     * 拉取离线消息
     * 
     * @param userId 用户ID
     * @param lastSequence 上次同步的序列号
     * @param limit 拉取数量限制
     * @return 消息列表
     */
    List<MessageVO> pullOfflineMessages(Long userId, Long lastSequence, Integer limit);
    
    /**
     * 获取用户最后同步的序列号
     * 
     * @param userId 用户ID
     * @return 最后序列号
     */
    Long getLastSequence(Long userId);
    
    /**
     * 更新用户最后同步的序列号
     * 
     * @param userId 用户ID
     * @param sequence 序列号
     */
    void updateLastSequence(Long userId, Long sequence);
    
    /**
     * 获取未读消息数量
     * 
     * @param userId 用户ID
     * @return 未读消息数
     */
    Long getUnreadCount(Long userId);
}
