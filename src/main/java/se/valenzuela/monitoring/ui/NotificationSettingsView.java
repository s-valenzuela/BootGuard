package se.valenzuela.monitoring.ui;

import com.vaadin.flow.component.Component;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import se.valenzuela.monitoring.core.model.MonitoredService;
import se.valenzuela.monitoring.notification.channel.NotificationChannel;
import se.valenzuela.monitoring.notification.channel.NotificationChannel.ConfigField;
import se.valenzuela.monitoring.notification.channel.NotificationChannel.FieldType;
import se.valenzuela.monitoring.notification.model.NotificationChannelConfig;
import se.valenzuela.monitoring.notification.model.NotificationServiceOverride;
import se.valenzuela.monitoring.notification.event.ServiceHealthChangedEvent;
import se.valenzuela.monitoring.notification.service.NotificationConfigService;
import se.valenzuela.monitoring.core.service.MonitoringService;
import se.valenzuela.monitoring.ui.component.EmailListField;
import se.valenzuela.monitoring.ui.component.ViewToolbar;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Route("notifications")
@Menu(order = 10, icon = "vaadin:bell", title = "Notifications")
public class NotificationSettingsView extends Main {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    private final List<NotificationChannel> channels;
    private final NotificationConfigService configService;
    private final MonitoringService monitoringService;

    public NotificationSettingsView(List<NotificationChannel> channels,
                                    NotificationConfigService configService,
                                    MonitoringService monitoringService) {
        this.channels = channels;
        this.configService = configService;
        this.monitoringService = monitoringService;

        addClassNames("view-content", LumoUtility.BoxSizing.BORDER, "scrollable-page");
        setWidthFull();

        add(new ViewToolbar("Notification Settings"));

        var content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.setWidthFull();

        content.add(new H3("Channel Settings"));
        channels.forEach(channel -> content.add(createChannelCard(channel)));

        content.add(new H3("Per-Service Overrides"));
        var grid = createServiceOverridesGrid();
        grid.setMinHeight("300px");
        grid.setHeight(null);
        grid.setAllRowsVisible(true);
        content.add(grid);

        add(content);
    }

    private VerticalLayout createChannelCard(NotificationChannel channel) {
        var card = new VerticalLayout();
        card.addClassNames("channel-card");
        card.setSpacing(false);

        Optional<NotificationChannelConfig> globalConfigOpt = configService.getGlobalConfig(channel.channelType());
        NotificationChannelConfig globalConfig = globalConfigOpt.orElseGet(() ->
                new NotificationChannelConfig(channel.channelType(), false, "{}"));

        var header = new HorizontalLayout();
        header.setAlignItems(HorizontalLayout.Alignment.CENTER);
        var title = new Span(channel.displayName());
        title.addClassName("channel-card-title");
        header.add(title);

        var enabledCheckbox = new Checkbox("Enabled");
        enabledCheckbox.setValue(globalConfig.isEnabled());

        var formResult = buildConfigForm(channel.configFields(), globalConfig.getConfigJson());
        var form = new VerticalLayout();
        form.setPadding(false);
        form.setSpacing(false);
        form.addClassName("channel-card-form");
        formResult.components().values().forEach(form::add);

        var saveButton = new Button("Save", VaadinIcon.CHECK.create(), _ -> {
            String json = fieldsToJson(formResult.valueGetters());
            globalConfig.setEnabled(enabledCheckbox.getValue());
            globalConfig.setConfigJson(json);
            configService.saveGlobalConfig(globalConfig);
            Notification.show("Settings saved for " + channel.displayName(),
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var testButton = new Button("Send Test", VaadinIcon.ENVELOPE.create(), _ -> {
            String json = fieldsToJson(formResult.valueGetters());
            if (!channel.validate(json)) {
                Notification.show("Invalid configuration",
                        3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            var testService = new MonitoredService("http://test.example.com");
            testService.setName("Test Service");
            var testEvent = new ServiceHealthChangedEvent(
                    testService, true, false, Instant.now());
            channel.send(testEvent, json);
            Notification.show("Test notification sent via " + channel.displayName(),
                    3000, Notification.Position.BOTTOM_START);
        });

        var buttons = new HorizontalLayout(saveButton, testButton);

        form.setVisible(enabledCheckbox.getValue());
        buttons.setVisible(enabledCheckbox.getValue());
        enabledCheckbox.addValueChangeListener(e -> {
            boolean enabled = e.getValue();
            form.setVisible(enabled);
            buttons.setVisible(enabled);
            if (!enabled) {
                globalConfig.setEnabled(false);
                configService.saveGlobalConfig(globalConfig);
                Notification.show(channel.displayName() + " disabled",
                        3000, Notification.Position.BOTTOM_START);
            }
        });

        card.add(header, enabledCheckbox, form, buttons);
        return card;
    }

    private Grid<MonitoredService> createServiceOverridesGrid() {
        var grid = new Grid<MonitoredService>();
        grid.setWidthFull();

        grid.addColumn(service -> service.getName() != null ? service.getName() : service.getUrl())
                .setHeader("Service")
                .setFlexGrow(2);

        for (NotificationChannel channel : channels) {
            grid.addComponentColumn(service -> {
                String effectiveState = getEffectiveState(service, channel.channelType());
                var badge = new Span(effectiveState);
                badge.addClassName(switch (effectiveState) {
                    case "Enabled" -> "override-state--enabled";
                    case "Disabled" -> "override-state--disabled";
                    default -> "override-state--inherit";
                });
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

    private record ConfigFormResult(Map<String, Component> components, Map<String, Supplier<String>> valueGetters) {
    }

    private ConfigFormResult buildConfigForm(List<ConfigField> fields, String json) {
        JsonNode node = null;
        if (json != null && !json.isBlank()) {
            try {
                node = JSON_MAPPER.readTree(json);
            } catch (Exception ignored) {
            }
        }
        Map<String, Component> components = new LinkedHashMap<>();
        Map<String, Supplier<String>> valueGetters = new LinkedHashMap<>();
        for (ConfigField field : fields) {
            String value = "";
            if (node != null && node.has(field.key())) {
                value = node.path(field.key()).asString("");
            }

            if (field.type() == FieldType.EMAIL_LIST) {
                var emailList = new EmailListField(field.label());
                emailList.setValue(value);
                components.put(field.key(), emailList);
                valueGetters.put(field.key(), emailList::getValue);
            } else if (field.type() == FieldType.SECRET) {
                var tf = new TextField(field.label());
                tf.setRequiredIndicatorVisible(field.required());
                if (field.description() != null) {
                    tf.setHelperText(field.description());
                }
                String fullValue = value;
                if (!value.isEmpty()) {
                    tf.setValue(maskSecret(value));
                }
                tf.setWidthFull();
                components.put(field.key(), tf);
                valueGetters.put(field.key(), () -> {
                    String current = tf.getValue().trim();
                    if (current.endsWith("**********") && !fullValue.isEmpty()) {
                        return fullValue;
                    }
                    return current;
                });
            } else {
                var tf = new TextField(field.label());
                tf.setRequiredIndicatorVisible(field.required());
                if (field.description() != null) {
                    tf.setHelperText(field.description());
                }
                if (value.isEmpty() && field.defaultValue() != null) {
                    tf.setPlaceholder(field.defaultValue());
                }
                tf.setValue(value);
                tf.setWidthFull();
                components.put(field.key(), tf);
                valueGetters.put(field.key(), () -> tf.getValue().trim());
            }
        }
        return new ConfigFormResult(components, valueGetters);
    }

    private String fieldsToJson(Map<String, Supplier<String>> valueGetters) {
        ObjectNode node = JSON_MAPPER.createObjectNode();
        valueGetters.forEach((key, getter) -> {
            String val = getter.get();
            if (!val.isEmpty()) {
                node.put(key, val);
            }
        });
        return node.toString();
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
            channelLabel.addClassName("channel-card-title");

            var stateSelect = new Select<String>();
            stateSelect.setLabel("State");
            stateSelect.setItems("Inherit", "Enabled", "Disabled");

            NotificationServiceOverride existing = existingOverrides.get(channel.channelType());
            if (existing != null && existing.getEnabled() != null) {
                stateSelect.setValue(existing.getEnabled() ? "Enabled" : "Disabled");
            } else {
                stateSelect.setValue("Inherit");
            }

            String existingJson = existing != null && existing.getConfigJson() != null
                    ? existing.getConfigJson() : null;
            var overrideResult = buildConfigForm(channel.configFields(), existingJson);
            var overrideForm = new VerticalLayout();
            overrideForm.setPadding(false);
            overrideForm.setSpacing(true);
            overrideResult.components().forEach((_, comp) -> {
                if (comp instanceof TextField tf) {
                    tf.setRequiredIndicatorVisible(false);
                    tf.setPlaceholder("Leave empty to inherit");
                }
                overrideForm.add(comp);
            });

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
                    String json = fieldsToJson(overrideResult.valueGetters());
                    override.setConfigJson("{}".equals(json) ? null : json);
                    configService.saveOverride(override);
                }
                Notification.show("Override saved for " + channel.displayName(),
                        3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            });
            saveOverrideButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

            channelSection.add(channelLabel, stateSelect, overrideForm, saveOverrideButton);
            content.add(channelSection);
        }

        var scrollable = new VerticalLayout(content);
        scrollable.setPadding(false);
        scrollable.addClassName("dialog-scroll-content");

        var closeButton = new Button("Close", _ -> dialog.close());
        dialog.getFooter().add(closeButton);
        dialog.add(scrollable);
        dialog.open();
    }

    private static String maskSecret(String value) {
        if (value.length() <= 10) {
            return "*".repeat(value.length());
        }
        return value.substring(0, value.length() - 10) + "**********";
    }
}
