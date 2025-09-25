package com.example.unifiedapi.service;

import com.example.unifiedapi.entity.OperationLog;
import com.example.unifiedapi.entity.User;
import com.example.unifiedapi.repository.OperationLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;

    @Autowired
    public OperationLogService(OperationLogRepository operationLogRepository) {
        this.operationLogRepository = operationLogRepository;
    }

    public OperationLog logOperation(String operationType, String status) {
        OperationLog log = new OperationLog();
        log.setOperationType(operationType);
        log.setOperationStatus(status);
        return operationLogRepository.save(log);
    }

    public OperationLog logOperation(String operationType, String status, Long fileSize, String fileName) {
        OperationLog log = new OperationLog();
        log.setOperationType(operationType);
        log.setOperationStatus(status);
        log.setFileSize(fileSize);
        log.setFilename(fileName);
        return operationLogRepository.save(log);
    }

    public OperationLog logOperationWithDetails(String operationType, String status, Long fileSize,
                                               String fileName, String details, String username,
                                               String matriculeFiscal, String errorMessage) {
        OperationLog log = new OperationLog();
        log.setOperationType(operationType);
        log.setOperationStatus(status);
        log.setFileSize(fileSize);
        log.setFilename(fileName);
        log.setDetails(details);
        log.setErrorMessage(errorMessage);
        // Note: username and matriculeFiscal are not stored directly in the new entity
        // They would be associated through the User entity
        return operationLogRepository.save(log);
    }

    public List<OperationLog> getRecentOperations(int count) {
        return operationLogRepository.findAllOrderByCreatedAtDesc().stream()
                .limit(count)
                .toList();
    }

    public List<OperationLog> getOperationsByType(String operationType) {
        return operationLogRepository.findByOperationTypeOrderByCreatedAtDesc(operationType);
    }

    public List<OperationLog> getOperationsByStatus(String status) {
        return operationLogRepository.findByOperationStatusOrderByCreatedAtDesc(status);
    }

    public List<OperationLog> getOperationsByUser(User user) {
        return operationLogRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    public long countByOperationType(String operationType) {
        return operationLogRepository.countByOperationType(operationType);
    }
    
    public long countByStatus(String status) {
        return operationLogRepository.countByStatus(status);
    }
    
    public long countAll() {
        return operationLogRepository.count();
    }
    
    public List<OperationLog> getRecentOperations(LocalDateTime since) {
        return operationLogRepository.findRecentOperations(since);
    }
}
