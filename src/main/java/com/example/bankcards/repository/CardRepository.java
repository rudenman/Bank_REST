package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {

    List<Card> findByUser(User user);


    boolean existsByCardNumber(String cardNumber);

    Page<Card> findByUser(User user, Pageable pageable);

    Optional<Card> findByIdAndUser(Long id, User user);

    @Modifying
    @Transactional
    @Query("UPDATE Card c SET c.status = 'EXPIRED' WHERE c.expiryDate < CURRENT_DATE AND c.status <> 'EXPIRED'")
    int markExpiredCardsBlocked();
}