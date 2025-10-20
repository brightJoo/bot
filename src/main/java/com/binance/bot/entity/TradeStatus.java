package com.binance.bot.entity;

public enum TradeStatus {
    PENDING, // 1시간 대기 중
    EXECUTED, // 주문 실행 완료 (SL 대기 중)
    FAILED, // 주문 실행 실패
    COMPLETED // SL 또는 TP로 거래 종료
}
