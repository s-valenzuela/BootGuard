package se.valenzuela.monitoring.ui;

import com.vaadin.flow.component.Component;
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

    public ServicesView(MonitoringService monitoringService) {
        setSizeFull();

        MonitoredServicesComponent monitoredServicesComponent = new MonitoredServicesComponent(monitoringService);

        configureDetailPanel();

        add(monitoredServicesComponent, detailPanel);

        setFlexGrow(2, monitoredServicesComponent);
        setFlexGrow(1, detailPanel);

        monitoredServicesComponent.asSingleSelect().addValueChangeListener(e -> {
            MonitoredService service = e.getValue();
            showDetails(service);
        });
    }

    private void configureDetailPanel() {
        detailPanel.setWidth("500px");
        detailPanel.setPadding(true);
        detailPanel.getStyle().set("border-left", "1px solid var(--lumo-contrast-10pct)");
        detailPanel.setVisible(false); // hidden until selection
    }

    private void showDetails(MonitoredService service) {
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

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        addFormRow(form, "Name", new Span(service.getName() != null ? service.getName() : "-"));
        addFormRow(form, "Version", new Span(service.getVersion() != null ? service.getVersion() : "-"));
        addFormRow(form, "URL", new Span(service.getUrl() != null ? service.getUrl() : "-"));
        addFormRow(form, "Info status", wrapLeft(getStatusIcon(service.isInfoStatus())));
        addFormRow(form, "Health status", wrapLeft(getStatusIcon(service.isHealthStatus())));
        addFormRow(form, "Last updated", new Span(lastUpdated));

        FormLayout endpointsForm = new FormLayout();
        endpointsForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        TextField infoEndpoint = new TextField();
        infoEndpoint.setValue(service.getInfoEndpoint() != null ? service.getInfoEndpoint() : "");
        infoEndpoint.setWidthFull();
        infoEndpoint.addValueChangeListener(e -> service.setInfoEndpoint(e.getValue()));

        TextField healthEndpoint = new TextField();
        healthEndpoint.setValue(service.getHealthEndpoint() != null ? service.getHealthEndpoint() : "");
        healthEndpoint.setWidthFull();
        healthEndpoint.addValueChangeListener(e -> service.setHealthEndpoint(e.getValue()));

        addFormRow(endpointsForm, "Info endpoint", infoEndpoint);
        addFormRow(endpointsForm, "Health endpoint", healthEndpoint);

        detailPanel.add(new H3("Details"), form, new H3("Endpoints"), endpointsForm);
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
