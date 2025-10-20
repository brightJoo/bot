package com.binance.bot.repository;

import com.binance.bot.entity.KnownSymbol;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnownSymbolRepository extends JpaRepository<KnownSymbol, String> {
    // JpaRepository에 의해 findById, save, findAll 등이 제공됩니다.
}
