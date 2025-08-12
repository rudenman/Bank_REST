package com.example.bankcards.service;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.InvalidCardOperationException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CardServiceTest {

    private CardRepository cardRepository;
    private UserRepository userRepository;
    private CardService cardService;

    @BeforeEach
    void setUp() {
        cardRepository = mock(CardRepository.class);
        userRepository = mock(UserRepository.class);
        cardService = new CardService(cardRepository, userRepository);
    }

    @Test
    void encryptAndDecrypt_shouldReturnOriginalString() throws Exception {
        String original = "1234567890123456";

        String encrypted = cardService.encrypt(original);
        assertNotNull(encrypted);
        assertNotEquals(original, encrypted);

        String decrypted = cardService.decrypt(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void maskCardNumber_shouldMaskCorrectly() {
        String cardNumber = "1234567890123456";
        String masked = cardService.maskCardNumber(cardNumber);
        assertEquals("**** **** **** 3456", masked);
    }

    @Test
    void createCard_shouldSaveAndReturnCardDto() {
        User user = new User();
        user.setId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setUsername("johndoe");

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(user));
        when(cardRepository.existsByCardNumber(anyString())).thenReturn(false);

        ArgumentCaptor<Card> captor = ArgumentCaptor.forClass(Card.class);
        when(cardRepository.save(captor.capture())).thenAnswer(invocation -> {
            Card card = invocation.getArgument(0);
            card.setId(42L);
            return card;
        });

        CardDto dto = cardService.createCard("johndoe");

        assertNotNull(dto);
        assertEquals(42L, dto.getId());
        assertEquals("John Doe", dto.getOwnerName());
        assertEquals(CardStatus.ACTIVE.name(), dto.getStatus());
        assertNotNull(dto.getMaskedCardNumber());
        assertEquals(BigDecimal.ZERO, dto.getBalance());
        assertNotNull(dto.getCreatedAt());
        assertNotNull(dto.getExpiryDate());

        Card savedCard = captor.getValue();
        assertNotNull(savedCard.getCardNumber());
        assertEquals(CardStatus.ACTIVE, savedCard.getStatus());
        assertEquals(user, savedCard.getUser());
        assertEquals(BigDecimal.ZERO, savedCard.getBalance());
    }

    @Test
    void createCard_shouldThrow_whenUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cardService.createCard("unknown"));

        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void getUserCards_shouldReturnPageOfCardDtos() {
        User user = new User();
        user.setId(1L);

        Card card1 = Card.builder()
                .id(1L)
                .cardNumber(encryptSafe("1111222233334444"))
                .user(user)
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(100))
                .expiryDate(LocalDate.now().plusYears(3))
                .createdAt(Instant.now())
                .build();

        Card card2 = Card.builder()
                .id(2L)
                .cardNumber(encryptSafe("5555666677778888"))
                .user(user)
                .status(CardStatus.BLOCKED)
                .balance(BigDecimal.valueOf(50))
                .expiryDate(LocalDate.now().plusYears(2))
                .createdAt(Instant.now())
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Card> page = new PageImpl<>(List.of(card1, card2), pageable, 2);

        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(cardRepository.findByUser(user, pageable)).thenReturn(page);

        Page<CardDto> result = cardService.getUserCards("user", pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals("**** **** **** 4444", result.getContent().get(0).getMaskedCardNumber());
        assertEquals(CardStatus.ACTIVE.name(), result.getContent().get(0).getStatus());
        assertEquals("**** **** **** 8888", result.getContent().get(1).getMaskedCardNumber());
        assertEquals(CardStatus.BLOCKED.name(), result.getContent().get(1).getStatus());
    }

    @Test
    void getUserCards_shouldThrow_whenUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cardService.getUserCards("unknown", PageRequest.of(0, 1)));
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void getCardDetailsById_shouldReturnDto() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setFirstName("Jane");
        user.setLastName("Smith");

        Card card = Card.builder()
                .id(5L)
                .cardNumber(cardService.encrypt("9999888877776666"))
                .user(user)
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.TEN)
                .expiryDate(LocalDate.now().plusYears(4))
                .createdAt(Instant.now())
                .build();

        when(userRepository.findByUsername("janesmith")).thenReturn(Optional.of(user));
        when(cardRepository.findByIdAndUser(5L, user)).thenReturn(Optional.of(card));

        CardDto dto = cardService.getCardDetailsById(5L, "janesmith");

        assertEquals(5L, dto.getId());
        assertEquals("Jane Smith", dto.getOwnerName());
        assertEquals(CardStatus.ACTIVE.name(), dto.getStatus());
        assertEquals(BigDecimal.TEN, dto.getBalance());
        assertEquals("**** **** **** 6666", dto.getMaskedCardNumber());
    }

    @Test
    void getCardDetailsById_shouldThrow_whenUserNotFound() {
        when(userRepository.findByUsername("noone")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cardService.getCardDetailsById(1L, "noone"));
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void getCardDetailsById_shouldThrow_whenCardNotFound() {
        User user = new User();
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(cardRepository.findByIdAndUser(1L, user)).thenReturn(Optional.empty());

        CardNotFoundException ex = assertThrows(CardNotFoundException.class,
                () -> cardService.getCardDetailsById(1L, "user"));
        assertEquals("Card not found", ex.getMessage());
    }

    @Test
    void topUpCardById_shouldIncreaseBalance() {
        User user = new User();
        user.setId(1L);

        Card card = new Card();
        card.setId(10L);
        card.setUser(user);
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(BigDecimal.valueOf(50));

        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(cardRepository.findByIdAndUser(10L, user)).thenReturn(Optional.of(card));

        cardService.topUpCardById(10L, "user", BigDecimal.valueOf(25));

        assertEquals(BigDecimal.valueOf(75), card.getBalance());
        verify(cardRepository).save(card);
    }

    @Test
    void topUpCardById_shouldThrow_whenUserNotFound() {
        when(userRepository.findByUsername("user")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cardService.topUpCardById(1L, "user", BigDecimal.TEN));
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void topUpCardById_shouldThrow_whenCardNotFound() {
        User user = new User();
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(cardRepository.findByIdAndUser(1L, user)).thenReturn(Optional.empty());

        CardNotFoundException ex = assertThrows(CardNotFoundException.class,
                () -> cardService.topUpCardById(1L, "user", BigDecimal.TEN));
        assertEquals("Card not found", ex.getMessage());
    }

    @Test
    void topUpCardById_shouldThrow_whenCardNotActive() {
        User user = new User();
        Card card = new Card();
        card.setUser(user);
        card.setStatus(CardStatus.BLOCKED);

        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(cardRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(card));

        InvalidCardOperationException ex = assertThrows(InvalidCardOperationException.class,
                () -> cardService.topUpCardById(1L, "user", BigDecimal.TEN));
        assertEquals("Card is not active", ex.getMessage());
    }

    private String encryptSafe(String cardNumber) {
        try {
            return cardService.encrypt(cardNumber);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
