package com.binance.bot.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class KnownSymbol {
    @Id
    private String symbol; // 예: BTCUSDT
    private String status; // 현재 상태 (예: TRADING)

    private LocalDateTime updrDtm;

    public KnownSymbol(String symbol, String status, LocalDateTime updrDtm) {
        this.symbol = symbol;
        this.status = status;
        this.updrDtm = updrDtm;
    }
}
