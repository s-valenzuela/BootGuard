package se.valenzuela.monitoring.ui.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.Getter;
import org.springframework.stereotype.Component;
import se.valenzuela.monitoring.core.model.Environment;
import se.valenzuela.monitoring.core.model.MonitoredService;
import se.valenzuela.monitoring.core.service.EnvironmentService;
import se.valenzuela.monitoring.core.service.MonitoringService;

import java.net.URI;
import java.net.URISyntaxException;

@Getter
@UIScope
@Component
public class AddMonitoredServiceComponent extends Button {

    private final MonitoringService monitoringService;
    private final EnvironmentService environmentService;

    public AddMonitoredServiceComponent(MonitoringService monitoringService, EnvironmentService environmentService) {
        super(VaadinIcon.PLUS.create());
        this.monitoringService = monitoringService;
        this.environmentService = environmentService;
        addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addClickListener(_ -> openDialog());
    }

    private void openDialog() {
        var dialog = new Dialog();
        dialog.setHeaderTitle("Add Service");
        dialog.setWidth("500px");

        var urlField = new TextField("Service URL");
        urlField.setPlaceholder("http://localhost:8080");
        urlField.setWidthFull();
        urlField.setRequired(true);
        urlField.addValueChangeListener(_ -> urlField.setInvalid(false));

        var infoEndpointField = new TextField("Info Endpoint");
        infoEndpointField.setPlaceholder(MonitoredService.DEFAULT_INFO_ENDPOINT);
        infoEndpointField.setWidthFull();

        var healthEndpointField = new TextField("Health Endpoint");
        healthEndpointField.setPlaceholder(MonitoredService.DEFAULT_HEALTH_ENDPOINT);
        healthEndpointField.setWidthFull();

        var environmentsGroup = new CheckboxGroup<Environment>("Environments");
        environmentsGroup.setItems(environmentService.getAllEnvironments());
        environmentsGroup.setItemLabelGenerator(Environment::getName);
        environmentsGroup.setWidthFull();

        var content = new VerticalLayout(urlField, infoEndpointField, healthEndpointField, environmentsGroup);
        content.setPadding(false);
        content.setSpacing(true);
        dialog.add(content);

        var addButton = new Button("Add", _ -> {
            String serviceUrl = urlField.getValue().trim();
            urlField.setInvalid(false);
            urlField.setErrorMessage(null);

            if (serviceUrl.isEmpty()) {
                urlField.setInvalid(true);
                urlField.setErrorMessage("Please enter a service URL");
                return;
            }

            try {
                URI uri = new URI(serviceUrl);
                String scheme = uri.getScheme();
                if (scheme == null || !(scheme.equals("http") || scheme.equals("https"))) {
                    throw new URISyntaxException(serviceUrl, "URL must start with http or https");
                }
            } catch (URISyntaxException e) {
                urlField.setInvalid(true);
                urlField.setErrorMessage("Invalid URL format");
                return;
            }

            String infoEndpoint = infoEndpointField.getValue().trim();
            String healthEndpoint = healthEndpointField.getValue().trim();

            MonitoredService service = monitoringService.addServiceWithEndpoints(
                    serviceUrl,
                    infoEndpoint.isEmpty() ? null : infoEndpoint,
                    healthEndpoint.isEmpty() ? null : healthEndpoint);

            if (service == null) {
                urlField.setInvalid(true);
                urlField.setErrorMessage("Service already exists: " + serviceUrl);
            } else {
                if (!environmentsGroup.getValue().isEmpty()) {
                    environmentService.updateServiceEnvironments(service, environmentsGroup.getValue());
                }
                dialog.close();
                Notification.show("Service added", 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }
        });
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var cancelButton = new Button("Cancel", _ -> dialog.close());

        dialog.getFooter().add(cancelButton, addButton);
        dialog.open();
    }
}
