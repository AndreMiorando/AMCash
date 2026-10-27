package br.com.amcash.user.service;

import br.com.amcash.user.entity.UsageProfile;
import br.com.amcash.user.entity.User;
import br.com.amcash.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTests {

    private UserRepository userRepository;
    private UserService userService;
    private User user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userService = new UserService(userRepository);
        user = new User("google-subject", "usuario@amcash.com", "Usuário", null);
        user.setId(UUID.randomUUID());
    }

    @Test
    void shouldScheduleDeletionAndAllowReactivation() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        userService.requestAccountDeletion(user.getId());

        assertNotNull(user.getDeletionRequestedAt());
        assertEquals(user.getDeletionRequestedAt().plusDays(30), user.getDeletionScheduledAt());

        var response = userService.reactivateAccount(user.getId());

        assertNull(user.getDeletionRequestedAt());
        assertNull(response.deletionScheduledAt());
    }

    @Test
    void shouldReturnCurrentUsageProfile() {
        user.updateUsageProfile(UsageProfile.PERSONAL);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        var response = userService.getProfile(user.getId());

        assertEquals(UsageProfile.PERSONAL, response.usageProfile());
        assertEquals(user.getEmail(), response.email());
    }

    @Test
    void shouldUpdateUsageProfileWithoutReplacingUserData() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        var response = userService.updateUsageProfile(user.getId(), UsageProfile.DELIVERY);

        assertEquals(UsageProfile.DELIVERY, response.usageProfile());
        assertEquals("Usuário", response.name());
        verify(userRepository).save(user);
    }
}
