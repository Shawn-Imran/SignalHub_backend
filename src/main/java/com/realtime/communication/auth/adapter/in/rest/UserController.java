package com.realtime.communication.auth.adapter.in.rest;

import com.realtime.communication.auth.application.port.UserRepository;
import com.realtime.communication.auth.domain.model.User;
import com.realtime.communication.auth.domain.model.UserId;
import com.realtime.communication.auth.domain.model.UserStatus;
import com.realtime.communication.shared.domain.exception.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for user management endpoints.
 * Handles user profile retrieval and updates.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "User profile and account management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Retrieve the profile of the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User profile retrieved",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<UserProfileResponse> getCurrentUser(@AuthenticationPrincipal String userId) {
        logger.info("Get current user profile request for userId: {}", userId);

        UserId id = new UserId(UUID.fromString(userId));
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        UserProfileResponse response = toProfileResponse(user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user profile by ID", description = "Retrieve any user's public profile information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserProfileResponse> getUserById(
            @PathVariable String userId,
            @AuthenticationPrincipal String currentUserId) {
        logger.info("Get user profile request for userId: {} by user: {}", userId, currentUserId);

        UserId id = new UserId(UUID.fromString(userId));
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        UserProfileResponse response = toProfileResponse(user);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/status")
    @Operation(summary = "Update user status", description = "Update the online status of the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status updated"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "400", description = "Invalid status value")
    })
    public ResponseEntity<Void> updateUserStatus(
            @AuthenticationPrincipal String userId,
            @RequestBody Map<String, String> request) {
        logger.info("Update user status request for userId: {}", userId);

        String statusStr = request.get("status");
        UserStatus status = UserStatus.valueOf(statusStr);

        UserId id = new UserId(UUID.fromString(userId));
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        user.updateStatus(status);
        userRepository.save(user);

        logger.info("User status updated: userId={}, status={}", userId, status);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/me/profile")
    @Operation(summary = "Update user profile", description = "Update profile information of the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<UserProfileResponse> updateUserProfile(
            @AuthenticationPrincipal String userId,
            @RequestBody UpdateProfileRequest request) {
        logger.info("Update user profile request for userId: {}", userId);

        UserId id = new UserId(UUID.fromString(userId));
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        user.updateProfile(request.displayName(), request.avatarUrl(), request.bio());
        User updatedUser = userRepository.save(user);

        logger.info("User profile updated: userId={}", userId);
        UserProfileResponse response = toProfileResponse(updatedUser);
        return ResponseEntity.ok(response);
    }

    private UserProfileResponse toProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId().getValue().toString(),
                user.getUsername().getValue(),
                user.getEmail().getValue(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getStatus().name(),
                user.getCreatedAt(),
                user.getLastSeenAt()
        );
    }

    // DTOs
    public record UserProfileResponse(
            String id,
            String username,
            String email,
            String displayName,
            String avatarUrl,
            String bio,
            String status,
            Instant createdAt,
            Instant lastSeenAt
    ) {}

    public record UpdateProfileRequest(
            String displayName,
            String avatarUrl,
            String bio
    ) {}
}

