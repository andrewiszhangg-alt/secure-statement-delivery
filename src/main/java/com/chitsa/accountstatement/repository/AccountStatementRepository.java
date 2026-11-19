package com.chitsa.accountstatement.repository;

import com.chitsa.accountstatement.entity.AccountStatement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountStatementRepository extends JpaRepository<AccountStatement, UUID> {

    Page<AccountStatement> findByAccountId(UUID accountId, Pageable pageable);

    Optional<AccountStatement> findByIdAndAccountId(UUID id, UUID accountId);
}
