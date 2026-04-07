package org.example.fleets.message.outbox.model.enums;

import lombok.Getter;

/**
 * Outbox 事件状态
 */
@Getter
public enum OutboxStatus {
    PENDING("PENDING"),
    SENDING("SENDING"),
    SENT("SENT"),
    FAILED("FAILED"),
    DEAD("DEAD");

    private final String code;

    OutboxStatus(String code) {
        this.code = code;
    }
}

