package com.example.bankcards.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class CardRequestDto {
    private Long id;
    private String cardId;
    private String requestType;
    private String status;
    private Instant createdAt;
}