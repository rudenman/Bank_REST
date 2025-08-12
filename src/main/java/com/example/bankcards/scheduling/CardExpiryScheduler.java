package com.example.bankcards.scheduling;

import com.example.bankcards.service.CardStatusSchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardExpiryScheduler {

    private final CardStatusSchedulerService cardStatusSchedulerService;

    @Scheduled(cron = "0 0 12 * * ?")  // каждый день в 12:00
    public void scheduledUpdate() {
        cardStatusSchedulerService.updateExpiredCardsStatus();
    }
}
