package com.example.bankcards.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CardDto {
    private Long id;
    private String maskedCardNumber;
    private String ownerName;
    private LocalDate expiryDate;
    private String status;
    private BigDecimal balance;
    private String createdAt;
}
