package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.base.BaseUserServiceImpl;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Custom User Service Implementation - Extended with Business Logic
 * 
 * This demonstrates how to extend the generated base service with custom business logic.
 * The @Primary annotation ensures this implementation is used instead of the default one.
 */
@Service
@Primary
@Transactional
public class CustomUserServiceImpl extends BaseUserServiceImpl {
    
    public CustomUserServiceImpl(UserRepository userRepository) {
        super(userRepository);
    }

    // Override create method to add validation and auto-timestamps
    @Override
    public User create(User user) {
        validateUser(user);
        user.setCreatedAt(LocalDateTime.now());
        user.setActive(true); // Default to active
        return super.create(user);
    }

    // Override update method to add update timestamp
    @Override
    public User update(Long id, User user) {
        validateUser(user);
        user.setUpdatedAt(LocalDateTime.now());
        return super.update(id, user);
    }

    // Custom business method: Find active users only
    public List<User> findActiveUsers() {
        return userRepository.findAll().stream()
                .filter(User::getActive)
                .toList();
    }

    // Custom business method: Deactivate user instead of deleting
    public void deactivateUser(Long userId) {
        User user = findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        user.setActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    // Custom business method: Activate user
    public void activateUser(Long userId) {
        User user = findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        user.setActive(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    // Custom business method: Get user statistics
    public UserStats getUserStats() {
        List<User> allUsers = userRepository.findAll();
        long totalUsers = allUsers.size();
        long activeUsers = allUsers.stream().filter(User::getActive).count();
        long inactiveUsers = totalUsers - activeUsers;
        
        return new UserStats(totalUsers, activeUsers, inactiveUsers);
    }

    // Private validation method
    private void validateUser(User user) {
        if (user.getUsername() == null || user.getUsername().trim().length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters long");
        }
        
        if (user.getEmail() == null || !user.getEmail().contains("@")) {
            throw new IllegalArgumentException("Valid email address is required");
        }
        
        if (user.getFirstName() == null || user.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        
        // Check for duplicate username (excluding current user in updates)
        Optional<User> existingUser = findByUsername(user.getUsername());
        if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Username already exists");
        }
        
        // Check for duplicate email (excluding current user in updates)
        Optional<User> existingEmail = findByEmail(user.getEmail());
        if (existingEmail.isPresent() && !existingEmail.get().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Email already exists");
        }
    }

    // Inner class for user statistics
    public static class UserStats {
        private final long totalUsers;
        private final long activeUsers;
        private final long inactiveUsers;

        public UserStats(long totalUsers, long activeUsers, long inactiveUsers) {
            this.totalUsers = totalUsers;
            this.activeUsers = activeUsers;
            this.inactiveUsers = inactiveUsers;
        }

        public long getTotalUsers() { return totalUsers; }
        public long getActiveUsers() { return activeUsers; }
        public long getInactiveUsers() { return inactiveUsers; }
        
        @Override
        public String toString() {
            return String.format("UserStats{total=%d, active=%d, inactive=%d}", 
                                totalUsers, activeUsers, inactiveUsers);
        }
    }
}
