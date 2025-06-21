package com.example.demo.controller;

import com.example.demo.controller.base.BaseUserController;
import com.example.demo.entity.User;
import com.example.demo.service.CustomUserServiceImpl;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Custom User Controller - Extended with Business Endpoints
 * 
 * This demonstrates how to extend the generated base controller with custom endpoints.
 */
@RestController
@RequestMapping("/api/users")
public class CustomUserController extends BaseUserController {
    
    private final CustomUserServiceImpl customUserService;
    
    public CustomUserController(UserService userService, 
                               @Autowired CustomUserServiceImpl customUserService) {
        super(userService);
        this.customUserService = customUserService;
    }

    // Custom endpoint: Get only active users
    @GetMapping("/active")
    public ResponseEntity<List<User>> getActiveUsers() {
        List<User> activeUsers = customUserService.findActiveUsers();
        return ResponseEntity.ok(activeUsers);
    }

    // Custom endpoint: Deactivate user (soft delete)
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<String> deactivateUser(@PathVariable Long id) {
        try {
            customUserService.deactivateUser(id);
            return ResponseEntity.ok("User deactivated successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Custom endpoint: Activate user
    @PostMapping("/{id}/activate")
    public ResponseEntity<String> activateUser(@PathVariable Long id) {
        try {
            customUserService.activateUser(id);
            return ResponseEntity.ok("User activated successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Custom endpoint: Get user statistics
    @GetMapping("/stats")
    public ResponseEntity<CustomUserServiceImpl.UserStats> getUserStats() {
        CustomUserServiceImpl.UserStats stats = customUserService.getUserStats();
        return ResponseEntity.ok(stats);
    }

    // Custom endpoint: Search users by name
    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String query) {
        List<User> users = customUserService.findAll().stream()
                .filter(user -> user.getFirstName().toLowerCase().contains(query.toLowerCase()) ||
                               user.getLastName().toLowerCase().contains(query.toLowerCase()) ||
                               user.getUsername().toLowerCase().contains(query.toLowerCase()))
                .toList();
        return ResponseEntity.ok(users);
    }
}
