package org.example.fleets.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.example.fleets.common.model.Conversation;

/**
 * 会话Mapper
 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
    
    /**
     * 增加未读消息数（幂等操作）
     */
    @Update("UPDATE conversation SET " +
            "unread_count = unread_count + 1, " +
            "last_message_id = #{messageId}, " +
            "last_message_content = #{content}, " +
            "last_message_time = #{messageTime} " +
            "WHERE conversation_id = #{conversationId} " +
            "AND owner_id = #{ownerId} " +
            "AND (last_message_time IS NULL OR last_message_time <= #{messageTime})")
    int incrementUnreadCount(@Param("conversationId") String conversationId,
                            @Param("ownerId") Long ownerId,
                            @Param("messageId") String messageId,
                            @Param("content") String content,
                            @Param("messageTime") Date messageTime);
    
    /**
     * 更新最后一条消息（不增加未读数）
     */
    @Update("UPDATE conversation SET " +
            "last_message_id = #{messageId}, " +
            "last_message_content = #{content}, " +
            "last_message_time = #{messageTime} " +
            "WHERE conversation_id = #{conversationId} " +
            "AND owner_id = #{ownerId} " +
            "AND (last_message_time IS NULL OR last_message_time <= #{messageTime})")
    int updateLastMessage(@Param("conversationId") String conversationId,
                         @Param("ownerId") Long ownerId,
                         @Param("messageId") String messageId,
                         @Param("content") String content,
                         @Param("messageTime") Date messageTime);
    
    /**
     * 清空未读消息数
     */
    @Update("UPDATE conversation SET unread_count = 0 " +
            "WHERE conversation_id = #{conversationId} AND owner_id = #{ownerId}")
    int clearUnreadCount(@Param("conversationId") String conversationId,
                        @Param("ownerId") Long ownerId);
}
