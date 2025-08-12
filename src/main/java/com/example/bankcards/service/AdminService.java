package com.example.bankcards.service;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CardRequestDto;
import com.example.bankcards.dto.UserDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardRequest;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardRequestStatus;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.UserStatus;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.CardRequestRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final CardRequestRepository cardRequestRepository;

    @Transactional
    public void updateCardRequestStatus(Long requestId, String status) {
        CardRequest request = cardRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Card request not found"));

        CardRequestStatus newStatus;
        try {
            newStatus = CardRequestStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status value: " + status);
        }

        request.setStatus(newStatus);
        cardRequestRepository.save(request);
    }

    public List<CardRequestDto> getAllCardRequests() {
        List<CardRequest> requests = cardRequestRepository.findAll();
        return requests.stream().map(this::toCardRequestDto).collect(Collectors.toList());
    }

    @Transactional
    public void blockCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found"));
        card.setStatus(CardStatus.BLOCKED);
        cardRepository.save(card);
    }

    @Transactional
    public void activateCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found"));
        card.setStatus(CardStatus.ACTIVE);
        cardRepository.save(card);
    }

    @Transactional
    public void deleteCard(Long cardId) {
        if (!cardRepository.existsById(cardId)) {
            throw new RuntimeException("Card not found");
        }
        cardRepository.deleteById(cardId);
    }

    public List<CardDto> getAllCards() {
        List<Card> cards = cardRepository.findAll();
        return cards.stream()
                .map(this::toCardDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateUserStatus(Long userId, String status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setStatus(UserStatus.valueOf(status));
        userRepository.save(user);

        if ("BLOCKED".equalsIgnoreCase(status) || "EXPIRED".equalsIgnoreCase(status)) {
            List<Card> cards = cardRepository.findByUser(user);
            cards.forEach(card -> card.setStatus(CardStatus.BLOCKED));
            cardRepository.saveAll(cards);
        }
    }

    public List<UserDto> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::toUserDto)
                .collect(Collectors.toList());
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) return "****";
        int length = cardNumber.length();
        String last4 = cardNumber.substring(length - 4);
        return "**** **** **** " + last4;
    }

    private UserDto toUserDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setStatus(String.valueOf(user.getStatus()));
        dto.setRole(String.valueOf(user.getRole()));
        return dto;
    }

    private CardRequestDto toCardRequestDto(CardRequest request) {
        CardRequestDto dto = new CardRequestDto();
        dto.setId(request.getId());
        dto.setCardId(String.valueOf(request.getCard().getId()));
        dto.setRequestType(String.valueOf(request.getRequestType()));
        dto.setStatus(String.valueOf(request.getStatus()));
        dto.setCreatedAt(request.getCreatedAt());
        return dto;
    }

    private CardDto toCardDto(Card card) {
        CardDto dto = new CardDto();

        dto.setId(card.getId());
        dto.setMaskedCardNumber(maskCardNumber(card.getCardNumber()));
        dto.setOwnerName(card.getUser().getUsername());
        if (card.getExpiryDate() != null) {
            dto.setExpiryDate(card.getExpiryDate());
        }
        dto.setStatus(String.valueOf(card.getStatus()));
        dto.setBalance(card.getBalance());
        if (card.getCreatedAt() != null) {
            dto.setCreatedAt(card.getCreatedAt().toString());
        }

        return dto;
    }
}