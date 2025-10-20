package com.binance.bot.client;

import com.binance.bot.dto.ExecuteShortRequest;
import com.binance.bot.dto.ExecuteShortResponse;
import com.binance.bot.dto.SymbolInfo;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

// name: Feign Bean 이름, url: Lambda Gateway URL
@FeignClient(name = "binanceLambdaClient", url = "${lambda.api.base-url}")
public interface BinanceLambdaClient {

    /**
     * API 1: 선물 시장 심볼 정보 조회
     * GET /symbols/futures-info
     */
    @GetMapping("/symbols/futures-info")
    List<SymbolInfo> getFuturesSymbolInfo();

    /**
     * API 2: 3배 숏 포지션 주문 실행 및 손절 설정
     * POST /trades/execute-short
     */
    @PostMapping("/trades/execute-short")
    ExecuteShortResponse executeShortTrade(@RequestBody ExecuteShortRequest request);
}