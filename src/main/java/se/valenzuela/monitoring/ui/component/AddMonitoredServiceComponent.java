package se.valenzuela.monitoring.ui.component;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.Getter;
import org.springframework.stereotype.Component;
import se.valenzuela.monitoring.service.MonitoringService;

import java.net.URI;
import java.net.URISyntaxException;

@Getter
@UIScope
@Component
public class AddMonitoredServiceComponent extends Composite<HorizontalLayout> {

    private final MonitoringService monitoringService;

    private final Span label = new Span("Service instance base URL:");
    private final TextField url = new TextField();

    public AddMonitoredServiceComponent(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
        Button addButton = new Button("+", _ -> addButtonClickListener());

        getContent().add(label, url, addButton);

        url.addValueChangeListener(_ -> url.setInvalid(false));
    }

    public String getUrl() {
        return url.getValue();
    }

    public void setUrl(String value) {
        url.setValue(value);
    }

    public void addButtonClickListener() {
        var serviceUrl = getUrl().trim();
        url.setInvalid(false);
        url.setErrorMessage(null);

        if (serviceUrl.isEmpty()) {
            url.setInvalid(true);
            url.setErrorMessage("Please enter a service URL");
            return;
        }

        try {
            URI uri = new URI(serviceUrl);
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equals("http") || scheme.equals("https"))) {
                throw new URISyntaxException(serviceUrl, "URL must start with http or https");
            }
        } catch (URISyntaxException e) {
            url.setInvalid(true);
            url.setErrorMessage("Invalid URL format");
            return;
        }

        boolean serviceAdded = monitoringService.addService(getUrl());

        if (!serviceAdded) {
            url.setInvalid(true);
            url.setErrorMessage("Service already exists: " + serviceUrl);
        } else {
            url.clear();
        }
    }

}
