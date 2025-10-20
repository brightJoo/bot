package com.binance.bot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;

// API 2 응답용
@Data
public class ExecuteShortResponse {
    private String status;
    private String symbol;
    // Python 코드: entry_price
    @JsonProperty("entry_price")
    private BigDecimal entryPrice;

    // Python 코드: sl_price (Stop Loss Price)
    @JsonProperty("sl_price")
    private BigDecimal slPrice;

    // Python 코드: sl_order_id
    @JsonProperty("sl_order_id")
    private Long slOrderId;
}
