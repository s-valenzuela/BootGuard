package se.valenzuela.monitoring.ui.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import se.valenzuela.monitoring.core.client.LoggersResponse;
import se.valenzuela.monitoring.core.model.MonitoredService;
import se.valenzuela.monitoring.core.service.MonitoringService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ServiceLoggersDialog extends BaseDialog {

    private record LoggerRow(String name, String effectiveLevel, String configuredLevel) {}

    private static final List<String> LOG_LEVELS = List.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR", "OFF");

    public ServiceLoggersDialog(MonitoredService service, MonitoringService monitoringService) {
        super("Loggers — " + (service.getName() != null ? service.getName() : service.getUrl()), WIDTH_WIDE);
        setHeight("70vh");
        build(service, monitoringService);
    }

    private void build(MonitoredService service, MonitoringService monitoringService) {
        LoggersResponse response;
        try {
            response = monitoringService.fetchLoggers(service);
        } catch (Exception e) {
            var msg = new VerticalLayout(
                    new Span("Could not reach /actuator/loggers on this service."),
                    new Span("Make sure the loggers endpoint is exposed in the service\u2019s actuator config."));
            msg.setPadding(false);
            add(msg);
            getFooter().add(closeButton());
            return;
        }

        if (response == null || response.loggers() == null) {
            add(new Span("No logger data returned."));
            getFooter().add(closeButton());
            return;
        }

        List<LoggerRow> allRows = response.loggers().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new LoggerRow(e.getKey(),
                        e.getValue().effectiveLevel(),
                        e.getValue().configuredLevel()))
                .toList();

        var dataProvider = new ListDataProvider<>(new ArrayList<>(allRows));
        dataProvider.setFilter(r -> r.configuredLevel() != null);

        var filterField = new TextField();
        filterField.setPlaceholder("Search loggers\u2026");
        filterField.setPrefixComponent(VaadinIcon.SEARCH.create());
        filterField.setClearButtonVisible(true);
        filterField.setWidthFull();
        filterField.addValueChangeListener(e -> {
            String text = e.getValue().trim().toLowerCase();
            if (text.isEmpty()) {
                dataProvider.setFilter(r -> r.configuredLevel() != null);
            } else {
                dataProvider.setFilter(r -> r.name().toLowerCase().contains(text));
            }
        });

        var hint = new Span("Showing loggers with an explicit level. Search to find any logger.");
        hint.addClassName("logger-hint");

        var grid = new Grid<LoggerRow>();
        grid.setDataProvider(dataProvider);
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER);

        grid.addColumn(LoggerRow::name)
                .setHeader("Logger")
                .setFlexGrow(1)
                .setSortable(true);

        grid.addComponentColumn(row -> levelBadge(row.effectiveLevel()))
                .setHeader("Effective")
                .setWidth("105px")
                .setFlexGrow(0);

        grid.addComponentColumn(row -> {
            var select = new Select<String>();
            var items = new ArrayList<String>();
            items.add("Inherit");
            items.addAll(LOG_LEVELS);
            select.setItems(items);
            select.setValue(row.configuredLevel() != null ? row.configuredLevel() : "Inherit");
            select.setWidth("110px");

            var applyBtn = new Button("Apply");
            applyBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            applyBtn.addClickListener(_ -> {
                String selected = select.getValue();
                String level = "Inherit".equals(selected) ? null : selected;
                try {
                    monitoringService.setLoggerLevel(service, row.name(), level);
                    Notification.show("Level updated", 2000, Notification.Position.BOTTOM_START)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } catch (Exception ex) {
                    Notification.show("Failed to set level: " + ex.getMessage(),
                            3000, Notification.Position.BOTTOM_START)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });

            var cell = new HorizontalLayout(select, applyBtn);
            cell.setAlignItems(HorizontalLayout.Alignment.CENTER);
            cell.setSpacing(true);
            cell.setPadding(false);
            return cell;
        }).setHeader("Set level").setWidth("220px").setFlexGrow(0);

        var content = new VerticalLayout(filterField, hint, grid);
        content.setSizeFull();
        content.setPadding(false);
        content.setSpacing(true);
        content.setFlexGrow(1, grid);

        add(content);
        getFooter().add(closeButton());
    }

    private static Span levelBadge(String level) {
        var badge = new Span(level != null ? level : "—");
        badge.addClassName("level-badge");
        if (level != null) badge.addClassName("level-badge--" + level.toLowerCase());
        return badge;
    }
}
