package com.example.bankcards.service;

import com.example.bankcards.repository.CardRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CardStatusSchedulerService {

    private final CardRepository cardRepository;

    @Transactional
    public void updateExpiredCardsStatus() {
        int updatedCount = cardRepository.markExpiredCardsBlocked();
        log.info("Updated expired cards count: {}", updatedCount);
    }
}
