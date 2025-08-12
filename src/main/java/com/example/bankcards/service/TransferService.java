package com.example.bankcards.service;

import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.InvalidCardOperationException;
import com.example.bankcards.exception.InvalidTransferException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    @Transactional
    public void transferMoney(TransferRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidTransferException("User not found"));

        Card fromCard = cardRepository.findByIdAndUser(request.getFromCardId(), user)
                .orElseThrow(() -> new InvalidTransferException("Source card not found or access denied"));

        Card toCard = cardRepository.findByIdAndUser(request.getToCardId(), user)
                .orElseThrow(() -> new InvalidTransferException("Target card not found or access denied"));

        BigDecimal amount = extractAmount(request);

        if (fromCard.getId().equals(toCard.getId())) {
            throw new InvalidCardOperationException("Cannot transfer to the same card");
        }

        if (fromCard.getUser().getId() != toCard.getUser().getId()) {
            throw new InvalidCardOperationException("Cards must belong to the same user");
        }

        if (fromCard.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds on source card");
        }

        fromCard.setBalance(fromCard.getBalance().subtract(amount));
        toCard.setBalance(toCard.getBalance().add(amount));

        cardRepository.save(fromCard);
        cardRepository.save(toCard);
    }

    private BigDecimal extractAmount(TransferRequest request) {
        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }
        return amount;
    }
}