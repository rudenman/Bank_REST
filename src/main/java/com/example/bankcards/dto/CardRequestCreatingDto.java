package com.example.bankcards.dto;

import com.example.bankcards.entity.enums.CardRequestType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class
CardRequestCreatingDto {

    @NotNull(message = "Card ID is required")
    private Long cardId;
    @NotNull(message = "Request type is required")
    private CardRequestType requestType;
}
