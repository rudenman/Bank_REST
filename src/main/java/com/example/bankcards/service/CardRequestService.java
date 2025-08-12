package com.example.bankcards.service;

import com.example.bankcards.dto.CardRequestCreatingDto;
import com.example.bankcards.dto.CardRequestDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardRequest;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardRequestStatus;
import com.example.bankcards.entity.enums.CardRequestType;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.exception.InvalidCardOperationException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.CardRequestRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CardRequestService {

    private final CardRequestRepository cardRequestRepository;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;

    @Transactional(readOnly = true)
    public List<CardRequestDto> getUserRequests(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return cardRequestRepository.findByUser_Id(user.getId()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }


    @Transactional
    public CardRequestDto createRequest(CardRequestCreatingDto requestDto, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Card card = cardRepository.findByIdAndUser(requestDto.getCardId(), user)
                .orElseThrow(() -> new RuntimeException("Card not found or access denied"));

        // Проверка бизнес-логики (опционально)
        if (requestDto.getRequestType() == CardRequestType.BLOCK && !card.getStatus().equals(CardStatus.ACTIVE)) {
            throw new InvalidCardOperationException("Only active cards can be blocked");
        }

        if (requestDto.getRequestType() == CardRequestType.CLOSE) {
            if (card.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                throw new InvalidCardOperationException("Cannot close card with non-zero balance");
            }
            if (!card.getStatus().equals(CardStatus.ACTIVE)) {
                throw new InvalidCardOperationException("Only active cards can be closed");
            }
        }

        CardRequest request = CardRequest.builder()
                .card(card)
                .user(user)
                .requestType(requestDto.getRequestType())
                .status(CardRequestStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        cardRequestRepository.save(request);
        return toDto(request);
    }

    private CardRequestDto toDto(CardRequest request) {
        CardRequestDto dto = new CardRequestDto();
        dto.setId(request.getId());
        dto.setCardId(String.valueOf(request.getCard() != null ? request.getCard().getId() : null));
        dto.setRequestType(request.getRequestType().name());
        dto.setStatus(request.getStatus().name());
        dto.setCreatedAt(request.getCreatedAt());
        return dto;
    }
}

