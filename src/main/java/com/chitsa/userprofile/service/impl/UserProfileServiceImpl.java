package com.chitsa.userprofile.service.impl;

import com.chitsa.common.exception.DuplicateResourceException;
import com.chitsa.common.util.ValidationUtil;
import com.chitsa.userprofile.dto.UserProfileRequestDto;
import com.chitsa.userprofile.dto.UserProfileResponseDto;
import com.chitsa.userprofile.entity.UserProfile;
import com.chitsa.userprofile.exception.UserProfileNotFoundException;
import com.chitsa.userprofile.mapper.UserProfileMapper;
import com.chitsa.userprofile.repository.UserProfileRepository;
import com.chitsa.userprofile.service.UserProfileService;
import com.chitsa.userprofile.util.UserProfileConstant;
import com.chitsa.userprofile.validator.UserProfileValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@Slf4j
@Service
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository repository;
    private final UserProfileMapper mapper;
    private final UserProfileValidator validator;
    private final TransactionTemplate newTransaction;

    public UserProfileServiceImpl(
            UserProfileRepository repository,
            UserProfileMapper mapper,
            UserProfileValidator validator,
            PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.mapper = mapper;
        this.validator = validator;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional
    @Override
    public UserProfileResponseDto create(UserProfileRequestDto requestDto) {
        validator.validateForCreate(requestDto);
        return saveNew(requestDto, false);
    }

    @Transactional
    @Override
    public UserProfileResponseDto update(UUID id, UserProfileRequestDto requestDto) {
        validator.validate(requestDto);
        var entity = findEntityById(id);
        mapper.updateEntityFromDto(requestDto, entity);
        log.info("Updated UserProfile id={}", id);
        return mapper.convertEntityToResponseDto(entity);
    }

    @Transactional(readOnly = true)
    @Override
    public UserProfileResponseDto findById(UUID id) {
        return mapper.convertEntityToResponseDto(findEntityById(id));
    }

    @Transactional(readOnly = true)
    @Override
    public UserProfileResponseDto findByIdpUuid(UUID idpUuid) {
        return repository.findByIdpUuid(idpUuid)
                .map(mapper::convertEntityToResponseDto)
                .orElseThrow(() -> new UserProfileNotFoundException(UserProfileConstant.NOT_FOUND_FOR_IDP + idpUuid));
    }

    /** The insert commits on its own so a lost race can read the row that won. */
    @Transactional
    @Override
    public UserProfileResponseDto findOrCreateByIdpUuid(UserProfileRequestDto requestDto) {
        validator.validateForCreate(requestDto);
        return repository.findByIdpUuid(requestDto.idpUuid())
                .map(mapper::convertEntityToResponseDto)
                .orElseGet(() -> insertOrReadWinner(requestDto));
    }

    private UserProfileResponseDto insertOrReadWinner(UserProfileRequestDto requestDto) {
        try {
            return newTransaction.execute(status -> insert(requestDto));
        } catch (DataIntegrityViolationException ex) {
            return repository.findByIdpUuid(requestDto.idpUuid())
                    .map(mapper::convertEntityToResponseDto)
                    .orElseThrow(() -> ex);
        }
    }

    private UserProfileResponseDto insert(UserProfileRequestDto requestDto) {
        return saveNew(requestDto, true);
    }

    private UserProfileResponseDto saveNew(UserProfileRequestDto requestDto, boolean flush) {
        if (repository.existsByIdpUuid(requestDto.idpUuid())) {
            throw new DuplicateResourceException(UserProfileConstant.DUPLICATE + requestDto.idpUuid());
        }
        var entity = mapper.convertDtoToEntity(requestDto);
        entity.setIdpUuid(requestDto.idpUuid());
        var saved = flush ? repository.saveAndFlush(entity) : repository.save(entity);
        log.info("Created UserProfile id={} idpUuid={}", saved.getId(), saved.getIdpUuid());
        return mapper.convertEntityToResponseDto(saved);
    }

    @Transactional
    @Override
    public void deleteById(UUID id) {
        repository.delete(findEntityById(id));
        repository.flush();
        log.info("Deleted UserProfile id={}", id);
    }

    @Transactional(readOnly = true)
    @Override
    public void requireExists(UUID id) {
        if (!repository.existsById(id)) {
            throw new UserProfileNotFoundException(UserProfileConstant.NOT_FOUND + id);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public boolean isProfileComplete(UUID id) {
        return findEntityById(id).isProfileComplete();
    }

    @Transactional(readOnly = true)
    @Override
    public void requireCompleteProfile(UUID id) {
        if (!isProfileComplete(id)) {
            log.warn("Incomplete profile blocked action profileId={}", id);
            ValidationUtil.fail(UserProfileConstant.PROFILE_INCOMPLETE);
        }
    }

    private UserProfile findEntityById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new UserProfileNotFoundException(UserProfileConstant.NOT_FOUND + id));
    }
}
