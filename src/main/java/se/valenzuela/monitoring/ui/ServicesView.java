package se.valenzuela.monitoring.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import se.valenzuela.monitoring.core.model.Environment;
import se.valenzuela.monitoring.core.model.MonitoredService;
import se.valenzuela.monitoring.core.service.EnvironmentService;
import se.valenzuela.monitoring.core.service.MonitoringService;
import se.valenzuela.monitoring.ui.component.ServiceCard;
import se.valenzuela.monitoring.ui.component.ServiceDetailDialog;
import se.valenzuela.monitoring.ui.component.ServiceEditDialog;
import se.valenzuela.monitoring.ui.component.ServiceLoggersDialog;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class ServicesView extends VerticalLayout {

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
        services.forEach(service -> cardGrid.add(new ServiceCard(service,
                () -> new ServiceDetailDialog(service, monitoringService, environmentService).open(),
                () -> new ServiceEditDialog(service, monitoringService, environmentService).open(),
                () -> new ServiceLoggersDialog(service, monitoringService).open(),
                () -> openDeleteDialog(service))));
    }

    // ── delete dialog ─────────────────────────────────────────────────────────

    private void openDeleteDialog(MonitoredService service) {
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
}
