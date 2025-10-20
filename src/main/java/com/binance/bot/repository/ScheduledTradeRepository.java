package com.binance.bot.repository;

import com.binance.bot.entity.ScheduledTrade;
import com.binance.bot.entity.TradeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduledTradeRepository extends JpaRepository<ScheduledTrade, Long> {
    /**
     * 실행 시간이 현재 시간보다 빠르거나 같고, 상태가 PENDING인 예약 주문을 조회합니다.
     */
    List<ScheduledTrade> findByExecutionTimeLessThanEqualAndStatus(
            LocalDateTime executionTime,
            TradeStatus status
    );
}
