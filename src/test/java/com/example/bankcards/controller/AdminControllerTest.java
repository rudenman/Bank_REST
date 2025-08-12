package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CardRequestDto;
import com.example.bankcards.dto.UserDto;
import com.example.bankcards.service.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(AdminController.class)
@WithMockUser(roles = "ADMIN")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;


    @Test
    void blockCard_shouldCallServiceAndReturnOk() throws Exception {
        Long cardId = 1L;
        mockMvc.perform(patch("/api/admin/cards/{cardId}/block", cardId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Card blocked"));

        verify(adminService, times(1)).blockCard(cardId);
    }

    @Test
    void activateCard_shouldCallServiceAndReturnOk() throws Exception {
        Long cardId = 2L;
        mockMvc.perform(patch("/api/admin/cards/{cardId}/activate", cardId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Card activated"));

        verify(adminService, times(1)).activateCard(cardId);
    }

    @Test
    void deleteCard_shouldCallServiceAndReturnOk() throws Exception {
        Long cardId = 3L;
        mockMvc.perform(delete("/api/admin/cards/{cardId}/delete", cardId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Card deleted"));

        verify(adminService, times(1)).deleteCard(cardId);
    }

    @Test
    void getAllCards_shouldReturnListOfCards() throws Exception {
        List<CardDto> cards = List.of(new CardDto(), new CardDto()); // можно заполнить, если нужно
        when(adminService.getAllCards()).thenReturn(cards);

        mockMvc.perform(get("/api/admin/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(cards.size())));

        verify(adminService, times(1)).getAllCards();
    }

    @Test
    void updateUserStatus_shouldCallServiceAndReturnOk() throws Exception {
        Long userId = 10L;
        String status = "ACTIVE";

        mockMvc.perform(patch("/api/admin/users/{userId}/status", userId)
                        .param("status", status)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("User status updated"));

        verify(adminService, times(1)).updateUserStatus(userId, status);
    }

    @Test
    void getAllUsers_shouldReturnListOfUsers() throws Exception {
        List<UserDto> users = List.of(new UserDto(), new UserDto());
        when(adminService.getAllUsers()).thenReturn(users);

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(users.size())));

        verify(adminService, times(1)).getAllUsers();
    }

    @Test
    void getCardRequests_shouldReturnListOfRequests() throws Exception {
        List<CardRequestDto> requests = List.of(new CardRequestDto(), new CardRequestDto());
        when(adminService.getAllCardRequests()).thenReturn(requests);

        mockMvc.perform(get("/api/admin/card-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(requests.size())));

        verify(adminService, times(1)).getAllCardRequests();
    }

    @Test
    void updateCardRequestStatus_shouldCallServiceAndReturnOk() throws Exception {
        Long requestId = 100L;
        String status = "APPROVED";

        mockMvc.perform(patch("/api/admin/card-requests/{requestId}/status", requestId)
                        .param("status", status)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Card request status updated"));

        verify(adminService, times(1)).updateCardRequestStatus(requestId, status);
    }
}