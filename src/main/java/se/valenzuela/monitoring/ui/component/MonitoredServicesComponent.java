package se.valenzuela.monitoring.ui.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.Getter;
import org.springframework.stereotype.Component;
import se.valenzuela.monitoring.core.model.Environment;
import se.valenzuela.monitoring.core.model.MonitoredService;
import se.valenzuela.monitoring.core.service.EnvironmentService;
import se.valenzuela.monitoring.core.service.MonitoringService;

import java.util.Comparator;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@Getter
@UIScope
@Component
public class MonitoredServicesComponent extends Grid<MonitoredService> {

    private final ListDataProvider<MonitoredService> dataProvider;
    private final MonitoringService monitoringService;
    private final EnvironmentService environmentService;
    private final Map<Long, Set<Environment>> environmentCache = new HashMap<>();
    private Set<Environment> environmentFilter = Set.of();
    private Consumer<MonitoredService> editListener;

    public MonitoredServicesComponent(MonitoringService monitoringService, EnvironmentService environmentService) {
        this.monitoringService = monitoringService;
        this.environmentService = environmentService;

        dataProvider = new ListDataProvider<>(new ArrayList<>(monitoringService.getServices()));

        setDataProvider(dataProvider);
        addColumn(MonitoredService::getName).setSortable(true).setHeader("Service").setFlexGrow(1);
        addColumn(MonitoredService::getVersion).setAutoWidth(true).setFlexGrow(0).setHeader("Version");
        addColumn(MonitoredService::getUrl).setHeader("URL").setSortable(true).setFlexGrow(1);

        addComponentColumn(service -> {
            Icon icon = VaadinIcon.CIRCLE.create();
            icon.setColor(service.isHealthStatus() ? "green" : "red");
            icon.setSize("22px");
            return icon;
        }).setHeader("Status").setAutoWidth(true).setFlexGrow(0).setSortable(true);

        loadEnvironmentCache();

        addComponentColumn(service -> {
            HorizontalLayout badges = new HorizontalLayout();
            badges.setSpacing(true);
            badges.setPadding(false);
            environmentCache.getOrDefault(service.getId(), Set.of()).stream()
                    .sorted(Comparator.comparingInt(Environment::getDisplayOrder).thenComparing(Environment::getName))
                    .forEach(env -> {
                Span badge = new Span(env.getName());
                badge.addClassName("env-badge");
                if (env.getColor() != null && !env.getColor().isBlank()) {
                    badge.addClassName("env-badge-colored");
                    badge.getStyle().set("background-color", env.getColor());
                } else {
                    badge.addClassName("env-badge-default");
                }
                badges.add(badge);
            });
            return badges;
        }).setHeader("Environments").setAutoWidth(true).setFlexGrow(0);

        addColumn(service -> {
            if (service.getLastUpdated() == null) {
                return "-";
            }

            return DateTimeFormatter
                    .ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withLocale(UI.getCurrent().getLocale())
                    .format(service.getLastUpdated().atZone(ZoneId.systemDefault()));
        }).setHeader("Last updated").setAutoWidth(true).setFlexGrow(0).setSortable(true);
        addColumn(new ComponentRenderer<>(service -> {
            Button editButton = new Button(VaadinIcon.EDIT.create(), _ -> {
                if (editListener != null) {
                    editListener.accept(service);
                }
            });
            Button deleteButton = new Button(VaadinIcon.TRASH.create(), _ -> openDeleteDialog(service));
            HorizontalLayout actions = new HorizontalLayout(editButton, deleteButton);
            actions.setSpacing(true);
            actions.setPadding(false);
            return actions;
        })).setHeader("Actions").setAutoWidth(true).setFlexGrow(0);
        setPartNameGenerator(s -> {
            if (!s.isInfoStatus()) return "unavailable";
            if (s.isCertExpiringSoon()) return "cert-expiring";
            return null;
        });
        setSizeFull();
        addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        UI ui = UI.getCurrent();
        Consumer<MonitoredService> listener = _ -> {
            if (ui != null && ui.isAttached()) {
                var services = monitoringService.getServices();
                loadEnvironmentCache();
                ui.access(() -> {
                    dataProvider.getItems().clear();
                    dataProvider.getItems().addAll(services);
                    applyEnvironmentFilter();
                });
            }
        };
        monitoringService.addListener(listener);

        addDetachListener(_ -> monitoringService.removeListener(listener));
    }

    public void setEnvironmentFilter(Set<Environment> filter) {
        this.environmentFilter = filter != null ? filter : Set.of();
        applyEnvironmentFilter();
    }

    public void setEditListener(Consumer<MonitoredService> editListener) {
        this.editListener = editListener;
    }

    private void loadEnvironmentCache() {
        environmentCache.clear();
        for (MonitoredService service : dataProvider.getItems()) {
            if (service.getId() != null) {
                environmentCache.put(service.getId(), environmentService.getEnvironmentsForService(service.getId()));
            }
        }
    }

    private void applyEnvironmentFilter() {
        if (environmentFilter.isEmpty()) {
            dataProvider.clearFilters();
        } else {
            dataProvider.setFilter(service -> {
                Set<Environment> envs = environmentCache.getOrDefault(service.getId(), Set.of());
                return environmentFilter.stream().anyMatch(envs::contains);
            });
        }
    }

    private void openDeleteDialog(MonitoredService service) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Delete service");

        VerticalLayout content = new VerticalLayout();
        content.add("Are you sure you want to delete:");
        content.add(new Span(service.getName() + " (" + service.getUrl() + ")"));
        dialog.add(content);

        Button confirm = new Button("Delete", _ -> {
            monitoringService.removeService(service);

            dataProvider.refreshAll();
            dialog.close();
        });

        confirm.getElement().getThemeList().add("error primary");

        Button cancel = new Button("Cancel", _ -> dialog.close());

        dialog.getFooter().add(cancel, confirm);

        dialog.open();
    }

}
