package com.example.bankcards.service;

import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.InvalidCardOperationException;
import com.example.bankcards.exception.InvalidTransferException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TransferServiceTest {

    private CardRepository cardRepository;
    private UserRepository userRepository;
    private TransferService transferService;

    private User user;
    private Card fromCard;
    private Card toCard;

    @BeforeEach
    void setUp() {
        cardRepository = mock(CardRepository.class);
        userRepository = mock(UserRepository.class);
        transferService = new TransferService(cardRepository, userRepository);

        user = new User();
        user.setId(1L);

        fromCard = new Card();
        fromCard.setId(100L);
        fromCard.setUser(user);
        fromCard.setBalance(BigDecimal.valueOf(500));

        toCard = new Card();
        toCard.setId(200L);
        toCard.setUser(user);
        toCard.setBalance(BigDecimal.valueOf(100));
    }

    @Test
    void transferMoney_successfulTransfer() {
        TransferRequest request = new TransferRequest();
        request.setFromCardId(fromCard.getId());
        request.setToCardId(toCard.getId());
        request.setAmount(BigDecimal.valueOf(200));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cardRepository.findByIdAndUser(fromCard.getId(), user)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndUser(toCard.getId(), user)).thenReturn(Optional.of(toCard));

        transferService.transferMoney(request, "testuser");

        assertEquals(BigDecimal.valueOf(300), fromCard.getBalance());
        assertEquals(BigDecimal.valueOf(300), toCard.getBalance());

        verify(cardRepository).save(fromCard);
        verify(cardRepository).save(toCard);
    }

    @Test
    void transferMoney_shouldThrow_whenUserNotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        TransferRequest request = new TransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(2L);
        request.setAmount(BigDecimal.TEN);

        InvalidTransferException ex = assertThrows(InvalidTransferException.class,
                () -> transferService.transferMoney(request, "testuser"));
        assertEquals("User not found", ex.getMessage());

        verifyNoInteractions(cardRepository);
    }

    @Test
    void transferMoney_shouldThrow_whenFromCardNotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cardRepository.findByIdAndUser(100L, user)).thenReturn(Optional.empty());

        TransferRequest request = new TransferRequest();
        request.setFromCardId(100L);
        request.setToCardId(200L);
        request.setAmount(BigDecimal.ONE);

        InvalidTransferException ex = assertThrows(InvalidTransferException.class,
                () -> transferService.transferMoney(request, "testuser"));
        assertEquals("Source card not found or access denied", ex.getMessage());

        verify(cardRepository, never()).save(any());
    }

    @Test
    void transferMoney_shouldThrow_whenToCardNotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cardRepository.findByIdAndUser(fromCard.getId(), user)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndUser(toCard.getId(), user)).thenReturn(Optional.empty());

        TransferRequest request = new TransferRequest();
        request.setFromCardId(fromCard.getId());
        request.setToCardId(toCard.getId());
        request.setAmount(BigDecimal.ONE);

        InvalidTransferException ex = assertThrows(InvalidTransferException.class,
                () -> transferService.transferMoney(request, "testuser"));
        assertEquals("Target card not found or access denied", ex.getMessage());

        verify(cardRepository, never()).save(any());
    }

    @Test
    void transferMoney_shouldThrow_whenSameCardTransfer() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cardRepository.findByIdAndUser(fromCard.getId(), user)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndUser(fromCard.getId(), user)).thenReturn(Optional.of(fromCard));

        TransferRequest request = new TransferRequest();
        request.setFromCardId(fromCard.getId());
        request.setToCardId(fromCard.getId());
        request.setAmount(BigDecimal.ONE);

        InvalidCardOperationException ex = assertThrows(InvalidCardOperationException.class,
                () -> transferService.transferMoney(request, "testuser"));
        assertEquals("Cannot transfer to the same card", ex.getMessage());

        verify(cardRepository, never()).save(any());
    }

    @Test
    void transferMoney_shouldThrow_whenCardsBelongToDifferentUsers() {
        User otherUser = new User();
        otherUser.setId(2L);

        Card toCardOtherUser = new Card();
        toCardOtherUser.setId(300L);
        toCardOtherUser.setUser(otherUser);
        toCardOtherUser.setBalance(BigDecimal.valueOf(100));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cardRepository.findByIdAndUser(fromCard.getId(), user)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndUser(toCardOtherUser.getId(), user)).thenReturn(Optional.of(toCardOtherUser));

        TransferRequest request = new TransferRequest();
        request.setFromCardId(fromCard.getId());
        request.setToCardId(toCardOtherUser.getId());
        request.setAmount(BigDecimal.ONE);

        InvalidCardOperationException ex = assertThrows(InvalidCardOperationException.class,
                () -> transferService.transferMoney(request, "testuser"));
        assertEquals("Cards must belong to the same user", ex.getMessage());

        verify(cardRepository, never()).save(any());
    }

    @Test
    void transferMoney_shouldThrow_whenInsufficientFunds() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cardRepository.findByIdAndUser(fromCard.getId(), user)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndUser(toCard.getId(), user)).thenReturn(Optional.of(toCard));

        TransferRequest request = new TransferRequest();
        request.setFromCardId(fromCard.getId());
        request.setToCardId(toCard.getId());
        request.setAmount(BigDecimal.valueOf(1000)); // больше чем баланс

        InsufficientFundsException ex = assertThrows(InsufficientFundsException.class,
                () -> transferService.transferMoney(request, "testuser"));
        assertEquals("Insufficient funds on source card", ex.getMessage());

        verify(cardRepository, never()).save(any());
    }

    @Test
    void transferMoney_shouldThrow_whenAmountIsNullOrNegative() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cardRepository.findByIdAndUser(fromCard.getId(), user)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndUser(toCard.getId(), user)).thenReturn(Optional.of(toCard));

        TransferRequest request1 = new TransferRequest();
        request1.setFromCardId(fromCard.getId());
        request1.setToCardId(toCard.getId());
        request1.setAmount(null);

        assertThrows(IllegalArgumentException.class,
                () -> transferService.transferMoney(request1, "testuser"));

        TransferRequest request2 = new TransferRequest();
        request2.setFromCardId(fromCard.getId());
        request2.setToCardId(toCard.getId());
        request2.setAmount(BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class,
                () -> transferService.transferMoney(request2, "testuser"));

        TransferRequest request3 = new TransferRequest();
        request3.setFromCardId(fromCard.getId());
        request3.setToCardId(toCard.getId());
        request3.setAmount(BigDecimal.valueOf(-5));

        assertThrows(IllegalArgumentException.class,
                () -> transferService.transferMoney(request3, "testuser"));

        verify(cardRepository, never()).save(any());
    }
}
