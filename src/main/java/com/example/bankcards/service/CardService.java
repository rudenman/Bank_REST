package com.example.bankcards.service;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.InvalidCardOperationException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.security.Key;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class CardService {

    private static final String ALGORITHM = "AES";
    private static final byte[] KEY = "MySuperSecretKey".getBytes(); // заменить на env var
    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    private Key getSecretKey() {
        return new SecretKeySpec(KEY, 0, KEY.length, ALGORITHM);
    }

    public String encrypt(String cardNumber) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());
        return Base64.getEncoder().encodeToString(cipher.doFinal(cardNumber.getBytes()));
    }

    public String decrypt(String encryptedCardNumber) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey());
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedCardNumber);
        return new String(cipher.doFinal(decodedBytes));
    }

    public String maskCardNumber(String cardNumber) {
        return "**** **** **** " + cardNumber.substring(12);
    }

    @Transactional
    public CardDto createCard(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String encryptedCardNumber;
        do {
            String plainCardNumber = CardNumberGenerator.generate();
            try {
                encryptedCardNumber = encrypt(plainCardNumber);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } while (cardRepository.existsByCardNumber(encryptedCardNumber));

        LocalDate expiryDate = LocalDate.now().plusYears(5);

        try {
            Card card = Card.builder()
                    .cardNumber(encrypt(encryptedCardNumber))
                    .expiryDate(expiryDate)
                    .user(user)
                    .status(CardStatus.ACTIVE)
                    .balance(BigDecimal.ZERO)
                    .createdAt(Instant.now())
                    .build();

            cardRepository.save(card);
            return toDto(card);
        } catch (Exception e) {
            throw new RuntimeException("Error creating card", e);
        }
    }

    @Transactional(readOnly = true)
    public Page<CardDto> getUserCards(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return cardRepository.findByUser(user, pageable)
                .map(this::toDto);
    }

    public CardDto getCardDetailsById(Long cardId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Card card = cardRepository.findByIdAndUser(cardId, user)
                .orElseThrow(() -> new CardNotFoundException("Card not found"));

        return toDto(card);
    }

    @Transactional
    public void topUpCardById(Long cardId, String username, BigDecimal amount) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Card card = cardRepository.findByIdAndUser(cardId, user)
                .orElseThrow(() -> new CardNotFoundException("Card not found"));

        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new InvalidCardOperationException("Card is not active");
        }

        card.setBalance(card.getBalance().add(amount));
        cardRepository.save(card);
    }

    private CardDto toDto(Card card) {
        try {
            String decrypted = decrypt(card.getCardNumber());
            CardDto dto = new CardDto();
            dto.setId(card.getId());
            dto.setMaskedCardNumber(maskCardNumber(decrypted));
            dto.setOwnerName(card.getUser().getFirstName() + " " + card.getUser().getLastName());
            dto.setExpiryDate(card.getExpiryDate());
            dto.setStatus(card.getStatus().name());
            dto.setBalance(card.getBalance());
            dto.setCreatedAt(card.getCreatedAt().toString());
            return dto;
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt card number", e);
        }
    }
}

