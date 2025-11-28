package com.realtime.communication.auth.adapter.in.rest;

import com.realtime.communication.auth.application.port.UserRepository;
import com.realtime.communication.auth.domain.model.User;
import com.realtime.communication.auth.domain.model.UserId;
import com.realtime.communication.auth.domain.model.UserStatus;
import com.realtime.communication.shared.domain.exception.NotFoundException;
import com.realtime.communication.shared.domain.exception.ValidationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
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
                    content = @Content(schema = @Schema(implementation = PublicUserProfileResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<PublicUserProfileResponse> getUserById(
            @PathVariable String userId,
            @AuthenticationPrincipal String currentUserId) {
        logger.info("Get user profile request for userId: {} by user: {}", userId, currentUserId);

        UserId id = new UserId(UUID.fromString(userId));
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        PublicUserProfileResponse response = toPublicProfileResponse(user);
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
            @Valid @RequestBody UpdateStatusRequest request) {
        logger.info("Update user status request for userId: {}", userId);

        UserStatus status;
        try {
            status = UserStatus.valueOf(request.status());
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid status value provided: {}", request.status());
            String validValues = String.join(", ",
                    java.util.Arrays.stream(UserStatus.values())
                            .map(Enum::name)
                            .toArray(String[]::new));
            throw new ValidationException("status",
                    "Invalid status value: " + request.status() + ". Valid values are: " + validValues);
        }

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
            @Valid @RequestBody UpdateProfileRequest request) {
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

    private PublicUserProfileResponse toPublicProfileResponse(User user) {
        return new PublicUserProfileResponse(
                user.getId().getValue().toString(),
                user.getUsername().getValue(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getStatus().name(),
                user.getCreatedAt(),
                user.getLastSeenAt()
        );
    }

    // DTOs

    /**
     * Response DTO for the authenticated user's own profile.
     * Includes sensitive information like email address.
     */
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

    /**
     * Response DTO for public user profiles.
     * Excludes sensitive information like email address for privacy.
     */
    public record PublicUserProfileResponse(
            String id,
            String username,
            String displayName,
            String avatarUrl,
            String bio,
            String status,
            Instant createdAt,
            Instant lastSeenAt
    ) {}

    public record UpdateProfileRequest(
            @Size(max = 100, message = "Display name must not exceed 100 characters")
            String displayName,

            @URL(message = "Avatar URL must be a valid URL")
            String avatarUrl,

            @Size(max = 500, message = "Bio must not exceed 500 characters")
            String bio
    ) {}

    public record UpdateStatusRequest(
            @NotNull(message = "Status is required")
            @Pattern(regexp = "ONLINE|OFFLINE|AWAY|BUSY",
                    message = "Invalid status value. Valid values are: ONLINE, OFFLINE, AWAY, BUSY")
            String status
    ) {}
}

