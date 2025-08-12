package com.example.bankcards.controller;

import com.example.bankcards.dto.CardRequestCreatingDto;
import com.example.bankcards.dto.CardRequestDto;
import com.example.bankcards.service.CardRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
@SecurityRequirement(name = "Authorization")
public class CardRequestController {

    private final CardRequestService cardRequestService;


    @Operation(
            summary = "Создать запрос на карту",
            description = "Создаёт новый запрос на карту для аутентифицированного пользователя. Требуется JWT токен в заголовке Authorization"
    )
    @PostMapping("/request")
    public ResponseEntity<CardRequestDto> createRequest(
            @RequestBody @Valid CardRequestCreatingDto requestDto) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        CardRequestDto response = cardRequestService.createRequest(requestDto, username);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Получить все запросы текущего пользователя",
            description = "Возвращает список запросов на карты для аутентифицированного пользователя. Требуется JWT токен в заголовке Authorization"
    )
    @GetMapping("/all")
    public ResponseEntity<List<CardRequestDto>> getMyRequests() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(cardRequestService.getUserRequests(username));
    }
}

