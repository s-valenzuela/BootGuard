package se.valenzuela.monitoring.ui;

import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import se.valenzuela.monitoring.model.Environment;
import se.valenzuela.monitoring.service.EnvironmentService;
import se.valenzuela.monitoring.service.MonitoringService;
import se.valenzuela.monitoring.ui.component.AddMonitoredServiceComponent;
import se.valenzuela.monitoring.ui.component.ViewToolbar;

/**
 * This view shows up when a user navigates to the root ('/') of the application.
 */
@Route
@Menu(order = -100, icon = "vaadin:home", title = "Services")
@UIScope
public class MainView extends Main {

    public MainView(MonitoringService monitoringService, EnvironmentService environmentService) {
        addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.BoxSizing.BORDER);
        setSizeFull();

        var servicesView = new ServicesView(monitoringService, environmentService);

        var environmentFilter = new MultiSelectComboBox<Environment>("Environment");
        environmentFilter.setItems(environmentService.getAllEnvironments());
        environmentFilter.setItemLabelGenerator(Environment::getName);
        environmentFilter.setPlaceholder("Filter by environment...");
        environmentFilter.setClearButtonVisible(true);
        environmentFilter.setWidth("300px");
        environmentFilter.addValueChangeListener(_ ->
                servicesView.getMonitoredServicesComponent().setEnvironmentFilter(environmentFilter.getValue()));

        var addButton = new AddMonitoredServiceComponent(monitoringService, environmentService);

        var actionBar = new HorizontalLayout(environmentFilter, addButton);
        actionBar.setWidthFull();
        actionBar.setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);
        actionBar.setAlignItems(HorizontalLayout.Alignment.END);
        actionBar.setPadding(false);

        var contentDiv = new Div();
        contentDiv.addClassNames(LumoUtility.Flex.GROW, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.AlignItems.CENTER, LumoUtility.JustifyContent.CENTER);

        var centerDiv = new Div(servicesView);
        centerDiv.addClassNames(LumoUtility.Flex.GROW, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.JustifyContent.CENTER);
        centerDiv.setSizeFull();
        contentDiv.add(centerDiv);

        add(new ViewToolbar("Spring Boot monitoring"));
        add(actionBar);
        add(contentDiv);

    }

}
