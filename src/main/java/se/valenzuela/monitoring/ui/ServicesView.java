package se.valenzuela.monitoring.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import se.valenzuela.monitoring.core.model.Environment;
import se.valenzuela.monitoring.core.model.MonitoredService;
import se.valenzuela.monitoring.core.service.EnvironmentService;
import se.valenzuela.monitoring.core.service.MonitoringService;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class ServicesView extends VerticalLayout {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final MonitoringService monitoringService;
    private final EnvironmentService environmentService;

    private Set<Environment> environmentFilter = Set.of();
    private List<MonitoredService> currentServices;

    private final HorizontalLayout summaryBar = new HorizontalLayout();
    private final Div cardGrid = new Div();

    public ServicesView(MonitoringService monitoringService, EnvironmentService environmentService) {
        this.monitoringService = monitoringService;
        this.environmentService = environmentService;

        setPadding(false);
        setSpacing(false);
        setSizeFull();

        summaryBar.addClassName("summary-bar");
        summaryBar.setSpacing(true);
        summaryBar.setPadding(false);

        cardGrid.addClassName("service-card-grid");
        cardGrid.setWidthFull();
        cardGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(300px, 1fr))")
                .set("gap", "var(--lumo-space-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("align-content", "start")
                .set("overflow-y", "auto")
                .set("box-sizing", "border-box");

        add(summaryBar);
        addAndExpand(cardGrid);

        currentServices = monitoringService.getServicesForDisplay();
        renderAll(currentServices);

        UI ui = UI.getCurrent();
        Consumer<MonitoredService> listener = _ -> {
            if (ui != null && ui.isAttached()) {
                List<MonitoredService> fresh = monitoringService.getServicesForDisplay();
                ui.access(() -> {
                    currentServices = fresh;
                    renderAll(currentServices);
                });
            }
        };
        monitoringService.addListener(listener);
        addDetachListener(_ -> monitoringService.removeListener(listener));
    }

    public void setEnvironmentFilter(Set<Environment> filter) {
        environmentFilter = filter != null ? filter : Set.of();
        renderAll(currentServices);
    }

    // ── rendering ─────────────────────────────────────────────────────────────

    private void renderAll(List<MonitoredService> all) {
        List<MonitoredService> visible = applyFilter(all);
        renderSummaryBar(visible);
        renderCards(sortByStatus(visible));
    }

    private List<MonitoredService> sortByStatus(List<MonitoredService> services) {
        return services.stream()
                .sorted(Comparator.comparingInt(ServicesView::statusPriority))
                .toList();
    }

    private static int statusPriority(MonitoredService s) {
        if (!s.isHealthStatus()) return 0;      // DOWN first
        if (s.isCertExpiringSoon()) return 1;   // WARNING second
        return 2;                               // HEALTHY last
    }

    private List<MonitoredService> applyFilter(List<MonitoredService> services) {
        if (environmentFilter.isEmpty()) return services;
        return services.stream()
                .filter(s -> s.getEnvironments().stream().anyMatch(environmentFilter::contains))
                .toList();
    }

    private void renderSummaryBar(List<MonitoredService> services) {
        summaryBar.removeAll();
        long healthy = services.stream().filter(s -> s.isHealthStatus() && !s.isCertExpiringSoon()).count();
        long down    = services.stream().filter(s -> !s.isHealthStatus()).count();
        long warning = services.stream().filter(s -> s.isHealthStatus() && s.isCertExpiringSoon()).count();
        summaryBar.add(
                summaryChip("var(--lumo-success-color)", healthy + " Healthy"),
                summaryChip("var(--lumo-error-color)",   down    + " Down"),
                summaryChip("goldenrod",                  warning + " Warning")
        );
    }

    private Span summaryChip(String color, String text) {
        var dot = VaadinIcon.CIRCLE.create();
        dot.setSize("12px");
        dot.setColor(color);
        var chip = new Span(dot, new Span(text));
        chip.addClassName("summary-chip");
        return chip;
    }

    private void renderCards(List<MonitoredService> services) {
        cardGrid.removeAll();
        services.forEach(service -> cardGrid.add(buildCard(service)));
    }

    // ── card ──────────────────────────────────────────────────────────────────

    private Div buildCard(MonitoredService service) {
        var card = new Div();
        card.addClassName("service-card");
        if (!service.isHealthStatus()) {
            card.addClassName("service-card--down");
        } else if (service.isCertExpiringSoon()) {
            card.addClassName("service-card--warning");
        } else {
            card.addClassName("service-card--healthy");
        }

        card.add(buildCardBody(service), buildCardActions(service));
        return card;
    }

    private Div buildCardBody(MonitoredService service) {
        var body = new Div();
        body.addClassName("service-card-body");
        body.addClickListener(_ -> openDetailDialog(service));

        // status dot · name · version
        var dot = statusIcon(service.isHealthStatus(), service.isCertExpiringSoon(), "16px");
        var name = new Span(service.getName() != null ? service.getName() : service.getUrl());
        name.addClassName("service-card-name");
        var version = new Span(service.getVersion() != null ? service.getVersion() : "");
        version.addClassName("service-card-version");
        var header = new Div(dot, name, version);
        header.addClassName("service-card-header");

        // URL — stop click so it doesn't also open the detail dialog
        var urlLink = new Anchor(service.getUrl(), service.getUrl());
        urlLink.setTarget("_blank");
        urlLink.addClassName("service-card-url");
        urlLink.getElement().executeJs("this.addEventListener('click', e => e.stopPropagation())");

        // environment badges
        var badgesRow = new Div();
        badgesRow.addClassName("service-card-badges");
        service.getEnvironments().stream()
                .sorted(Comparator.comparingInt(Environment::getDisplayOrder)
                        .thenComparing(Environment::getName))
                .forEach(env -> badgesRow.add(envBadge(env)));

        // cert expiry + last-checked footer
        var footer = new Div();
        footer.addClassName("service-card-footer");
        if (service.getEarliestCertExpiry() != null) {
            long days = Duration.between(Instant.now(), service.getEarliestCertExpiry()).toDays();
            var certSpan = new Span("Cert · " + days + "d");
            certSpan.addClassName("service-card-cert");
            if (service.isCertExpiringSoon()) certSpan.addClassName("service-card-cert--warning");
            footer.add(certSpan);
        }
        footer.add(buildLiveCheckedSpan(service.getLastUpdated()));

        body.add(header, urlLink, badgesRow, footer);
        return body;
    }

    private HorizontalLayout buildCardActions(MonitoredService service) {
        var editBtn = new Button(VaadinIcon.EDIT.create(), _ -> openEditDialog(service, null));
        editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        editBtn.setTooltipText("Edit");

        var deleteBtn = new Button(VaadinIcon.TRASH.create(), _ -> openDeleteDialog(service, null));
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        deleteBtn.setTooltipText("Delete");

        var actions = new HorizontalLayout(editBtn, deleteBtn);
        actions.addClassName("service-card-actions");
        actions.setSpacing(false);
        return actions;
    }

    // ── detail dialog ─────────────────────────────────────────────────────────

    private void openDetailDialog(MonitoredService service) {
        var dialog = new Dialog();
        dialog.setWidth("480px");
        dialog.setHeaderTitle(service.getName() != null ? service.getName() : service.getUrl());

        var content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);

        // health status + version hero row
        var dot = statusIcon(service.isHealthStatus(), service.isCertExpiringSoon(), "18px");
        String healthColor = !service.isHealthStatus() ? "var(--lumo-error-color)"
                : service.isCertExpiringSoon() ? "goldenrod" : "var(--lumo-success-color)";
        String healthText = service.getHealthResponseStatus() != null
                ? service.getHealthResponseStatus() : (service.isHealthStatus() ? "UP" : "DOWN");
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
                ? DATETIME_FMT.format(service.getLastUpdated()) + "  (" + relativeTime(service.getLastUpdated()) + ")"
                : "—"));
        if (service.getEarliestCertExpiry() != null) {
            long days = Duration.between(Instant.now(), service.getEarliestCertExpiry()).toDays();
            var certSpan = new Span(DATE_FMT.format(service.getEarliestCertExpiry()) + "  (" + days + " days)");
            if (service.isCertExpiringSoon())
                certSpan.getStyle().set("color", "goldenrod").set("font-weight", "600");
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
                    .forEach(env -> envRow.add(envBadge(env)));
            var envSection = new VerticalLayout(sectionLabel("Environments"), envRow);
            envSection.setPadding(false);
            envSection.setSpacing(false);
            content.add(envSection);
        }

        dialog.add(content);

        var editBtn = new Button("Edit", VaadinIcon.EDIT.create(), _ -> {
            dialog.close();
            openEditDialog(service, null);
        });
        editBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var deleteBtn = new Button("Delete", VaadinIcon.TRASH.create(), _ -> {
            dialog.close();
            openDeleteDialog(service, null);
        });
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

        dialog.getFooter().add(new Button("Close", _ -> dialog.close()), deleteBtn, editBtn);
        dialog.open();
    }

    // ── edit dialog ───────────────────────────────────────────────────────────

    private void openEditDialog(MonitoredService service, Dialog parentToClose) {
        var dialog = new Dialog();
        dialog.setWidth("480px");
        dialog.setHeaderTitle("Edit " + (service.getName() != null ? service.getName() : service.getUrl()));

        String originalUrl      = service.getUrl() != null ? service.getUrl() : "";
        String originalInfo     = service.getInfoEndpoint() != null ? service.getInfoEndpoint() : "";
        String originalHealth   = service.getHealthEndpoint() != null ? service.getHealthEndpoint() : "";
        Integer originalInterval = service.getHealthCheckIntervalSeconds();
        Set<Environment> originalEnvs = new HashSet<>(service.getEnvironments());

        var urlField = new TextField("URL");
        urlField.setValue(originalUrl);
        urlField.setWidthFull();

        var infoField = new TextField("Info endpoint");
        infoField.setValue(originalInfo);
        infoField.setWidthFull();

        var healthField = new TextField("Health endpoint");
        healthField.setValue(originalHealth);
        healthField.setWidthFull();

        var intervalField = new IntegerField("Health check interval (seconds)");
        intervalField.setMin(5);
        intervalField.setMax(3600);
        intervalField.setStepButtonsVisible(true);
        intervalField.setClearButtonVisible(true);
        intervalField.setHelperText("Leave empty to use environment / default");
        intervalField.setValue(originalInterval);
        intervalField.setWidthFull();

        var envGroup = new CheckboxGroup<Environment>("Environments");
        envGroup.setItems(environmentService.getAllEnvironments());
        envGroup.setItemLabelGenerator(Environment::getName);
        envGroup.setValue(originalEnvs);
        envGroup.setWidthFull();

        var saveBtn = new Button("Save", VaadinIcon.CHECK.create(), _ -> {
            String newUrl = urlField.getValue().trim();
            if (!newUrl.equals(originalUrl)) {
                monitoringService.updateServiceUrl(service, newUrl);
            }
            service.setInfoEndpoint(infoField.getValue().trim());
            service.setHealthEndpoint(healthField.getValue().trim());
            service.setHealthCheckIntervalSeconds(intervalField.getValue());
            if (service.getId() != null) {
                environmentService.updateServiceEnvironments(service, envGroup.getValue());
            }
            monitoringService.saveService(service);
            monitoringService.notifyListeners(service);
            dialog.close();
            if (parentToClose != null) parentToClose.close();
            Notification.show("Service saved", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.setEnabled(false);

        Runnable dirty = () -> saveBtn.setEnabled(
                !urlField.getValue().equals(originalUrl)
                || !infoField.getValue().equals(originalInfo)
                || !healthField.getValue().equals(originalHealth)
                || !Objects.equals(intervalField.getValue(), originalInterval)
                || !envGroup.getValue().equals(originalEnvs));

        urlField.addValueChangeListener(_ -> dirty.run());
        infoField.addValueChangeListener(_ -> dirty.run());
        healthField.addValueChangeListener(_ -> dirty.run());
        intervalField.addValueChangeListener(_ -> dirty.run());
        envGroup.addValueChangeListener(_ -> dirty.run());

        var content = new VerticalLayout(urlField, infoField, healthField, intervalField, envGroup);
        content.setPadding(false);
        content.setSpacing(true);
        dialog.add(content);
        dialog.getFooter().add(new Button("Cancel", _ -> dialog.close()), saveBtn);
        dialog.open();
    }

    // ── delete dialog ─────────────────────────────────────────────────────────

    private void openDeleteDialog(MonitoredService service, Dialog parentToClose) {
        var confirm = new ConfirmDialog();
        confirm.setHeader("Delete " + (service.getName() != null ? service.getName() : service.getUrl()));
        confirm.setText("This will permanently remove the service and all its configuration.");
        confirm.setCancelable(true);
        confirm.setConfirmText("Delete");
        confirm.setConfirmButtonTheme("error primary");
        confirm.addConfirmListener(_ -> {
            monitoringService.removeService(service);
            if (parentToClose != null) parentToClose.close();
            Notification.show("Service deleted", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        confirm.open();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Icon statusIcon(boolean healthy, boolean warning, String size) {
        var icon = VaadinIcon.CIRCLE.create();
        icon.setSize(size);
        icon.setColor(!healthy ? "var(--lumo-error-color)" : warning ? "goldenrod" : "var(--lumo-success-color)");
        return icon;
    }

    private Span envBadge(Environment env) {
        var badge = new Span(env.getName());
        badge.addClassName("env-badge");
        if (env.getColor() != null && !env.getColor().isBlank()) {
            badge.addClassName("env-badge-colored");
            badge.getStyle().set("background-color", env.getColor());
        } else {
            badge.addClassName("env-badge-default");
        }
        return badge;
    }

    private void addDetailRow(Div grid, String label, com.vaadin.flow.component.Component value) {
        var labelSpan = new Span(label);
        labelSpan.addClassName("detail-label");
        var row = new Div(labelSpan, value);
        row.addClassName("detail-row");
        grid.add(row);
    }

    private Span infoStatusSpan(boolean ok) {
        var dot = VaadinIcon.CIRCLE.create();
        dot.setColor(ok ? "var(--lumo-success-color)" : "var(--lumo-error-color)");
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

    private static Span buildLiveCheckedSpan(Instant lastUpdated) {
        var span = new Span();
        span.addClassName("service-card-time");

        if (lastUpdated == null) {
            span.setText("Never checked");
            return span;
        }

        span.getElement().setAttribute("data-checked-at", String.valueOf(lastUpdated.toEpochMilli()));
        span.setText("Checked " + relativeTime(lastUpdated)); // shown before JS runs

        span.getElement().executeJs("""
            (function(el) {
                if (el._tick) clearInterval(el._tick);
                function fmt(ms) {
                    var s = Math.floor((Date.now() - ms) / 1000);
                    if (s < 60)   return s + 's ago';
                    if (s < 3600) return Math.floor(s / 60) + ' min ago';
                    if (s < 86400) return Math.floor(s / 3600) + 'h ago';
                    return Math.floor(s / 86400) + 'd ago';
                }
                var t = parseInt(el.getAttribute('data-checked-at'));
                el._tick = setInterval(function() {
                    el.textContent = 'Checked ' + fmt(t);
                }, 1000);
            })(this);
            """);

        return span;
    }

    private static String relativeTime(Instant instant) {
        if (instant == null) return "never";
        long seconds = Duration.between(instant, Instant.now()).toSeconds();
        if (seconds < 60) return seconds + "s ago";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + " min ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        return (hours / 24) + "d ago";
    }
}
