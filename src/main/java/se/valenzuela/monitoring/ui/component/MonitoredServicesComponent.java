package se.valenzuela.monitoring.ui.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.Getter;
import org.springframework.stereotype.Component;
import se.valenzuela.monitoring.model.MonitoredService;
import se.valenzuela.monitoring.service.MonitoringService;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.function.Consumer;

@Getter
@UIScope
@Component
public class MonitoredServicesComponent extends Grid<MonitoredService> {

    private final ListDataProvider<MonitoredService> dataProvider;
    private final MonitoringService monitoringService;

    public MonitoredServicesComponent(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;

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

        addColumn(service -> {
            if (service.getLastUpdated() == null) {
                return "-";
            }

            return DateTimeFormatter
                    .ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withLocale(UI.getCurrent().getLocale())
                    .format(service.getLastUpdated().atZone(ZoneId.systemDefault()));
        }).setHeader("Last updated").setSortable(true);
        addColumn(new ComponentRenderer<>(service ->
                new Button(VaadinIcon.TRASH.create(), _ -> openDeleteDialog(service)))
        ).setHeader("Actions");
        setPartNameGenerator(s -> !s.isInfoStatus() ? "unavailable" : null);
        setSizeFull();
        addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        UI ui = UI.getCurrent();
        Consumer<MonitoredService> listener = _ -> {
            if (ui != null && ui.isAttached()) {
                ui.access(() -> {
                    dataProvider.getItems().clear();
                    dataProvider.getItems().addAll(monitoringService.getServices());
                    dataProvider.refreshAll();
                });
            }
        };
        monitoringService.addListener(listener);

        addDetachListener(_ -> monitoringService.removeListener(listener));
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
