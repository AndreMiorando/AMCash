package br.com.amcash.user.service;

import br.com.amcash.shared.exception.NotFoundException;
import br.com.amcash.user.dto.response.UserProfileResponse;
import br.com.amcash.user.entity.UsageProfile;
import br.com.amcash.user.entity.User;
import br.com.amcash.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                user.getUsageProfile()
        );
    }
}
