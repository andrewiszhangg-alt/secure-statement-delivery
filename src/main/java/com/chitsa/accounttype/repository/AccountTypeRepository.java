package com.chitsa.accounttype.repository;

import com.chitsa.accounttype.entity.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AccountTypeRepository extends JpaRepository<AccountType, UUID> {

    List<AccountType> findAllByOrderByLabelAsc();

    boolean existsByCode(String code);
}
