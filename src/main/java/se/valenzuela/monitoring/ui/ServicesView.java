package se.valenzuela.monitoring.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import se.valenzuela.monitoring.model.MonitoredService;
import se.valenzuela.monitoring.service.MonitoringService;
import se.valenzuela.monitoring.ui.component.MonitoredServicesComponent;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ServicesView extends HorizontalLayout {

    private final VerticalLayout detailPanel = new VerticalLayout();
    private final MonitoringService monitoringService;
    private TextField urlField;
    private TextField infoEndpointField;
    private TextField healthEndpointField;
    private Button saveButton;

    public ServicesView(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
        setSizeFull();

        MonitoredServicesComponent monitoredServicesComponent = new MonitoredServicesComponent(monitoringService);

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
        detailPanel.getStyle().set("border-left", "1px solid var(--lumo-contrast-10pct)");
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

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        addFormRow(form, "Name", new Span(service.getName() != null ? service.getName() : "-"));
        addFormRow(form, "Version", new Span(service.getVersion() != null ? service.getVersion() : "-"));

        urlField = new TextField();
        urlField.setValue(originalUrl);
        urlField.setWidthFull();
        urlField.setReadOnly(!editMode);
        addFormRow(form, "URL", urlField);

        addFormRow(form, "Info status", wrapLeft(getStatusIcon(service.isInfoStatus())));
        addFormRow(form, "Health status", wrapLeft(getStatusIcon(service.isHealthStatus())));
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

        saveButton = new Button("Save", VaadinIcon.CHECK.create(), _ -> {
            String newUrl = urlField.getValue().trim();
            if (!newUrl.equals(originalUrl)) {
                monitoringService.updateServiceUrl(service, newUrl);
            }
            service.setInfoEndpoint(infoEndpointField.getValue());
            service.setHealthEndpoint(healthEndpointField.getValue());
            showDetails(service, false);
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.setVisible(editMode);
        saveButton.setEnabled(false);

        Runnable dirtyCheck = () -> {
            boolean changed = !urlField.getValue().equals(originalUrl)
                    || !infoEndpointField.getValue().equals(originalInfoEndpoint)
                    || !healthEndpointField.getValue().equals(originalHealthEndpoint);
            saveButton.setEnabled(changed);
        };
        urlField.addValueChangeListener(_ -> dirtyCheck.run());
        infoEndpointField.addValueChangeListener(_ -> dirtyCheck.run());
        healthEndpointField.addValueChangeListener(_ -> dirtyCheck.run());

        detailPanel.add(new H3("Details"), form, new H3("Endpoints"), endpointsForm, saveButton);
    }

    private void addFormRow(FormLayout form, String label, Component value) {
        Span labelSpan = new Span(label);
        labelSpan.getStyle().set("font-weight", "500").set("color", "var(--lumo-secondary-text-color)");
        form.add(labelSpan, value);
    }

    private Icon getStatusIcon(boolean status) {
        Icon statusIcon = VaadinIcon.CIRCLE.create();
        statusIcon.setColor(status ? "green" : "red");
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
}
