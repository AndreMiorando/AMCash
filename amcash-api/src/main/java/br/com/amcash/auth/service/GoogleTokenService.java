package br.com.amcash.auth.service;

import br.com.amcash.shared.exception.ServiceUnavailableException;
import br.com.amcash.shared.exception.UnauthorizedException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Service
public class GoogleTokenService {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenService(GoogleIdTokenVerifier verifier) {
        this.verifier = verifier;
    }

    public GoogleIdentity verify(String rawIdToken) {
        try {
            GoogleIdToken idToken = verifier.verify(rawIdToken);

            if (idToken == null) {
                throw new UnauthorizedException("Token do Google inválido ou expirado");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new UnauthorizedException("O e-mail da conta Google não foi verificado");
            }

            String subject = payload.getSubject();
            String email = payload.getEmail();

            if (subject == null || subject.isBlank() || email == null || email.isBlank()) {
                throw new UnauthorizedException("A conta Google não forneceu os dados obrigatórios");
            }

            String name = claimAsString(payload, "name");
            String pictureUrl = claimAsString(payload, "picture");

            if (name == null || name.isBlank()) {
                name = email.contains("@")
                        ? email.substring(0, email.indexOf('@'))
                        : email;
            }

            return new GoogleIdentity(subject, email, name, pictureUrl);
        } catch (GeneralSecurityException exception) {
            throw new UnauthorizedException("Token do Google inválido ou expirado");
        } catch (IOException exception) {
            throw new ServiceUnavailableException(
                    "Não foi possível validar o token com o Google",
                    exception
            );
        }
    }

    private String claimAsString(GoogleIdToken.Payload payload, String claim) {
        Object value = payload.get(claim);
        return value == null ? null : value.toString();
    }
}
