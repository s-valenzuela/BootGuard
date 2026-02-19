package se.valenzuela.monitoring.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import se.valenzuela.monitoring.service.AppSettingService;
import se.valenzuela.monitoring.ui.component.ViewToolbar;

@Route("settings")
@Menu(order = 15, icon = "vaadin:cog", title = "Settings")
public class SettingsView extends Main {

    public SettingsView(AppSettingService appSettingService) {
        addClassNames("view-content", LumoUtility.BoxSizing.BORDER, "scrollable-page");
        setWidthFull();

        add(new ViewToolbar("Settings"));

        var content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.setWidthFull();

        var card = new VerticalLayout();
        card.addClassNames("channel-card");
        card.setSpacing(false);

        var expiryField = new IntegerField("Certificate expiry warning (days)");
        expiryField.setHelperText("Services with certificates expiring within this many days will be highlighted");
        expiryField.setMin(1);
        expiryField.setMax(365);
        expiryField.setStepButtonsVisible(true);
        expiryField.setValue(appSettingService.getCertExpiryWarningDays());
        expiryField.setWidthFull();

        var saveButton = new Button("Save", VaadinIcon.CHECK.create(), _ -> {
            Integer value = expiryField.getValue();
            if (value == null || value < 1) {
                Notification.show("Please enter a valid number of days",
                        3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            appSettingService.setValue(AppSettingService.CERT_EXPIRY_WARNING_DAYS, String.valueOf(value));
            Notification.show("Settings saved",
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        card.add(expiryField, saveButton);
        content.add(card);
        add(content);
    }
}
