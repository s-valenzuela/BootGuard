package se.valenzuela.monitoring.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
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


        var contentDiv = new Div();
        contentDiv.addClassNames(LumoUtility.Flex.GROW, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.AlignItems.CENTER, LumoUtility.JustifyContent.CENTER);

        var centerDiv = new Div(new AddMonitoredServiceComponent(monitoringService), new ServicesView(monitoringService, environmentService));
        centerDiv.addClassNames(LumoUtility.Flex.GROW, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.JustifyContent.CENTER);
        centerDiv.setSizeFull();
        contentDiv.add(centerDiv);


        add(new ViewToolbar("Spring Boot monitoring"));
        add(contentDiv);

    }

}
