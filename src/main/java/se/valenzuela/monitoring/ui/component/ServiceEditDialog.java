package se.valenzuela.monitoring.ui.component;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import se.valenzuela.monitoring.core.model.Environment;
import se.valenzuela.monitoring.core.model.MonitoredService;
import se.valenzuela.monitoring.core.service.EnvironmentService;
import se.valenzuela.monitoring.core.service.MonitoringService;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ServiceEditDialog extends BaseDialog {

    public ServiceEditDialog(MonitoredService service, MonitoringService monitoringService,
                             EnvironmentService environmentService) {
        super("Edit " + (service.getName() != null ? service.getName() : service.getUrl()));

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
            close();
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

        attachDirtyCheck(dirty, urlField, infoField, healthField, intervalField, envGroup);

        var content = new VerticalLayout(urlField, infoField, healthField, intervalField, envGroup);
        content.setPadding(false);
        content.setSpacing(true);
        add(content);
        getFooter().add(cancelButton(), saveBtn);
    }

    @SuppressWarnings("unchecked")
    private static void attachDirtyCheck(Runnable check, HasValue<?, ?>... fields) {
        for (var field : fields) {
            ((HasValue<HasValue.ValueChangeEvent<Object>, Object>) field)
                    .addValueChangeListener(_ -> check.run());
        }
    }
}
