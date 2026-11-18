package ru.spvb.trading.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.spvb.trading.domain.market.Instrument;

public interface InstrumentRepository extends JpaRepository<Instrument, String> {
}
