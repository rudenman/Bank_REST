package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CardRequestDto;
import com.example.bankcards.dto.UserDto;
import com.example.bankcards.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;


    @PatchMapping("/cards/{cardId}/block")
    public ResponseEntity<?> blockCard(@PathVariable Long cardId) {
        adminService.blockCard(cardId);
        return ResponseEntity.ok("Card blocked");
    }

    @PatchMapping("/cards/{cardId}/activate")
    public ResponseEntity<?> activateCard(@PathVariable Long cardId) {
        adminService.activateCard(cardId);
        return ResponseEntity.ok("Card activated");
    }

    @DeleteMapping("/cards/{cardId}/delete")
    public ResponseEntity<?> deleteCard(@PathVariable Long cardId) {
        adminService.deleteCard(cardId);
        return ResponseEntity.ok("Card deleted");
    }

    @GetMapping("/cards")
    public ResponseEntity<List<CardDto>> getAllCards() {
        List<CardDto> cards = adminService.getAllCards();
        return ResponseEntity.ok(cards);
    }

    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<?> updateUserStatus(@PathVariable Long userId, @RequestParam String status) {
        adminService.updateUserStatus(userId, status);
        return ResponseEntity.ok("User status updated");
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = adminService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/card-requests")
    public ResponseEntity<List<CardRequestDto>> getCardRequests() {
        List<CardRequestDto> requests = adminService.getAllCardRequests();
        return ResponseEntity.ok(requests);
    }

    @PatchMapping("/card-requests/{requestId}/status")
    public ResponseEntity<?> updateCardRequestStatus(@PathVariable Long requestId, @RequestParam String status) {
        adminService.updateCardRequestStatus(requestId, status);
        return ResponseEntity.ok("Card request status updated");
    }
}
