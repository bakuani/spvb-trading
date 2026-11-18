package ru.spvb.trading.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.spvb.trading.domain.account.Position;
import ru.spvb.trading.domain.account.PositionId;

import java.util.List;

public interface PositionRepository extends JpaRepository<Position, PositionId> {
    List<Position> findByIdAccountIdOrderByIdSeccode(long accountId);
}
