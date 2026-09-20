package br.com.amcash.auth.service;

public record GoogleIdentity(
        String subject,
        String email,
        String name,
        String pictureUrl
) {
}
