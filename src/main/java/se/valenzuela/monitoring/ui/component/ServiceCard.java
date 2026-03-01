package se.valenzuela.monitoring.ui.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import se.valenzuela.monitoring.core.model.Environment;
import se.valenzuela.monitoring.core.model.MonitoredService;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;

public class ServiceCard extends Div {

    public ServiceCard(MonitoredService service, Runnable onDetail, Runnable onEdit,
                       Runnable onLoggers, Runnable onDelete) {
        addClassName("service-card");
        if (!service.isHealthStatus()) {
            addClassName("service-card--down");
        } else if (service.isCertExpiringSoon()) {
            addClassName("service-card--warning");
        } else {
            addClassName("service-card--healthy");
        }
        add(buildCardBody(service, onDetail), buildCardActions(onEdit, onLoggers, onDelete));
    }

    private Div buildCardBody(MonitoredService service, Runnable onDetail) {
        var body = new Div();
        body.addClassName("service-card-body");
        body.addClickListener(_ -> onDetail.run());

        var dot = ServiceViewUtils.statusIcon(service.isHealthStatus(), service.isCertExpiringSoon());
        var name = new Span(service.getName() != null ? service.getName() : service.getUrl());
        name.addClassName("service-card-name");
        var version = new Span(service.getVersion() != null ? service.getVersion() : "");
        version.addClassName("service-card-version");
        var header = new Div(dot, name, version);
        header.addClassName("service-card-header");

        var urlLink = new Anchor(service.getUrl(), service.getUrl());
        urlLink.setTarget("_blank");
        urlLink.addClassName("service-card-url");
        urlLink.getElement().executeJs("this.addEventListener('click', e => e.stopPropagation())");

        var badgesRow = new Div();
        badgesRow.addClassName("service-card-badges");
        service.getEnvironments().stream()
                .sorted(Comparator.comparingInt(Environment::getDisplayOrder)
                        .thenComparing(Environment::getName))
                .forEach(env -> badgesRow.add(ServiceViewUtils.envBadge(env)));

        var footer = new Div();
        footer.addClassName("service-card-footer");
        if (service.getEarliestCertExpiry() != null) {
            long days = Duration.between(Instant.now(), service.getEarliestCertExpiry()).toDays();
            var certSpan = new Span("Cert · " + days + "d");
            certSpan.addClassName("service-card-cert");
            if (service.isCertExpiringSoon()) certSpan.addClassName("service-card-cert--warning");
            footer.add(certSpan);
        }
        footer.add(ServiceViewUtils.buildLiveCheckedSpan(service.getLastUpdated()));

        body.add(header, urlLink, badgesRow, footer);
        return body;
    }

    private HorizontalLayout buildCardActions(Runnable onEdit, Runnable onLoggers, Runnable onDelete) {
        var editBtn = new Button(VaadinIcon.EDIT.create(), _ -> onEdit.run());
        editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        editBtn.setTooltipText("Edit");

        var loggersBtn = new Button(VaadinIcon.LIST.create(), _ -> onLoggers.run());
        loggersBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        loggersBtn.setTooltipText("Loggers");

        var deleteBtn = new Button(VaadinIcon.TRASH.create(), _ -> onDelete.run());
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        deleteBtn.setTooltipText("Delete");

        var actions = new HorizontalLayout(editBtn, loggersBtn, deleteBtn);
        actions.addClassName("service-card-actions");
        actions.setSpacing(false);
        return actions;
    }
}
