package com.example.bankcards.repository;

import com.example.bankcards.entity.CardRequest;
import com.example.bankcards.entity.enums.CardRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardRequestRepository extends JpaRepository<CardRequest, Long> {

    List<CardRequest> findByUser_Id(Long userId);

    List<CardRequest> findByStatus(CardRequestStatus status);
}
