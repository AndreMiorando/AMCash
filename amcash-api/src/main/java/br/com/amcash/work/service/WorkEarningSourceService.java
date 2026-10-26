package br.com.amcash.work.service;

import br.com.amcash.shared.exception.BadRequestException;
import br.com.amcash.shared.exception.NotFoundException;
import br.com.amcash.user.entity.UsageProfile;
import br.com.amcash.user.entity.User;
import br.com.amcash.user.repository.UserRepository;
import br.com.amcash.work.dto.response.WorkEarningSourceResponse;
import br.com.amcash.work.entity.WorkEarningSource;
import br.com.amcash.work.repository.WorkEarningRepository;
import br.com.amcash.work.repository.WorkEarningSourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class WorkEarningSourceService {
    private record SystemSource(String key, String name, int sortOrder) {}

    private static final List<SystemSource> RIDE_SOURCES = List.of(
            new SystemSource("UBER", "Uber", 0), new SystemSource("NINETY_NINE", "99", 1));
    private static final List<SystemSource> DELIVERY_SOURCES = List.of(
            new SystemSource("IFOOD", "iFood", 0), new SystemSource("NINETY_NINE_FOOD", "99Food", 1));

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
        Set<String> profileKeys = systemSources(user).stream().map(SystemSource::key).collect(Collectors.toSet());

        return sourceRepository.findAllByUserIdOrderBySortOrderAscNameAsc(userId).stream()
                .filter(WorkEarningSource::isActive)
                .filter(source -> source.getSystemKey() == null || profileKeys.contains(source.getSystemKey()))
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
    public void deactivate(UUID userId, UUID sourceId, LocalDate date) {
        WorkEarningSource source = sourceRepository.findByIdAndUserId(sourceId, userId)
                .orElseThrow(() -> new NotFoundException("Ganho não encontrado"));
        if (source.getSystemKey() != null) throw new BadRequestException("Os ganhos padrão não podem ser desativados");
        source.deactivate();
        sourceRepository.save(source);
        if (date != null) {
            earningRepository.deleteBySourceAndDate(userId, source.getName().toLowerCase(Locale.ROOT), date);
        }
    }

    private void ensureSystemSource(User user, SystemSource systemSource) {
        if (sourceRepository.existsByUserIdAndSystemKey(user.getId(), systemSource.key())) return;
        WorkEarningSource custom = sourceRepository.findByUserIdAndNameIgnoreCase(user.getId(), systemSource.name())
                .filter(source -> source.getSystemKey() == null)
                .orElse(null);
        if (custom != null) {
            custom.promoteToSystem(systemSource.key(), systemSource.name(), systemSource.sortOrder());
            sourceRepository.save(custom);
            return;
        }
        sourceRepository.save(new WorkEarningSource(user, systemSource.name(), systemSource.key(), systemSource.sortOrder()));
    }

    private void ensureSystemSources(User user) {
        systemSources(user).forEach(source -> ensureSystemSource(user, source));
    }

    private List<SystemSource> systemSources(User user) {
        return user.getUsageProfile() == UsageProfile.DELIVERY ? DELIVERY_SOURCES : RIDE_SOURCES;
    }

    private WorkEarningSourceResponse toResponse(WorkEarningSource source) {
        return new WorkEarningSourceResponse(source.getId(), source.getName(), source.getSystemKey(),
                source.isActive(), source.getSortOrder());
    }
}
