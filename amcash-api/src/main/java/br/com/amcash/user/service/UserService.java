package br.com.amcash.user.service;

import br.com.amcash.shared.exception.NotFoundException;
import br.com.amcash.user.dto.response.UserProfileResponse;
import br.com.amcash.user.entity.UsageProfile;
import br.com.amcash.user.entity.User;
import br.com.amcash.user.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        return toResponse(findUser(userId));
    }

    @Transactional
    public UserProfileResponse updateUsageProfile(UUID userId, UsageProfile usageProfile) {
        User user = findUser(userId);
        user.updateUsageProfile(usageProfile);
        return toResponse(userRepository.save(user));
    }

    /** Desativa a conta agora; a exclusão definitiva ocorre após o prazo de carência. */
    @Transactional
    public void requestAccountDeletion(UUID userId) {
        User user = findUser(userId);
        if (user.getDeletionRequestedAt() == null) user.requestDeletion();
        userRepository.save(user);
    }

    @Transactional
    public UserProfileResponse reactivateAccount(UUID userId) {
        User user = findUser(userId);
        user.cancelDeletion();
        return toResponse(userRepository.save(user));
    }

    /** Exclui contas vencidas; as tabelas dependentes são apagadas em cascata pelo banco (ON DELETE CASCADE). */
    @Scheduled(cron = "0 15 3 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public int purgeExpiredAccounts() {
        OffsetDateTime limit = OffsetDateTime.now(ZoneOffset.UTC).minusDays(User.DELETION_GRACE_DAYS);
        return userRepository.deleteAccountsRequestedBefore(limit);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }

    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPictureUrl(),
                user.getUsageProfile(),
                user.getDeletionScheduledAt()
        );
    }
}
