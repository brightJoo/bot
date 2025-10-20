package com.binance.bot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class ScheduledTrade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;

    @Enumerated(EnumType.STRING)
    private TradeStatus status; // PENDING, EXECUTED, FAILED, COMPLETED

    private LocalDateTime listingTime; // 신규 감지 시간
    private LocalDateTime executionTime; // 주문 실행 예약 시간 (listingTime + 1 hour)

    private String entryPrice; // 주문 실행 후 기록되는 진입 가격
    private Long slOrderId; // Stop Loss 주문 ID
}
