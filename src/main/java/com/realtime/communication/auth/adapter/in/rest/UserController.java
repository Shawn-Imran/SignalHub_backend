package com.realtime.communication.auth.adapter.in.rest;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user management endpoints
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @GetMapping("/me")
    public Object getCurrentUser(@AuthenticationPrincipal String userId) {
        // TODO: Implement get current user profile
        return new UserProfileResponse(userId, "Current User");
    }

    private record UserProfileResponse(String userId, String displayName) {}
}

