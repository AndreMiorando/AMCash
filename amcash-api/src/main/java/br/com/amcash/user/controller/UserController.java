package br.com.amcash.user.controller;

import br.com.amcash.config.AuthenticatedUser;
import br.com.amcash.config.CurrentUser;
import br.com.amcash.user.dto.request.UsageProfileRequest;
import br.com.amcash.user.dto.response.UserProfileResponse;
import br.com.amcash.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserProfileResponse profile(@CurrentUser AuthenticatedUser user) {
        return userService.getProfile(user.userId());
    }

    @PutMapping("/usage-profile")
    public ResponseEntity<UserProfileResponse> updateUsageProfile(
            @CurrentUser AuthenticatedUser user,
            @Valid @RequestBody UsageProfileRequest request) {
        return ResponseEntity.ok(userService.updateUsageProfile(user.userId(), request.usageProfile()));
    }
}
