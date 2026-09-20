package br.com.amcash.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.GoogleUtils;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Configuration
public class GoogleAuthConfig {

    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier(
            @Value("${google.auth.client-id}") String clientId)
            throws GeneralSecurityException, IOException {

        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException(
                    "GOOGLE_CLIENT_ID deve ser configurado para habilitar o login"
            );
        }

        return new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport.Builder()
                        .trustCertificates(GoogleUtils.getCertificateTrustStore())
                        .build(),
                GsonFactory.getDefaultInstance()
        )
                .setAudience(List.of(clientId))
                .build();
    }
}
