package se.valenzuela.monitoring.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import se.valenzuela.monitoring.settings.service.AppSettingService;

@Route("settings")
@Menu(order = 15, icon = "vaadin:cog", title = "Settings")
public class SettingsView extends Main {

    public SettingsView(AppSettingService appSettingService) {
        addClassNames("view-content", LumoUtility.BoxSizing.BORDER, "scrollable-page");
        setWidthFull();

        var page = new VerticalLayout();
        page.setPadding(true);
        page.setSpacing(false);
        page.setWidthFull();

        page.add(sectionHeader("Monitoring",
                "Global thresholds and behaviour that apply across all monitored services."));
        page.add(buildCertExpiryCard(appSettingService));

        add(page);
    }

    private VerticalLayout buildCertExpiryCard(AppSettingService appSettingService) {
        var card = new VerticalLayout();
        card.addClassNames("channel-card", "channel-card--narrow");
        card.setSpacing(false);
        card.setPadding(false);

        // header
        var title = new Span("Certificate expiry");
        title.addClassName("channel-card-title");
        var header = new HorizontalLayout(title);
        header.addClassName("channel-card-header");
        header.setAlignItems(HorizontalLayout.Alignment.CENTER);
        header.setWidthFull();
        header.setPadding(false);

        // form
        var expiryField = new IntegerField("Warn when expiry is within (days)");
        expiryField.setHelperText("Services with certificates expiring within this threshold are highlighted in amber");
        expiryField.setMin(1);
        expiryField.setMax(365);
        expiryField.setStepButtonsVisible(true);
        expiryField.setValue(appSettingService.getCertExpiryWarningDays());
        expiryField.setWidthFull();

        var form = new VerticalLayout(expiryField);
        form.addClassName("channel-card-form");
        form.setSpacing(false);
        form.setPadding(false);

        // footer — button declared before listener so the lambda can reference it
        var saveButton = new Button("Save", VaadinIcon.CHECK.create());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.setEnabled(false);

        saveButton.addClickListener(_ -> {
            Integer value = expiryField.getValue();
            if (value == null || value < 1) {
                Notification.show("Please enter a valid number of days",
                        3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            appSettingService.setValue(AppSettingService.CERT_EXPIRY_WARNING_DAYS, String.valueOf(value));
            saveButton.setEnabled(false);
            Notification.show("Settings saved",
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

        expiryField.addValueChangeListener(_ ->
                saveButton.setEnabled(expiryField.getValue() != null
                        && !expiryField.getValue().equals(appSettingService.getCertExpiryWarningDays())));

        var footer = new HorizontalLayout(saveButton);
        footer.addClassName("channel-card-footer");
        footer.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);
        footer.setPadding(false);

        card.add(header, new Hr(), form, footer);
        return card;
    }

    private Div sectionHeader(String title, String description) {
        var titleSpan = new Span(title);
        titleSpan.addClassName("notif-section-title");
        var descSpan = new Span(description);
        descSpan.addClassName("notif-section-desc");
        var header = new Div(titleSpan, descSpan);
        header.addClassName("notif-section-header");
        return header;
    }
}
