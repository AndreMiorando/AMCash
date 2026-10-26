package br.com.amcash.work.service;

import br.com.amcash.shared.exception.BadRequestException;
import br.com.amcash.shared.exception.NotFoundException;
import br.com.amcash.user.entity.User;
import br.com.amcash.user.repository.UserRepository;
import br.com.amcash.work.dto.response.WorkEarningSourceResponse;
import br.com.amcash.work.entity.WorkEarningSource;
import br.com.amcash.work.repository.WorkEarningRepository;
import br.com.amcash.work.repository.WorkEarningSourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class WorkEarningSourceService {
    private static final String UBER = "UBER";
    private static final String NINETY_NINE = "NINETY_NINE";

    private final WorkEarningSourceRepository sourceRepository;
    private final WorkEarningRepository earningRepository;
    private final UserRepository userRepository;

    public WorkEarningSourceService(WorkEarningSourceRepository sourceRepository,
                                    WorkEarningRepository earningRepository,
                                    UserRepository userRepository) {
        this.sourceRepository = sourceRepository;
        this.earningRepository = earningRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public List<WorkEarningSourceResponse> list(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        ensureSystemSources(user);

        return sourceRepository.findAllByUserIdOrderBySortOrderAscNameAsc(userId).stream()
                .filter(WorkEarningSource::isActive)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public WorkEarningSourceResponse create(UUID userId, String requestedName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        ensureSystemSources(user);
        String name = requestedName.trim();
        WorkEarningSource existing = sourceRepository.findByUserIdAndNameIgnoreCase(userId, name).orElse(null);
        if (existing != null) {
            if (existing.isActive()) throw new BadRequestException("Esse ganho já existe");
            existing.activate();
            existing.rename(name);
            return toResponse(sourceRepository.save(existing));
        }
        int sortOrder = Math.max(2, sourceRepository.maxSortOrder(userId) + 1);
        return toResponse(sourceRepository.save(new WorkEarningSource(user, name, null, sortOrder)));
    }

    @Transactional
    public WorkEarningSourceResponse rename(UUID userId, UUID sourceId, String requestedName) {
        WorkEarningSource source = sourceRepository.findByIdAndUserId(sourceId, userId)
                .orElseThrow(() -> new NotFoundException("Ganho não encontrado"));
        if (source.getSystemKey() != null) throw new BadRequestException("Os ganhos padrão não podem ser renomeados");
        String name = requestedName.trim();
        sourceRepository.findByUserIdAndNameIgnoreCase(userId, name)
                .filter(existing -> !existing.getId().equals(sourceId))
                .ifPresent(existing -> { throw new BadRequestException("Esse ganho já existe"); });
        String previousName = source.getName();
        source.rename(name);
        earningRepository.renameSource(userId, previousName.toLowerCase(Locale.ROOT), name);
        return toResponse(sourceRepository.save(source));
    }

    @Transactional
    public void deactivate(UUID userId, UUID sourceId) {
        WorkEarningSource source = sourceRepository.findByIdAndUserId(sourceId, userId)
                .orElseThrow(() -> new NotFoundException("Ganho não encontrado"));
        if (source.getSystemKey() != null) throw new BadRequestException("Os ganhos padrão não podem ser desativados");
        source.deactivate();
        sourceRepository.save(source);
    }

    private void ensureSystemSource(User user, String name, String systemKey, int sortOrder) {
        if (!sourceRepository.existsByUserIdAndSystemKey(user.getId(), systemKey)) {
            sourceRepository.save(new WorkEarningSource(user, name, systemKey, sortOrder));
        }
    }

    private void ensureSystemSources(User user) {
        ensureSystemSource(user, "Uber", UBER, 0);
        ensureSystemSource(user, "99", NINETY_NINE, 1);
    }

    private WorkEarningSourceResponse toResponse(WorkEarningSource source) {
        return new WorkEarningSourceResponse(source.getId(), source.getName(), source.getSystemKey(),
                source.isActive(), source.getSortOrder());
    }
}
