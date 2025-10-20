package com.binance.bot;

// com.binance.bot.service.SymbolCacheInitializer.java

import com.binance.bot.client.BinanceLambdaClient;
import com.binance.bot.dto.SymbolInfo;
import com.binance.bot.entity.KnownSymbol;
import com.binance.bot.repository.KnownSymbolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SymbolCacheInitializer {

    private final KnownSymbolRepository knownSymbolRepository;
    private final BinanceLambdaClient lambdaClient;

    // 메모리에 로드될 코인 심볼 Set (멀티스레드 환경을 고려하여 Collections.synchronizedSet 사용)
    private final Set<String> knownSymbolsSet = Collections.synchronizedSet(new HashSet<>());

    @PostConstruct
    public void init() {
        log.info("▶️ Application Startup: 기존 KnownSymbol 목록을 DB에서 로드하여 메모리에 캐싱 시작.");

        // DB에서 모든 심볼을 로드하여 Set에 저장
        Set<String> initialSymbols = knownSymbolRepository.findAll().stream()
                .map(s -> s.getSymbol())
                .collect(Collectors.toSet());
        List<SymbolInfo> symbolList = new ArrayList<>();
        if(initialSymbols.isEmpty()) {
            symbolList = lambdaClient.getFuturesSymbolInfo();
            List<KnownSymbol> symbols = symbolList.stream().map(x->
                    new KnownSymbol(x.getSymbol(), x.getStatus(), LocalDateTime.now())).toList();
            knownSymbolRepository.saveAllAndFlush(symbols);
            knownSymbolsSet.addAll(new HashSet<>(symbolList.stream().map(x->x.getSymbol()).toList()));
        }else{
            knownSymbolsSet.addAll(initialSymbols);
        }





        log.info("✅ 초기 로드된 KnownSymbol 개수: {}", knownSymbolsSet.size());
    }

    // 배치에서 사용할 수 있도록 Set 반환 메소드 제공
    public Set<String> getKnownSymbolsSet() {
        return knownSymbolsSet;
    }
}
