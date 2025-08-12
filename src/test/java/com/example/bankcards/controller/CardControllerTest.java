package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.service.CardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CardController.class)
@WithMockUser(username = "testuser")
@AutoConfigureMockMvc(addFilters = false)
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardService cardService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getUserCards_shouldReturnPageOfCards() throws Exception {
        CardDto card1 = new CardDto();
        CardDto card2 = new CardDto();
        List<CardDto> cards = List.of(card1, card2);
        Page<CardDto> page = new PageImpl<>(cards, PageRequest.of(0, 10), cards.size());

        when(cardService.getUserCards(eq("testuser"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/cards/all")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(cards.size())));

        verify(cardService, times(1)).getUserCards(eq("testuser"), any(Pageable.class));
    }

    @Test
    void createCard_shouldReturnCreatedCard() throws Exception {
        CardDto card = new CardDto();
        when(cardService.createCard("testuser")).thenReturn(card);

        mockMvc.perform(post("/api/cards/create"))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(card)));

        verify(cardService, times(1)).createCard("testuser");
    }

    @Test
    void getCardDetails_shouldReturnCard() throws Exception {
        CardDto card = new CardDto();
        when(cardService.getCardDetailsById(5L, "testuser")).thenReturn(card);

        mockMvc.perform(get("/api/cards/{id}", 5L))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(card)));

        verify(cardService, times(1)).getCardDetailsById(5L, "testuser");
    }
}