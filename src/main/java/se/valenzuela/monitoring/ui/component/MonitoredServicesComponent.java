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
import se.valenzuela.monitoring.model.Environment;
import se.valenzuela.monitoring.model.MonitoredService;
import se.valenzuela.monitoring.service.EnvironmentService;
import se.valenzuela.monitoring.service.MonitoringService;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Set;
import java.util.function.Consumer;

@Getter
@UIScope
@Component
public class MonitoredServicesComponent extends Grid<MonitoredService> {

    private final ListDataProvider<MonitoredService> dataProvider;
    private final MonitoringService monitoringService;
    private final EnvironmentService environmentService;
    private Consumer<MonitoredService> editListener;

    public MonitoredServicesComponent(MonitoringService monitoringService, EnvironmentService environmentService) {
        this.monitoringService = monitoringService;
        this.environmentService = environmentService;

        dataProvider = new ListDataProvider<>(new ArrayList<>(monitoringService.getServices()));

        setDataProvider(dataProvider);
        addColumn(MonitoredService::getName).setSortable(true).setHeader("Service");
        addColumn(MonitoredService::getVersion).setHeader("Version");
        addColumn(MonitoredService::getUrl).setHeader("URL").setSortable(true);

        addComponentColumn(service -> {
            Icon icon = VaadinIcon.CIRCLE.create();
            icon.setColor(service.isHealthStatus() ? "green" : "red");
            icon.setSize("22px");
            return icon;
        }).setHeader("Status").setAutoWidth(true).setFlexGrow(0).setSortable(true);

        addComponentColumn(service -> {
            HorizontalLayout badges = new HorizontalLayout();
            badges.setSpacing(true);
            badges.setPadding(false);
            if (service.getId() != null) {
                Set<Environment> envs = environmentService.getEnvironmentsForService(service.getId());
                for (Environment env : envs) {
                    Span badge = new Span(env.getName());
                    badge.getStyle()
                            .set("font-size", "var(--lumo-font-size-xs)")
                            .set("padding", "2px 8px")
                            .set("border-radius", "var(--lumo-border-radius-s)");
                    if (env.getColor() != null && !env.getColor().isBlank()) {
                        badge.getStyle()
                                .set("background-color", env.getColor())
                                .set("color", "white");
                    } else {
                        badge.getStyle()
                                .set("background-color", "var(--lumo-contrast-10pct)")
                                .set("color", "var(--lumo-body-text-color)");
                    }
                    badges.add(badge);
                }
            }
            return badges;
        }).setHeader("Environments").setAutoWidth(true);

        addColumn(service -> {
            if (service.getLastUpdated() == null) {
                return "-";
            }

            return DateTimeFormatter
                    .ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withLocale(UI.getCurrent().getLocale())
                    .format(service.getLastUpdated().atZone(ZoneId.systemDefault()));
        }).setHeader("Last updated").setSortable(true);
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
        })).setHeader("Actions");
        setPartNameGenerator(s -> !s.isInfoStatus() ? "unavailable" : null);
        setSizeFull();
        addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        UI ui = UI.getCurrent();
        Consumer<MonitoredService> listener = _ -> {
            if (ui != null && ui.isAttached()) {
                var services = monitoringService.getServices();
                ui.access(() -> {
                    dataProvider.getItems().clear();
                    dataProvider.getItems().addAll(services);
                    dataProvider.refreshAll();
                });
            }
        };
        monitoringService.addListener(listener);

        addDetachListener(_ -> monitoringService.removeListener(listener));
    }

    public void setEditListener(Consumer<MonitoredService> editListener) {
        this.editListener = editListener;
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
