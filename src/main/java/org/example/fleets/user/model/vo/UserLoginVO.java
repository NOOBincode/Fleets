package org.example.fleets.user.model.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Data
@Setter
@Getter
public class UserLoginVO {
    private Long userId;
    private String username;
    private String nickname;
    private String token;
    private String tokenHead;
}
