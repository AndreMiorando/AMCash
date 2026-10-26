package br.com.amcash.work.service;

import br.com.amcash.user.entity.UsageProfile;
import br.com.amcash.user.entity.User;
import br.com.amcash.user.repository.UserRepository;
import br.com.amcash.work.entity.WorkEarningSource;
import br.com.amcash.work.repository.WorkEarningRepository;
import br.com.amcash.work.repository.WorkEarningSourceRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkEarningSourceServiceTests {

    @Test
    void shouldCreateAReusableSourceAfterTheExistingOnes() {
        WorkEarningSourceRepository sourceRepository = mock(WorkEarningSourceRepository.class);
        WorkEarningRepository earningRepository = mock(WorkEarningRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(sourceRepository.findByUserIdAndNameIgnoreCase(userId, "Particular")).thenReturn(Optional.empty());
        when(sourceRepository.maxSortOrder(userId)).thenReturn(3);
        when(sourceRepository.save(any(WorkEarningSource.class))).thenAnswer(invocation -> invocation.getArgument(0));
        WorkEarningSourceService service = new WorkEarningSourceService(sourceRepository, earningRepository, userRepository);

        var created = service.create(userId, "  Particular  ");

        assertEquals("Particular", created.name());
        assertEquals(4, created.sortOrder());
        assertEquals(true, created.active());
    }

    @Test
    void shouldRenameTheSourceInTheCatalogAndItsHistory() {
        WorkEarningSourceRepository sourceRepository = mock(WorkEarningSourceRepository.class);
        WorkEarningRepository earningRepository = mock(WorkEarningRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        UUID userId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        WorkEarningSource source = new WorkEarningSource(mock(User.class), "Particulr", null, 2);
        when(sourceRepository.findByIdAndUserId(sourceId, userId)).thenReturn(Optional.of(source));
        when(sourceRepository.findByUserIdAndNameIgnoreCase(userId, "Particular")).thenReturn(Optional.empty());
        when(sourceRepository.save(source)).thenReturn(source);
        WorkEarningSourceService service = new WorkEarningSourceService(sourceRepository, earningRepository, userRepository);

        var updated = service.rename(userId, sourceId, " Particular ");

        assertEquals("Particular", updated.name());
        verify(earningRepository).renameSource(userId, "particulr", "Particular");
    }

    @Test
    void shouldDeactivateTheSourceAndDeleteItsEarningOnTheRequestedDate() {
        WorkEarningSourceRepository sourceRepository = mock(WorkEarningSourceRepository.class);
        WorkEarningRepository earningRepository = mock(WorkEarningRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        UUID userId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 10, 26);
        WorkEarningSource source = new WorkEarningSource(mock(User.class), "Teste", null, 2);
        when(sourceRepository.findByIdAndUserId(sourceId, userId)).thenReturn(Optional.of(source));
        WorkEarningSourceService service = new WorkEarningSourceService(sourceRepository, earningRepository, userRepository);

        service.deactivate(userId, sourceId, date);

        verify(sourceRepository).save(source);
        verify(earningRepository).deleteBySourceAndDate(userId, "teste", date);
        assertEquals(false, source.isActive());
    }

    @Test
    void shouldListDeliveryPlatformsAndKeepCustomSourcesForDeliveryProfile() {
        WorkEarningSourceRepository sourceRepository = mock(WorkEarningSourceRepository.class);
        WorkEarningRepository earningRepository = mock(WorkEarningRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getUsageProfile()).thenReturn(UsageProfile.DELIVERY);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        WorkEarningSource customIfood = new WorkEarningSource(user, "ifood", null, 3);
        when(sourceRepository.findByUserIdAndNameIgnoreCase(userId, "iFood")).thenReturn(Optional.of(customIfood));
        when(sourceRepository.findByUserIdAndNameIgnoreCase(userId, "99Food")).thenReturn(Optional.empty());
        when(sourceRepository.findAllByUserIdOrderBySortOrderAscNameAsc(userId)).thenReturn(List.of(
                new WorkEarningSource(user, "Uber", "UBER", 0), customIfood,
                new WorkEarningSource(user, "99Food", "NINETY_NINE_FOOD", 1), new WorkEarningSource(user, "Particular", null, 4)));
        WorkEarningSourceService service = new WorkEarningSourceService(sourceRepository, earningRepository, userRepository);

        var sources = service.list(userId);

        assertEquals(List.of("iFood", "99Food", "Particular"), sources.stream().map(source -> source.name()).toList());
        assertEquals("IFOOD", customIfood.getSystemKey());
    }
}
