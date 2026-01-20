package org.example.fleets.message.converter;

import org.example.fleets.message.model.entity.Message;
import org.example.fleets.message.model.vo.MessageVO;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 消息转换器 - 使用MapStruct
 */
@Mapper(componentModel = "spring")
public interface MessageConverter {
    
    /**
     * Message转MessageVO
     */
    MessageVO toVO(Message message);
    
    /**
     * 批量转换
     */
    List<MessageVO> toVOList(List<Message> messages);
}
