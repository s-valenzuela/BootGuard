package se.valenzuela.monitoring.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import se.valenzuela.monitoring.model.MonitoredService;
import se.valenzuela.monitoring.notification.channel.NotificationChannel;
import se.valenzuela.monitoring.notification.config.NotificationChannelConfig;
import se.valenzuela.monitoring.notification.config.NotificationServiceOverride;
import se.valenzuela.monitoring.notification.event.ServiceHealthChangedEvent;
import se.valenzuela.monitoring.notification.service.NotificationConfigService;
import se.valenzuela.monitoring.service.MonitoringService;
import se.valenzuela.monitoring.ui.component.ViewToolbar;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Route("notifications")
@Menu(order = 10, icon = "vaadin:bell", title = "Notifications")
public class NotificationSettingsView extends Main {

    private final List<NotificationChannel> channels;
    private final NotificationConfigService configService;
    private final MonitoringService monitoringService;

    public NotificationSettingsView(List<NotificationChannel> channels,
                                    NotificationConfigService configService,
                                    MonitoringService monitoringService) {
        this.channels = channels;
        this.configService = configService;
        this.monitoringService = monitoringService;

        addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.BoxSizing.BORDER);
        setSizeFull();

        add(new ViewToolbar("Notification Settings"));

        var content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.setSizeFull();

        content.add(new H3("Channel Settings"));
        channels.forEach(channel -> content.add(createChannelCard(channel)));

        content.add(new H3("Per-Service Overrides"));
        content.add(createServiceOverridesGrid());

        add(content);
    }

    private VerticalLayout createChannelCard(NotificationChannel channel) {
        var card = new VerticalLayout();
        card.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderRadius.MEDIUM, LumoUtility.Padding.MEDIUM);
        card.setSpacing(true);

        Optional<NotificationChannelConfig> globalConfigOpt = configService.getGlobalConfig(channel.channelType());
        NotificationChannelConfig globalConfig = globalConfigOpt.orElseGet(() ->
                new NotificationChannelConfig(channel.channelType(), false, "{}"));

        var header = new HorizontalLayout();
        header.setAlignItems(HorizontalLayout.Alignment.CENTER);
        var title = new Span(channel.displayName());
        title.getStyle().set("font-weight", "600").set("font-size", "var(--lumo-font-size-l)");
        header.add(title);

        var enabledCheckbox = new Checkbox("Enabled");
        enabledCheckbox.setValue(globalConfig.isEnabled());

        var helpText = new Span(channel.configDescription());
        helpText.getStyle().set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        var configArea = new TextArea("Configuration JSON");
        configArea.setWidthFull();
        configArea.setValue(globalConfig.getConfigJson() != null ? globalConfig.getConfigJson() : "{}");
        configArea.setMinHeight("100px");

        var saveButton = new Button("Save", VaadinIcon.CHECK.create(), _ -> {
            globalConfig.setEnabled(enabledCheckbox.getValue());
            globalConfig.setConfigJson(configArea.getValue());
            configService.saveGlobalConfig(globalConfig);
            Notification.show("Settings saved for " + channel.displayName(),
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var testButton = new Button("Send Test", VaadinIcon.ENVELOPE.create(), _ -> {
            if (!channel.validate(configArea.getValue())) {
                Notification.show("Invalid configuration",
                        3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            var testService = new MonitoredService("http://test.example.com");
            testService.setName("Test Service");
            var testEvent = new ServiceHealthChangedEvent(
                    testService, true, false, Instant.now());
            channel.send(testEvent, configArea.getValue());
            Notification.show("Test notification sent via " + channel.displayName(),
                    3000, Notification.Position.BOTTOM_START);
        });

        var buttons = new HorizontalLayout(saveButton, testButton);

        card.add(header, enabledCheckbox, helpText, configArea, buttons);
        return card;
    }

    private Grid<MonitoredService> createServiceOverridesGrid() {
        var grid = new Grid<MonitoredService>();
        grid.setSizeFull();

        grid.addColumn(service -> service.getName() != null ? service.getName() : service.getUrl())
                .setHeader("Service")
                .setFlexGrow(2);

        for (NotificationChannel channel : channels) {
            grid.addComponentColumn(service -> {
                String effectiveState = getEffectiveState(service, channel.channelType());
                var badge = new Span(effectiveState);
                badge.getStyle().set("font-size", "var(--lumo-font-size-s)");
                if ("Enabled".equals(effectiveState)) {
                    badge.getStyle().set("color", "var(--lumo-success-color)");
                } else if ("Disabled".equals(effectiveState)) {
                    badge.getStyle().set("color", "var(--lumo-error-color)");
                } else {
                    badge.getStyle().set("color", "var(--lumo-secondary-text-color)");
                }
                return badge;
            }).setHeader(channel.displayName()).setFlexGrow(1);
        }

        grid.addComponentColumn(service -> {
            var editButton = new Button(VaadinIcon.EDIT.create(), _ -> openOverrideDialog(service));
            editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            return editButton;
        }).setHeader("").setFlexGrow(0).setWidth("80px");

        grid.setItems(monitoringService.getServices());
        return grid;
    }

    private String getEffectiveState(MonitoredService service, String channelType) {
        if (service.getId() == null) {
            return "Inherit";
        }
        Optional<NotificationServiceOverride> override =
                configService.getOverride(service.getId(), channelType);
        if (override.isPresent() && override.get().getEnabled() != null) {
            return override.get().getEnabled() ? "Enabled" : "Disabled";
        }
        return "Inherit";
    }

    private void openOverrideDialog(MonitoredService service) {
        var dialog = new Dialog();
        dialog.setHeaderTitle("Override for " + (service.getName() != null ? service.getName() : service.getUrl()));
        dialog.setWidth("500px");

        var content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);

        Map<String, NotificationServiceOverride> existingOverrides =
                configService.getOverridesForService(service.getId()).stream()
                        .collect(Collectors.toMap(NotificationServiceOverride::getChannelType, o -> o));

        for (NotificationChannel channel : channels) {
            var channelSection = new VerticalLayout();
            channelSection.setPadding(false);
            channelSection.setSpacing(false);

            var channelLabel = new Span(channel.displayName());
            channelLabel.getStyle().set("font-weight", "600");

            var stateSelect = new Select<String>();
            stateSelect.setLabel("State");
            stateSelect.setItems("Inherit", "Enabled", "Disabled");

            var overrideConfigArea = new TextArea("Config JSON override (optional)");
            overrideConfigArea.setWidthFull();
            overrideConfigArea.setMinHeight("80px");

            NotificationServiceOverride existing = existingOverrides.get(channel.channelType());
            if (existing != null && existing.getEnabled() != null) {
                stateSelect.setValue(existing.getEnabled() ? "Enabled" : "Disabled");
            } else {
                stateSelect.setValue("Inherit");
            }
            if (existing != null && existing.getConfigJson() != null) {
                overrideConfigArea.setValue(existing.getConfigJson());
            }

            var saveOverrideButton = new Button("Save", _ -> {
                String selected = stateSelect.getValue();
                if ("Inherit".equals(selected)) {
                    if (existing != null) {
                        configService.deleteOverride(existing);
                    }
                } else {
                    NotificationServiceOverride override = existing != null ? existing
                            : new NotificationServiceOverride(service, channel.channelType());
                    override.setEnabled("Enabled".equals(selected));
                    String configJson = overrideConfigArea.getValue();
                    override.setConfigJson(configJson != null && !configJson.isBlank() ? configJson : null);
                    configService.saveOverride(override);
                }
                Notification.show("Override saved for " + channel.displayName(),
                        3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            });
            saveOverrideButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

            channelSection.add(channelLabel, stateSelect, overrideConfigArea, saveOverrideButton);
            content.add(channelSection);
        }

        var closeButton = new Button("Close", _ -> dialog.close());
        dialog.getFooter().add(closeButton);
        dialog.add(content);
        dialog.open();
    }
}
