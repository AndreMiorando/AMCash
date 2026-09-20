package br.com.amcash.auth.controller;

import br.com.amcash.auth.dto.request.GoogleLoginRequest;
import br.com.amcash.auth.dto.response.GoogleLoginResponse;
import br.com.amcash.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/google")
    public ResponseEntity<GoogleLoginResponse> loginWithGoogle(
            @Valid @RequestBody GoogleLoginRequest request) {

        return ResponseEntity.ok(authService.loginWithGoogle(request.idToken()));
    }
}
