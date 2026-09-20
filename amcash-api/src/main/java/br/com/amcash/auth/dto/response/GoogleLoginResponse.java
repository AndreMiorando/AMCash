package br.com.amcash.auth.dto.response;

public record GoogleLoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        AuthenticatedUserResponse user
) {
}
