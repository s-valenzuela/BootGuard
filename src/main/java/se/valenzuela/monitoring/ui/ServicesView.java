package se.valenzuela.monitoring.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import se.valenzuela.monitoring.core.model.Environment;
import se.valenzuela.monitoring.core.model.MonitoredService;
import se.valenzuela.monitoring.core.service.EnvironmentService;
import se.valenzuela.monitoring.core.service.MonitoringService;
import se.valenzuela.monitoring.ui.component.MonitoredServicesComponent;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ServicesView extends HorizontalLayout {

    private final VerticalLayout detailPanel = new VerticalLayout();
    private final MonitoringService monitoringService;
    private final EnvironmentService environmentService;
    private final MonitoredServicesComponent monitoredServicesComponent;
    private TextField urlField;
    private TextField infoEndpointField;
    private TextField healthEndpointField;
    private IntegerField checkIntervalField;
    private CheckboxGroup<Environment> environmentCheckboxGroup;
    private Button saveButton;

    public ServicesView(MonitoringService monitoringService, EnvironmentService environmentService) {
        this.monitoringService = monitoringService;
        this.environmentService = environmentService;
        setSizeFull();

        monitoredServicesComponent = new MonitoredServicesComponent(monitoringService, environmentService);

        configureDetailPanel();

        add(monitoredServicesComponent, detailPanel);

        setFlexGrow(2, monitoredServicesComponent);
        setFlexGrow(1, detailPanel);

        monitoredServicesComponent.asSingleSelect().addValueChangeListener(e -> {
            MonitoredService service = e.getValue();
            showDetails(service, false);
        });

        monitoredServicesComponent.setEditListener(service -> {
            monitoredServicesComponent.asSingleSelect().setValue(service);
            showDetails(service, true);
        });
    }

    private void configureDetailPanel() {
        detailPanel.setWidth("500px");
        detailPanel.setPadding(true);
        detailPanel.addClassName("detail-panel");
        detailPanel.setVisible(false); // hidden until selection
    }

    private void showDetails(MonitoredService service, boolean editMode) {
        detailPanel.removeAll();

        if (service == null) {
            detailPanel.setVisible(false);
            return;
        }

        detailPanel.setVisible(true);

        String lastUpdated = service.getLastUpdated() != null
                ? DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        .format(service.getLastUpdated().atZone(ZoneId.systemDefault()))
                : "-";

        String originalUrl = service.getUrl() != null ? service.getUrl() : "";
        String originalInfoEndpoint = service.getInfoEndpoint() != null ? service.getInfoEndpoint() : "";
        String originalHealthEndpoint = service.getHealthEndpoint() != null ? service.getHealthEndpoint() : "";
        Integer originalInterval = service.getHealthCheckIntervalSeconds();

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        addFormRow(form, "Name", new Span(service.getName() != null ? service.getName() : "-"));
        addFormRow(form, "Version", new Span(service.getVersion() != null ? service.getVersion() : "-"));

        urlField = new TextField();
        urlField.setValue(originalUrl);
        urlField.setWidthFull();
        urlField.setReadOnly(!editMode);
        addFormRow(form, "URL", urlField);

        addFormRow(form, "Info status", wrapLeft(getStatusIcon(service.isInfoStatus(), false)));
        addFormRow(form, "Health status", wrapLeft(getStatusIcon(service.isHealthStatus(), service.isCertExpiringSoon())));
        if (service.getEarliestCertExpiry() != null) {
            String certExpiry = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    .format(service.getEarliestCertExpiry().atZone(ZoneId.systemDefault()));
            Span certExpirySpan = new Span(certExpiry);
            if (service.isCertExpiringSoon()) {
                certExpirySpan.getStyle().set("color", "goldenrod").set("font-weight", "bold");
            }
            addFormRow(form, "Cert expires", certExpirySpan);
        }
        addFormRow(form, "Last updated", new Span(lastUpdated));

        FormLayout endpointsForm = new FormLayout();
        endpointsForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        infoEndpointField = new TextField();
        infoEndpointField.setValue(originalInfoEndpoint);
        infoEndpointField.setWidthFull();
        infoEndpointField.setReadOnly(!editMode);

        healthEndpointField = new TextField();
        healthEndpointField.setValue(originalHealthEndpoint);
        healthEndpointField.setWidthFull();
        healthEndpointField.setReadOnly(!editMode);

        addFormRow(endpointsForm, "Info endpoint", infoEndpointField);
        addFormRow(endpointsForm, "Health endpoint", healthEndpointField);

        Set<Environment> originalEnvironments = service.getId() != null
                ? environmentService.getEnvironmentsForService(service.getId())
                : new HashSet<>();

        environmentCheckboxGroup = new CheckboxGroup<>("Environments");
        environmentCheckboxGroup.setItems(environmentService.getAllEnvironments());
        environmentCheckboxGroup.setItemLabelGenerator(Environment::getName);
        environmentCheckboxGroup.setValue(originalEnvironments);
        environmentCheckboxGroup.setReadOnly(!editMode);
        environmentCheckboxGroup.setWidthFull();

        checkIntervalField = new IntegerField("Health Check Interval (seconds)");
        checkIntervalField.setMin(5);
        checkIntervalField.setMax(3600);
        checkIntervalField.setStepButtonsVisible(true);
        checkIntervalField.setClearButtonVisible(true);
        checkIntervalField.setHelperText("Leave empty to use environment/default interval");
        checkIntervalField.setValue(originalInterval);
        checkIntervalField.setReadOnly(!editMode);
        checkIntervalField.setWidthFull();

        saveButton = new Button("Save", VaadinIcon.CHECK.create(), _ -> {
            String newUrl = urlField.getValue().trim();
            if (!newUrl.equals(originalUrl)) {
                monitoringService.updateServiceUrl(service, newUrl);
            }
            service.setInfoEndpoint(infoEndpointField.getValue());
            service.setHealthEndpoint(healthEndpointField.getValue());
            service.setHealthCheckIntervalSeconds(checkIntervalField.getValue());
            if (service.getId() != null) {
                environmentService.updateServiceEnvironments(service, environmentCheckboxGroup.getValue());
            }
            monitoringService.saveService(service);
            monitoringService.notifyListeners(service);
            showDetails(service, false);
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.setVisible(editMode);
        saveButton.setEnabled(false);

        Runnable dirtyCheck = () -> {
            boolean changed = !urlField.getValue().equals(originalUrl)
                    || !infoEndpointField.getValue().equals(originalInfoEndpoint)
                    || !healthEndpointField.getValue().equals(originalHealthEndpoint)
                    || !environmentCheckboxGroup.getValue().equals(originalEnvironments)
                    || !Objects.equals(checkIntervalField.getValue(), originalInterval);
            saveButton.setEnabled(changed);
        };
        urlField.addValueChangeListener(_ -> dirtyCheck.run());
        infoEndpointField.addValueChangeListener(_ -> dirtyCheck.run());
        healthEndpointField.addValueChangeListener(_ -> dirtyCheck.run());
        environmentCheckboxGroup.addValueChangeListener(_ -> dirtyCheck.run());
        checkIntervalField.addValueChangeListener(_ -> dirtyCheck.run());

        detailPanel.add(new H3("Details"), form, new H3("Endpoints"), endpointsForm,
                new H3("Environments"), environmentCheckboxGroup,
                new H3("Health Check"), checkIntervalField, saveButton);
    }

    private void addFormRow(FormLayout form, String label, Component value) {
        Span labelSpan = new Span(label);
        labelSpan.addClassName("form-label");
        form.add(labelSpan, value);
    }

    private Icon getStatusIcon(boolean status, boolean warn) {
        Icon statusIcon = VaadinIcon.CIRCLE.create();
        String color;
        if (!status) {
            color = "red";
        } else if (warn) {
            color = "goldenrod";
        } else {
            color = "green";
        }
        statusIcon.setColor(color);
        statusIcon.setSize("22px");
        return statusIcon;
    }

    private Component wrapLeft(Component component) {
        HorizontalLayout wrapper = new HorizontalLayout(component);
        wrapper.setJustifyContentMode(JustifyContentMode.START);
        wrapper.setWidthFull();
        wrapper.setPadding(false);
        wrapper.setMargin(false);
        return wrapper;
    }

    public MonitoredServicesComponent getMonitoredServicesComponent() {
        return monitoredServicesComponent;
    }
}
