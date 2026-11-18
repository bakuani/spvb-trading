package ru.spvb.trading.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.spvb.trading.domain.account.Account;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUserIdOrderById(long userId);

    Optional<Account> findByIdAndUserId(long id, long userId);
}
