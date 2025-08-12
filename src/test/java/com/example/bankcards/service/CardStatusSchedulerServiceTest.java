package com.example.bankcards.service;

import com.example.bankcards.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CardStatusSchedulerServiceTest {

    private CardRepository cardRepository;
    private CardStatusSchedulerService schedulerService;

    @BeforeEach
    void setUp() {
        cardRepository = mock(CardRepository.class);
        schedulerService = new CardStatusSchedulerService(cardRepository);
    }

    @Test
    void updateExpiredCardsStatus_shouldCallRepositoryAndLogUpdatedCount() {
        when(cardRepository.markExpiredCardsBlocked()).thenReturn(5);

        schedulerService.updateExpiredCardsStatus();

        verify(cardRepository, times(1)).markExpiredCardsBlocked();
    }
}
