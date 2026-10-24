package br.com.amcash.notification.controller;

import br.com.amcash.config.AuthenticatedUser;
import br.com.amcash.config.CurrentUser;
import br.com.amcash.notification.dto.NotificationResponse;
import br.com.amcash.notification.service.FinancialNotificationService;
import br.com.amcash.vehicle.service.VehicleNotificationService;
import br.com.amcash.operationalcost.service.OperationalMileageSuggestionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class FinancialNotificationController {
    private final FinancialNotificationService service;
    private final VehicleNotificationService vehicleService;
    private final OperationalMileageSuggestionService operationalMileageService;

    public FinancialNotificationController(FinancialNotificationService service, VehicleNotificationService vehicleService,OperationalMileageSuggestionService operationalMileageService) { this.service = service; this.vehicleService = vehicleService; this.operationalMileageService=operationalMileageService; }

    @GetMapping
    public List<NotificationResponse> list(@CurrentUser AuthenticatedUser user) { return combined(service.list(user.userId()), vehicleService.list(user.userId()),operationalMileageService.list(user.userId())); }

    @PatchMapping("/read")
    public List<NotificationResponse> markAllRead(@CurrentUser AuthenticatedUser user) { return combined(service.markAllRead(user.userId()), vehicleService.markAllRead(user.userId()),operationalMileageService.markAllRead(user.userId())); }

    private List<NotificationResponse> combined(List<NotificationResponse> financial, List<NotificationResponse> vehicle,List<NotificationResponse> operational) {
        return java.util.stream.Stream.concat(java.util.stream.Stream.concat(financial.stream(), vehicle.stream()),operational.stream())
                .sorted(java.util.Comparator.comparing(NotificationResponse::createdAt).reversed()).toList();
    }
}
