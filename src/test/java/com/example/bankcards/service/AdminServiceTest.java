package com.example.bankcards.service;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CardRequestDto;
import com.example.bankcards.dto.UserDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardRequest;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardRequestStatus;
import com.example.bankcards.entity.enums.CardRequestType;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.UserRole;
import com.example.bankcards.entity.enums.UserStatus;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.CardRequestRepository;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminServiceTest {

    private UserRepository userRepository;
    private CardRepository cardRepository;
    private CardRequestRepository cardRequestRepository;

    private AdminService adminService;

    @BeforeEach
    void setup() {
        userRepository = mock(UserRepository.class);
        cardRepository = mock(CardRepository.class);
        cardRequestRepository = mock(CardRequestRepository.class);
        adminService = new AdminService(userRepository, cardRepository, cardRequestRepository);
    }

    @Test
    void updateCardRequestStatus_shouldUpdateStatus_whenRequestExists() {
        CardRequest request = new CardRequest();
        request.setId(1L);
        request.setStatus(CardRequestStatus.PENDING);

        when(cardRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(cardRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        adminService.updateCardRequestStatus(1L, "APPROVED");

        assertEquals(CardRequestStatus.APPROVED, request.getStatus());
        verify(cardRequestRepository).save(request);
    }

    @Test
    void updateCardRequestStatus_shouldThrow_whenRequestNotFound() {
        when(cardRequestRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminService.updateCardRequestStatus(1L, "APPROVED"));
        assertEquals("Card request not found", ex.getMessage());
    }

    @Test
    void updateCardRequestStatus_shouldThrow_whenInvalidStatus() {
        CardRequest request = new CardRequest();
        when(cardRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminService.updateCardRequestStatus(1L, "INVALID_STATUS"));
        assertEquals("Invalid status value: INVALID_STATUS", ex.getMessage());
    }

    @Test
    void getAllCardRequests_shouldReturnDtos() {
        CardRequest request = new CardRequest();
        request.setId(1L);
        request.setCard(new Card());
        request.getCard().setId(5L);
        request.setRequestType(CardRequestType.BLOCK);
        request.setStatus(CardRequestStatus.PENDING);
        request.setCreatedAt(Instant.now());

        when(cardRequestRepository.findAll()).thenReturn(List.of(request));

        List<CardRequestDto> result = adminService.getAllCardRequests();

        assertEquals(1, result.size());
        CardRequestDto dto = result.get(0);
        assertEquals("5", dto.getCardId());
        assertEquals("BLOCK", dto.getRequestType());
        assertEquals("PENDING", dto.getStatus());
        assertNotNull(dto.getCreatedAt());
    }

    @Test
    void blockCard_shouldSetStatusToBlocked() {
        Card card = new Card();
        card.setId(1L);
        card.setStatus(CardStatus.ACTIVE);

        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        adminService.blockCard(1L);

        assertEquals(CardStatus.BLOCKED, card.getStatus());
        verify(cardRepository).save(card);
    }

    @Test
    void blockCard_shouldThrow_whenCardNotFound() {
        when(cardRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminService.blockCard(1L));
        assertEquals("Card not found", ex.getMessage());
    }

    @Test
    void activateCard_shouldSetStatusToActive() {
        Card card = new Card();
        card.setId(1L);
        card.setStatus(CardStatus.BLOCKED);

        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        adminService.activateCard(1L);

        assertEquals(CardStatus.ACTIVE, card.getStatus());
        verify(cardRepository).save(card);
    }

    @Test
    void deleteCard_shouldDeleteCard_whenExists() {
        when(cardRepository.existsById(1L)).thenReturn(true);

        adminService.deleteCard(1L);

        verify(cardRepository).deleteById(1L);
    }

    @Test
    void deleteCard_shouldThrow_whenCardNotExists() {
        when(cardRepository.existsById(1L)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminService.deleteCard(1L));
        assertEquals("Card not found", ex.getMessage());
    }

    @Test
    void getAllCards_shouldReturnDtos() {
        Card card = new Card();
        card.setId(1L);
        card.setCardNumber("1234567812345678");
        User user = new User();
        user.setUsername("owner");
        card.setUser(user);
        card.setExpiryDate(LocalDate.of(2030, 12, 31));
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(BigDecimal.valueOf(5000));
        card.setCreatedAt(Instant.now());

        when(cardRepository.findAll()).thenReturn(List.of(card));

        List<CardDto> dtos = adminService.getAllCards();

        assertEquals(1, dtos.size());
        CardDto dto = dtos.get(0);
        assertEquals(card.getId(), dto.getId());
        assertEquals("**** **** **** 5678", dto.getMaskedCardNumber());
        assertEquals(user.getUsername(), dto.getOwnerName());
        assertEquals(card.getExpiryDate(), dto.getExpiryDate());
        assertEquals(card.getStatus().name(), dto.getStatus());
        assertEquals(card.getBalance(), dto.getBalance());
        assertNotNull(dto.getCreatedAt());
    }

    @Test
    void updateUserStatus_shouldUpdateStatusAndBlockCardsIfNeeded() {
        User user = new User();
        user.setId(1L);
        user.setStatus(UserStatus.ACTIVE);

        Card card1 = new Card();
        card1.setStatus(CardStatus.ACTIVE);
        Card card2 = new Card();
        card2.setStatus(CardStatus.ACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cardRepository.findByUser(user)).thenReturn(List.of(card1, card2));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(cardRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        adminService.updateUserStatus(1L, "BLOCKED");

        assertEquals(UserStatus.BLOCKED, user.getStatus());
        assertEquals(CardStatus.BLOCKED, card1.getStatus());
        assertEquals(CardStatus.BLOCKED, card2.getStatus());

        verify(userRepository).save(user);
        verify(cardRepository).saveAll(List.of(card1, card2));
    }

    @Test
    void updateUserStatus_shouldUpdateStatusWithoutBlockingCards() {
        User user = new User();
        user.setId(1L);
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        adminService.updateUserStatus(1L, "ACTIVE");

        assertEquals(UserStatus.ACTIVE, user.getStatus());
        verify(userRepository).save(user);
        verify(cardRepository, never()).saveAll(any());
    }

    @Test
    void updateUserStatus_shouldThrow_whenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminService.updateUserStatus(1L, "BLOCKED"));

        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void getAllUsers_shouldReturnDtos() {
        User user = new User();
        user.setId(1L);
        user.setUsername("username");
        user.setEmail("email@example.com");
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.USER);

        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserDto> dtos = adminService.getAllUsers();

        assertEquals(1, dtos.size());
        UserDto dto = dtos.get(0);
        assertEquals(user.getId(), dto.getId());
        assertEquals(user.getUsername(), dto.getUsername());
        assertEquals(user.getEmail(), dto.getEmail());
        assertEquals(user.getStatus().name(), dto.getStatus());
        assertEquals(user.getRole().name(), dto.getRole());
    }

    @Test
    void maskCardNumber_shouldMaskProperly() throws Exception {
        // Проверка приватного метода через рефлексию (если нужно)
        var method = AdminService.class.getDeclaredMethod("maskCardNumber", String.class);
        method.setAccessible(true);

        String masked = (String) method.invoke(adminService, "1234567812345678");
        assertEquals("**** **** **** 5678", masked);

        masked = (String) method.invoke(adminService, "1234");
        assertEquals("**** **** **** 1234", masked);

        masked = (String) method.invoke(adminService, "123");
        assertEquals("****", masked);

    }
}
