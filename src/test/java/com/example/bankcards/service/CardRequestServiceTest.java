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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CardRequestServiceTest {

    private CardRequestRepository cardRequestRepository;
    private UserRepository userRepository;
    private CardRepository cardRepository;

    private CardRequestService cardRequestService;

    @BeforeEach
    void setUp() {
        cardRequestRepository = mock(CardRequestRepository.class);
        userRepository = mock(UserRepository.class);
        cardRepository = mock(CardRepository.class);

        cardRequestService = new CardRequestService(cardRequestRepository, userRepository, cardRepository);
    }

    @Test
    void getUserRequests_shouldReturnList() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        CardRequest request1 = CardRequest.builder()
                .id(10L)
                .requestType(CardRequestType.BLOCK)
                .status(CardRequestStatus.PENDING)
                .createdAt(Instant.now())
                .build();
        CardRequest request2 = CardRequest.builder()
                .id(20L)
                .requestType(CardRequestType.CLOSE)
                .status(CardRequestStatus.APPROVED)
                .createdAt(Instant.now())
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cardRequestRepository.findByUser_Id(1L)).thenReturn(List.of(request1, request2));

        List<CardRequestDto> dtos = cardRequestService.getUserRequests("testuser");

        assertEquals(2, dtos.size());
        assertEquals("BLOCK", dtos.get(0).getRequestType());
        assertEquals(CardRequestStatus.PENDING.name(), dtos.get(0).getStatus());
        assertEquals("CLOSE", dtos.get(1).getRequestType());
        assertEquals(CardRequestStatus.APPROVED.name(), dtos.get(1).getStatus());
    }

    @Test
    void getUserRequests_shouldThrow_whenUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cardRequestService.getUserRequests("unknown"));
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void createRequest_shouldCreateAndReturnDto() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        Card card = new Card();
        card.setId(2L);
        card.setUser(user);
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(BigDecimal.ZERO);

        CardRequestCreatingDto creatingDto = new CardRequestCreatingDto();
        creatingDto.setCardId(2L);
        creatingDto.setRequestType(CardRequestType.BLOCK);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cardRepository.findByIdAndUser(2L, user)).thenReturn(Optional.of(card));

        ArgumentCaptor<CardRequest> captor = ArgumentCaptor.forClass(CardRequest.class);

        CardRequestDto result = cardRequestService.createRequest(creatingDto, "testuser");

        verify(cardRequestRepository).save(captor.capture());
        CardRequest savedRequest = captor.getValue();

        assertEquals(card, savedRequest.getCard());
        assertEquals(user, savedRequest.getUser());
        assertEquals(CardRequestType.BLOCK, savedRequest.getRequestType());
        assertEquals(CardRequestStatus.PENDING, savedRequest.getStatus());
        assertNotNull(savedRequest.getCreatedAt());

        assertEquals(savedRequest.getId(), result.getId());
        assertEquals(savedRequest.getRequestType().name(), result.getRequestType());
        assertEquals(savedRequest.getStatus().name(), result.getStatus());
        assertEquals(savedRequest.getCreatedAt(), result.getCreatedAt());
    }

    @Test
    void createRequest_shouldThrow_whenUserNotFound() {
        CardRequestCreatingDto creatingDto = new CardRequestCreatingDto();
        creatingDto.setCardId(1L);
        creatingDto.setRequestType(CardRequestType.BLOCK);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cardRequestService.createRequest(creatingDto, "testuser"));
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void createRequest_shouldThrow_whenCardNotFoundOrAccessDenied() {
        User user = new User();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cardRepository.findByIdAndUser(1L, user)).thenReturn(Optional.empty());

        CardRequestCreatingDto creatingDto = new CardRequestCreatingDto();
        creatingDto.setCardId(1L);
        creatingDto.setRequestType(CardRequestType.BLOCK);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cardRequestService.createRequest(creatingDto, "testuser"));
        assertEquals("Card not found or access denied", ex.getMessage());
    }

    @Test
    void createRequest_shouldThrow_whenBlockingNonActiveCard() {
        User user = new User();
        Card card = new Card();
        card.setUser(user);
        card.setStatus(CardStatus.BLOCKED); // not ACTIVE

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cardRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(card));

        CardRequestCreatingDto creatingDto = new CardRequestCreatingDto();
        creatingDto.setCardId(1L);
        creatingDto.setRequestType(CardRequestType.BLOCK);

        InvalidCardOperationException ex = assertThrows(InvalidCardOperationException.class,
                () -> cardRequestService.createRequest(creatingDto, "testuser"));
        assertEquals("Only active cards can be blocked", ex.getMessage());
    }

    @Test
    void createRequest_shouldThrow_whenClosingCardWithNonZeroBalance() {
        User user = new User();
        Card card = new Card();
        card.setUser(user);
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(BigDecimal.valueOf(100));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cardRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(card));

        CardRequestCreatingDto creatingDto = new CardRequestCreatingDto();
        creatingDto.setCardId(1L);
        creatingDto.setRequestType(CardRequestType.CLOSE);

        InvalidCardOperationException ex = assertThrows(InvalidCardOperationException.class,
                () -> cardRequestService.createRequest(creatingDto, "testuser"));
        assertEquals("Cannot close card with non-zero balance", ex.getMessage());
    }

    @Test
    void createRequest_shouldThrow_whenClosingNonActiveCard() {
        User user = new User();
        Card card = new Card();
        card.setUser(user);
        card.setStatus(CardStatus.BLOCKED);
        card.setBalance(BigDecimal.ZERO);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cardRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(card));

        CardRequestCreatingDto creatingDto = new CardRequestCreatingDto();
        creatingDto.setCardId(1L);
        creatingDto.setRequestType(CardRequestType.CLOSE);

        InvalidCardOperationException ex = assertThrows(InvalidCardOperationException.class,
                () -> cardRequestService.createRequest(creatingDto, "testuser"));
        assertEquals("Only active cards can be closed", ex.getMessage());
    }
}
