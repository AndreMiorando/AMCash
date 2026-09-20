package br.com.amcash.auth.service;

import br.com.amcash.auth.dto.response.AuthenticatedUserResponse;
import br.com.amcash.auth.dto.response.GoogleLoginResponse;
import br.com.amcash.config.JwtService;
import br.com.amcash.user.entity.User;
import br.com.amcash.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final GoogleTokenService googleTokenService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthService(
            GoogleTokenService googleTokenService,
            UserRepository userRepository,
            JwtService jwtService) {
        this.googleTokenService = googleTokenService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public GoogleLoginResponse loginWithGoogle(String idToken) {
        GoogleIdentity identity = googleTokenService.verify(idToken);

        User user = userRepository.findByGoogleSubject(identity.subject())
                .map(existingUser -> {
                    existingUser.updateGoogleProfile(
                            identity.email(),
                            identity.name(),
                            identity.pictureUrl()
                    );
                    return existingUser;
                })
                .orElseGet(() -> new User(
                        identity.subject(),
                        identity.email(),
                        identity.name(),
                        identity.pictureUrl()
                ));

        user = userRepository.save(user);

        String accessToken = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getName()
        );

        return new GoogleLoginResponse(
                accessToken,
                "Bearer",
                jwtService.getExpirationSeconds(),
                new AuthenticatedUserResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getPictureUrl()
                )
        );
    }
}
