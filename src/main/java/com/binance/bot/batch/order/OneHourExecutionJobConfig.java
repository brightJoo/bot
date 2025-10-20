package com.binance.bot.batch.order;

// com.binance.bot.batch.OneHourExecutionJobConfig.java


import com.binance.bot.client.BinanceLambdaClient;

import com.binance.bot.dto.ExecuteShortRequest;
import com.binance.bot.dto.ExecuteShortResponse;
import com.binance.bot.entity.ScheduledTrade;
import com.binance.bot.entity.TradeStatus;
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
import org.springframework.batch.item.support.ListItemReader; // List 기반 Reader 사용
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OneHourExecutionJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ScheduledTradeRepository scheduledTradeRepository;
    private final BinanceLambdaClient lambdaClient;

    @Value("${trade.usdt-amount}")
    private BigDecimal usdtAmount; // application.yaml에서 정의한 거래 금액

    // ------------------------------------
    // Job 및 Step 정의
    // ------------------------------------
    @Bean
    public Job oneHourExecutionJob() {
        return new JobBuilder("oneHourExecutionJob", jobRepository)
                .start(executeTradeStep())
                .build();
    }

    @Bean
    public Step executeTradeStep() {
        return new StepBuilder("executeTradeStep", jobRepository)
                // ScheduledTrade를 읽어와서 주문 실행 후 다시 ScheduledTrade로 저장
                .<ScheduledTrade, ScheduledTrade>chunk(5, transactionManager)
                .reader(scheduledTradeReader())
                .processor(tradeExecutionProcessor())
                .writer(scheduledTradeWriter())
                .build();
    }

    // ------------------------------------
    // Reader: 실행 시간이 도래한 예약 주문 조회
    // ------------------------------------
    @Bean
    public ItemReader<ScheduledTrade> scheduledTradeReader() {
        // Job 실행 시점에 DB에서 조건에 맞는 모든 주문을 조회하여 메모리 리스트에 로드합니다.
        List<ScheduledTrade> pendingTrades = scheduledTradeRepository
                .findByExecutionTimeLessThanEqualAndStatus(
                        LocalDateTime.now(),
                        TradeStatus.PENDING
                );

        log.info("🔍 실행 대상 예약 주문 개수: {}", pendingTrades.size());

        // 메모리에 로드된 리스트를 ItemReader로 사용합니다.
        return new ListItemReader<>(pendingTrades);
    }

    // ------------------------------------
    // Processor: Lambda API 호출을 통한 주문 실행
    // ------------------------------------
    @Bean
    public ItemProcessor<ScheduledTrade, ScheduledTrade> tradeExecutionProcessor() {
        return trade -> {
            String symbol = trade.getSymbol();

            log.info("▶️ 주문 실행 시작: {} (예약 시간: {})", symbol, trade.getExecutionTime());

            ExecuteShortRequest request = ExecuteShortRequest.builder()
                    .symbol(symbol)
                    .usdtAmount(usdtAmount)
                    .build();

            try {
                // 1. Lambda API 호출 (3배 숏 포지션 진입 + 10% SL 설정)
                ExecuteShortResponse response = lambdaClient.executeShortTrade(request);

                // 2. 주문 결과 업데이트
                if ("SUCCESS".equals(response.getStatus())) {
                    trade.setStatus(TradeStatus.EXECUTED); // 주문 완료 상태
                    trade.setEntryPrice(response.getEntryPrice().toPlainString());
                    trade.setSlOrderId(response.getSlOrderId());
                    log.info("✅ 주문 성공: {}, 진입가: {}, SL ID: {}", symbol, response.getEntryPrice(), response.getSlOrderId());
                } else {
                    trade.setStatus(TradeStatus.FAILED); // 주문 실패
                    log.error("❌ 주문 실패 (Lambda 응답): {} - {}", symbol, response.getStatus());
                }
            } catch (Exception e) {
                // 통신 오류, API 오류 등 예외 발생 시 처리
                trade.setStatus(TradeStatus.FAILED);
                log.error("❌ 주문 실패 (예외 발생): {} - {}", symbol, e.getMessage());
            }

            // 업데이트된 ScheduledTrade 엔티티를 Writer로 전달
            return trade;
        };
    }

    // ------------------------------------
    // Writer: 주문 결과 DB 반영
    // ------------------------------------
    @Bean
    public ItemWriter<ScheduledTrade> scheduledTradeWriter() {
        // Simple JpaRepository saveAll을 사용하여 척크 단위로 업데이트
        return scheduledTradeRepository::saveAll;
    }
}