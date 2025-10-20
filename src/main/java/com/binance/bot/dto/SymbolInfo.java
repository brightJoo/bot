package com.binance.bot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

// API 1 응답용
@Data
public class SymbolInfo {
    private String symbol;
    private String status;
    // Python 코드: price_precision
    @JsonProperty("price_precision")
    private int pricePrecision;

    // Python 코드: quantity_precision
    @JsonProperty("quantity_precision")
    private int quantityPrecision;
    // ... 기타 정보
}
