package br.com.amcash.notification.controller;

import br.com.amcash.config.AuthenticatedUser;
import br.com.amcash.config.CurrentUser;
import br.com.amcash.notification.dto.NotificationResponse;
import br.com.amcash.notification.service.FinancialNotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class FinancialNotificationController {
    private final FinancialNotificationService service;

    public FinancialNotificationController(FinancialNotificationService service) { this.service = service; }

    @GetMapping
    public List<NotificationResponse> list(@CurrentUser AuthenticatedUser user) { return service.list(user.userId()); }

    @PatchMapping("/read")
    public List<NotificationResponse> markAllRead(@CurrentUser AuthenticatedUser user) { return service.markAllRead(user.userId()); }
}
