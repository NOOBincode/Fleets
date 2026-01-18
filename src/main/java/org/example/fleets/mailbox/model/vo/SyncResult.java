package org.example.fleets.mailbox.model.vo;

import lombok.Data;
import org.example.fleets.message.model.vo.MessageVO;

import java.util.List;

/**
 * 同步结果VO
 */
@Data
public class SyncResult {
    
    // 当前最大序列号
    private Long currentSequence;
    
    // 消息列表
    private List<MessageVO> messages;
    
    // 是否还有更多消息
    private Boolean hasMore;
    
    // 总数
    private Long total;
    
    public static SyncResult empty() {
        SyncResult result = new SyncResult();
        result.setCurrentSequence(0L);
        result.setMessages(new java.util.ArrayList<>());
        result.setHasMore(false);
        result.setTotal(0L);
        return result;
    }
}
