package com.binance.bot.batch.search;
// com.binance.bot.batch.NewListingScanJobConfig.java


import com.binance.bot.SymbolCacheInitializer;
import com.binance.bot.client.BinanceLambdaClient;

import com.binance.bot.dto.SymbolInfo;
import com.binance.bot.entity.KnownSymbol;
import com.binance.bot.entity.ScheduledTrade;
import com.binance.bot.entity.TradeStatus;
import com.binance.bot.repository.KnownSymbolRepository;
import com.binance.bot.repository.ScheduledTradeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
// ... (생략: imports)

@Slf4j
@Configuration
@RequiredArgsConstructor
public class NewListingScanJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final BinanceLambdaClient lambdaClient;
    private final ScheduledTradeRepository scheduledTradeRepository;
    private final KnownSymbolRepository knownSymbolRepository; // DB에도 저장해야 함
    private final SymbolCacheInitializer symbolCacheInitializer; // 메모리 Set 접근용 서비스

    @Value("${trade.execution-delay-minutes}")
    private long executionDelayMinutes;

    @Bean
    public Job newListingScanJob() {
        return new JobBuilder("newListingScanJob", jobRepository)
                .start(scanListingStep())
                .build();
    }

    @Bean
    public Step scanListingStep() {
        return new StepBuilder("scanListingStep", jobRepository)
                // Reader: SymbolInfo (Lambda 응답), Processor: ScheduledTrade (예약 객체)
                .<SymbolInfo, ScheduledTrade>chunk(10, transactionManager)
                .reader(apiCallItemReader())
                .processor(listingProcessor())
                .writer(dbWriter())
                .build();
    }

    // ------------------------------------
    // Reader: Lambda API 호출 및 메모리 로딩
    // ------------------------------------
    @Bean
    public ItemReader<SymbolInfo> apiCallItemReader() {
        return new ItemReader<SymbolInfo>() {
            // 전체 심볼 목록을 저장할 필드
            private List<SymbolInfo> symbolList = null;
            private int nextIndex = 0;

            @Override
            public SymbolInfo read() {
                // Job 실행 중 최초 1회만 Lambda API를 호출하여 전체 리스트를 로드합니다.
                if (symbolList == null) {
                    log.info("▶️ Lambda API 호출: /symbols/futures-info");
                    try {
                        symbolList = lambdaClient.getFuturesSymbolInfo();
                        log.info("✅ 총 {}개 심볼 정보 수신", symbolList.size());
                    } catch (Exception e) {
                        log.error("❌ Lambda API 호출 실패: {}", e.getMessage());
                        // 통신 실패 시 더 이상 읽을 아이템이 없음을 알리고 Step 종료
                        return null;
                    }
                }

                // 리스트에서 하나씩 아이템을 반환합니다.
                if (nextIndex < symbolList.size()) {
                    return symbolList.get(nextIndex++);
                } else {
                    // 모든 항목 처리 완료 시 null 반환 (Reader 종료)
                    return null;
                }
            }
        };
    }
    // ------------------------------------
    // Processor: 메모리 Map 기반 신규 코인 감지 및 예약 생성
    // ------------------------------------
    @Bean
    public ItemProcessor<SymbolInfo, ScheduledTrade> listingProcessor() {
        // 캐시된 Set을 가져옵니다.
        final Set<String> knownSymbolsSet = symbolCacheInitializer.getKnownSymbolsSet();

        return item -> {
            String symbol = item.getSymbol();

            // 1. 메모리 Set에서 신규 코인 여부 확인
            // Set.add()는 항목이 이미 존재하면 false를 반환합니다.
            if (!knownSymbolsSet.contains(symbol)) {

                // Set에 없다는 것은 신규 상장이라는 의미입니다.
                log.warn("🔥 신규 상장 코인 감지: {}", symbol);

                // 2. 예약 엔티티 생성 (1시간 지연)
                LocalDateTime listingTime = LocalDateTime.now();
                LocalDateTime executionTime = listingTime.plusMinutes(executionDelayMinutes);

                ScheduledTrade scheduledTrade = new ScheduledTrade();
                scheduledTrade.setSymbol(symbol);
                scheduledTrade.setStatus(TradeStatus.PENDING);
                scheduledTrade.setListingTime(listingTime);
                scheduledTrade.setExecutionTime(executionTime);

                log.info("   -> 1시간 후 주문 예약: {} (실행 시각: {})", symbol, executionTime);

                // 3. 메모리 Set에 즉시 추가 (다음 Reader 아이템이 중복 처리되는 것을 방지)
                knownSymbolsSet.add(symbol);

                // 4. Writer로 예약 객체를 전달
                return scheduledTrade;
            } else {
                // 기존 코인은 건너뜁니다.
                return null;
            }
        };
    }

    // ------------------------------------
    // Writer: DB에 ScheduledTrade 및 KnownSymbol 저장
    // ------------------------------------
    @Bean
    public ItemWriter<ScheduledTrade> dbWriter() {
        return chunk -> {
            for (ScheduledTrade trade : chunk) {
                // 1. ScheduledTrade 저장 (예약 주문)
                scheduledTradeRepository.save(trade);

                // 2. KnownSymbol 저장 (영구 기록. 메모리 Set은 재시작 시 초기화되므로 DB 저장은 필수)
                KnownSymbol known = new KnownSymbol(trade.getSymbol(), "TRADING", LocalDateTime.now());
                knownSymbolRepository.save(known);
            }
        };
    }
}