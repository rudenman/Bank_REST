package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.TopUpRequest;
import com.example.bankcards.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @GetMapping("all")
    public ResponseEntity<Page<CardDto>> getUserCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        Pageable pageable = PageRequest.of(page, size);
        Page<CardDto> cards = cardService.getUserCards(username, pageable);

        return ResponseEntity.ok(cards);
    }

    @PostMapping("/create")
    public ResponseEntity<CardDto> createCard() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        CardDto card = cardService.createCard(username);
        return ResponseEntity.ok(card);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CardDto> getCardDetails(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        CardDto card = cardService.getCardDetailsById(id, username);
        return ResponseEntity.ok(card);
    }

    @PatchMapping("/{id}/topup")
    public ResponseEntity<?> topUpCard(
            @PathVariable Long id,
            @RequestBody @Valid TopUpRequest request) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        cardService.topUpCardById(id, username, request.getAmount());
        return ResponseEntity.ok("Card topped up successfully. Amount: " + request.getAmount());
    }
}
