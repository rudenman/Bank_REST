package com.example.bankcards.controller;

import com.example.bankcards.dto.CardRequestCreatingDto;
import com.example.bankcards.dto.CardRequestDto;
import com.example.bankcards.entity.enums.CardRequestType;
import com.example.bankcards.service.CardRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CardRequestController.class)
@WithMockUser(username = "testuser")
class CardRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardRequestService cardRequestService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createRequest_shouldReturnCreatedRequest() throws Exception {
        CardRequestCreatingDto creatingDto = new CardRequestCreatingDto();
        creatingDto.setCardId(123L);
        creatingDto.setRequestType(CardRequestType.BLOCK);

        CardRequestDto responseDto = new CardRequestDto();

        when(cardRequestService.createRequest(any(CardRequestCreatingDto.class), eq("testuser")))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/requests/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creatingDto))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(responseDto)));

        ArgumentCaptor<CardRequestCreatingDto> captor = ArgumentCaptor.forClass(CardRequestCreatingDto.class);
        verify(cardRequestService, times(1)).createRequest(captor.capture(), eq("testuser"));
    }

    @Test
    void getMyRequests_shouldReturnListOfRequests() throws Exception {
        List<CardRequestDto> requests = List.of(new CardRequestDto(), new CardRequestDto());

        when(cardRequestService.getUserRequests("testuser")).thenReturn(requests);

        mockMvc.perform(get("/api/requests/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(requests.size())));

        verify(cardRequestService, times(1)).getUserRequests("testuser");
    }
}