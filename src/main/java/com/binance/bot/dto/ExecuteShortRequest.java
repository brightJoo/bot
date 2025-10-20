package com.binance.bot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;


import java.math.BigDecimal;

// API 2 요청용
@Data
@Builder
public class ExecuteShortRequest {
    private String symbol;

    @JsonProperty("usdt_amount")
    private BigDecimal usdtAmount;
}

