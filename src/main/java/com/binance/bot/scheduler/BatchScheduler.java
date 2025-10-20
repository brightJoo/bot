package com.binance.bot.scheduler;

// com.binance.bot.batch.BatchScheduler.java

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchScheduler {

    private final JobLauncher jobLauncher;

    // @Qualifier를 사용하여 NewListingScanJob 주입
    @Qualifier("newListingScanJob")
    private final Job newListingScanJob;

    // @Qualifier를 사용하여 OneHourExecutionJob 주입
    @Qualifier("oneHourExecutionJob")
    private final Job oneHourExecutionJob;

    // ----------------------------------------------------
    // NewListingScanJob 실행: 매시 0분, 30분에 실행
    // ----------------------------------------------------
    @Scheduled(cron = "0 0/30 * * * ?")
    public void runNewListingScanJob() {
        // Job 재실행을 위해 매번 고유한 JobParameter를 생성합니다.
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("jobName", "NewListingScanJob")
                .addLong("runTime", System.currentTimeMillis())
                .toJobParameters();

        try {
            log.info("▶️ [Scheduler] NewListingScanJob 시작: {}", jobParameters.getLong("runTime"));
            JobExecution execution = jobLauncher.run(newListingScanJob, jobParameters);
            log.info("✅ [Scheduler] NewListingScanJob 완료. Status: {}", execution.getStatus());
        } catch (Exception e) {
            log.error("❌ [Scheduler] NewListingScanJob 실행 실패", e);
        }
    }

    // ----------------------------------------------------
    // OneHourExecutionJob 실행: 매시 1분, 31분에 실행 (스캔 잡 실행 1분 후)
    // ----------------------------------------------------
    @Scheduled(cron = "0 4,5 * * * ?")
    public void runOneHourExecutionJob() {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("jobName", "OneHourExecutionJob")
                .addLong("runTime", System.currentTimeMillis())
                .toJobParameters();

        try {
            log.info("▶️ [Scheduler] OneHourExecutionJob 시작: {}", jobParameters.getLong("runTime"));
            JobExecution execution = jobLauncher.run(oneHourExecutionJob, jobParameters);
            log.info("✅ [Scheduler] OneHourExecutionJob 완료. Status: {}", execution.getStatus());
        } catch (Exception e) {
            log.error("❌ [Scheduler] OneHourExecutionJob 실행 실패", e);
        }
    }
}
