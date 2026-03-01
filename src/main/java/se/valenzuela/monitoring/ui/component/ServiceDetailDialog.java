package se.valenzuela.monitoring.ui.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import se.valenzuela.monitoring.core.client.HealthStatus;
import se.valenzuela.monitoring.core.model.Environment;
import se.valenzuela.monitoring.core.model.MonitoredService;
import se.valenzuela.monitoring.core.service.EnvironmentService;
import se.valenzuela.monitoring.core.service.MonitoringService;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Set;

public class ServiceDetailDialog extends BaseDialog {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    public ServiceDetailDialog(MonitoredService service, MonitoringService monitoringService,
                               EnvironmentService environmentService) {
        super(service.getName() != null ? service.getName() : service.getUrl());

        var content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);

        // health status + version hero row
        var dot = ServiceViewUtils.statusIcon(service.isHealthStatus(), service.isCertExpiringSoon(), "18px");
        String healthColor = !service.isHealthStatus() ? ServiceViewUtils.COLOR_ERROR
                : service.isCertExpiringSoon() ? ServiceViewUtils.COLOR_WARNING : ServiceViewUtils.COLOR_SUCCESS;
        String healthText = service.getHealthResponseStatus() != null
                ? service.getHealthResponseStatus()
                : (service.isHealthStatus() ? HealthStatus.UP : HealthStatus.DOWN);
        var healthLabel = new Span(healthText);
        healthLabel.getStyle().set("font-weight", "600").set("color", healthColor);
        var versionLabel = new Span(service.getVersion() != null ? "v" + service.getVersion() : "");
        versionLabel.addClassName("service-card-version");
        var statusRow = new HorizontalLayout(dot, healthLabel, versionLabel);
        statusRow.setAlignItems(HorizontalLayout.Alignment.CENTER);
        statusRow.setSpacing(true);

        var urlLink = new Anchor(service.getUrl(), service.getUrl());
        urlLink.setTarget("_blank");
        urlLink.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-primary-color)");

        content.add(statusRow, urlLink, new Hr());

        // status details
        var detailsGrid = new Div();
        detailsGrid.addClassName("detail-grid");
        addDetailRow(detailsGrid, "Info status", infoStatusSpan(service.isInfoStatus()));
        addDetailRow(detailsGrid, "Last check", new Span(service.getLastUpdated() != null
                ? DATETIME_FMT.format(service.getLastUpdated()) + "  (" + ServiceViewUtils.relativeTime(service.getLastUpdated()) + ")"
                : "—"));
        if (service.getEarliestCertExpiry() != null) {
            long days = Duration.between(Instant.now(), service.getEarliestCertExpiry()).toDays();
            var certSpan = new Span(DATE_FMT.format(service.getEarliestCertExpiry()) + "  (" + days + " days)");
            if (service.isCertExpiringSoon())
                certSpan.getStyle().set("color", ServiceViewUtils.COLOR_WARNING).set("font-weight", "600");
            addDetailRow(detailsGrid, "Cert expires", certSpan);
        }
        content.add(detailsGrid, new Hr());

        // endpoints + interval
        var endpointsGrid = new Div();
        endpointsGrid.addClassName("detail-grid");
        addDetailRow(endpointsGrid, "Info endpoint", new Span(service.getInfoEndpoint()));
        addDetailRow(endpointsGrid, "Health endpoint", new Span(service.getHealthEndpoint()));
        int effectiveInterval = service.getEffectiveHealthCheckIntervalSeconds();
        String intervalLabel = service.getHealthCheckIntervalSeconds() != null
                ? effectiveInterval + "s"
                : effectiveInterval + "s (default)";
        addDetailRow(endpointsGrid, "Check interval", new Span(intervalLabel));
        content.add(endpointsGrid);

        // environments
        Set<Environment> envs = service.getEnvironments();
        if (!envs.isEmpty()) {
            content.add(new Hr());
            var envRow = new Div();
            envRow.addClassName("detail-env-row");
            envs.stream()
                    .sorted(Comparator.comparingInt(Environment::getDisplayOrder)
                            .thenComparing(Environment::getName))
                    .forEach(env -> envRow.add(ServiceViewUtils.envBadge(env)));
            var envSection = new VerticalLayout(sectionLabel("Environments"), envRow);
            envSection.setPadding(false);
            envSection.setSpacing(false);
            content.add(envSection);
        }

        add(content);

        var editBtn = new Button("Edit", VaadinIcon.EDIT.create(), _ -> {
            close();
            new ServiceEditDialog(service, monitoringService, environmentService).open();
        });
        editBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var deleteBtn = new Button("Delete", VaadinIcon.TRASH.create(), _ -> {
            close();
            showDeleteConfirm(service, monitoringService);
        });
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

        getFooter().add(closeButton(), deleteBtn, editBtn);
    }

    private static void showDeleteConfirm(MonitoredService service, MonitoringService monitoringService) {
        var confirm = new ConfirmDialog();
        confirm.setHeader("Delete " + (service.getName() != null ? service.getName() : service.getUrl()));
        confirm.setText("This will permanently remove the service and all its configuration.");
        confirm.setCancelable(true);
        confirm.setConfirmText("Delete");
        confirm.setConfirmButtonTheme("error primary");
        confirm.addConfirmListener(_ -> {
            monitoringService.removeService(service);
            Notification.show("Service deleted", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        confirm.open();
    }

    private void addDetailRow(Div grid, String label, Component value) {
        var labelSpan = new Span(label);
        labelSpan.addClassName("detail-label");
        var row = new Div(labelSpan, value);
        row.addClassName("detail-row");
        grid.add(row);
    }

    private Span infoStatusSpan(boolean ok) {
        var dot = VaadinIcon.CIRCLE.create();
        dot.setColor(ok ? ServiceViewUtils.COLOR_SUCCESS : ServiceViewUtils.COLOR_ERROR);
        dot.setSize("14px");
        var span = new Span(dot, new Span(ok ? "OK" : "Unavailable"));
        span.getStyle().set("display", "inline-flex").set("align-items", "center").set("gap", "4px");
        return span;
    }

    private Span sectionLabel(String text) {
        var label = new Span(text);
        label.addClassName("detail-label");
        return label;
    }
}
