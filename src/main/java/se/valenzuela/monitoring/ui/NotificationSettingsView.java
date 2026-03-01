package se.valenzuela.monitoring.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
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
import se.valenzuela.monitoring.ui.component.BaseDialog;
import se.valenzuela.monitoring.ui.component.EmailListField;
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

        var page = new VerticalLayout();
        page.setPadding(true);
        page.setSpacing(false);
        page.setWidthFull();

        // ── Channels ───────────────────────────────────────────────────────
        page.add(sectionHeader("Channels",
                "Configure where notifications are sent when a service changes health state."));

        var channelGrid = new Div();
        channelGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(380px, 1fr))")
                .set("gap", "var(--lumo-space-m)")
                .set("width", "100%")
                .set("margin-bottom", "var(--lumo-space-l)");
        channels.forEach(channel -> channelGrid.add(createChannelCard(channel)));
        page.add(channelGrid);

        // ── Service overrides ──────────────────────────────────────────────
        page.add(sectionHeader("Service overrides",
                "Fine-tune which channels alert for each service. \u201cInherit\u201d follows the channel\u2019s global setting."));
        page.add(createServiceOverridesGrid());

        add(page);
    }

    // ── section header ────────────────────────────────────────────────────────

    private Div sectionHeader(String title, String description) {
        var titleSpan = new Span(title);
        titleSpan.addClassName("notif-section-title");
        var descSpan = new Span(description);
        descSpan.addClassName("notif-section-desc");
        var header = new Div(titleSpan, descSpan);
        header.addClassName("notif-section-header");
        return header;
    }

    // ── channel card ──────────────────────────────────────────────────────────

    private VerticalLayout createChannelCard(NotificationChannel channel) {
        Optional<NotificationChannelConfig> globalConfigOpt = configService.getGlobalConfig(channel.channelType());
        NotificationChannelConfig globalConfig = globalConfigOpt.orElseGet(() ->
                new NotificationChannelConfig(channel.channelType(), false, "{}"));
        boolean isEnabled = globalConfig.isEnabled();

        var card = new VerticalLayout();
        card.addClassName("channel-card");
        card.setSpacing(false);
        card.setPadding(false);
        card.getStyle().set("padding", "0").set("gap", "0").set("overflow", "hidden");

        // header: channel name + status badge
        var statusBadge = buildStatusBadge(isEnabled);
        var title = new Span(channel.displayName());
        title.addClassName("channel-card-title");
        var header = new HorizontalLayout(title, statusBadge);
        header.setAlignItems(HorizontalLayout.Alignment.CENTER);
        header.setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);
        header.setWidthFull();
        header.setPadding(false);
        header.getStyle().set("padding", "var(--lumo-space-m)");

        // enable toggle
        var enabledCheckbox = new Checkbox("Send notifications via " + channel.displayName());
        enabledCheckbox.setValue(isEnabled);
        enabledCheckbox.getStyle().set("padding", "var(--lumo-space-xs) var(--lumo-space-m)");

        // config form
        var formResult = buildConfigForm(channel.configFields(), globalConfig.getConfigJson());
        var form = new VerticalLayout();
        form.addClassName("channel-card-form");
        form.setSpacing(true);
        form.setPadding(false);
        form.getStyle().set("padding", "0 var(--lumo-space-m) var(--lumo-space-xs)");
        formResult.components().values().forEach(form::add);

        // footer: test + save
        var saveButton = new Button("Save", VaadinIcon.CHECK.create(), _ -> {
            String json = fieldsToJson(formResult.valueGetters());
            boolean enabled = enabledCheckbox.getValue();
            globalConfig.setEnabled(enabled);
            globalConfig.setConfigJson(json);
            configService.saveGlobalConfig(globalConfig);
            updateStatusBadge(statusBadge, enabled);
            Notification.show(channel.displayName() + " settings saved",
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var testButton = new Button("Send test", VaadinIcon.ENVELOPE.create(), _ -> {
            String json = fieldsToJson(formResult.valueGetters());
            if (!channel.validate(json)) {
                Notification.show("Save valid settings before testing",
                        3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            var testService = new MonitoredService("http://test.example.com");
            testService.setName("Test Service");
            channel.send(new ServiceHealthChangedEvent(testService, true, false, Instant.now()), json);
            Notification.show("Test sent via " + channel.displayName(), 3000, Notification.Position.BOTTOM_START);
        });

        var footer = new HorizontalLayout(testButton, saveButton);
        footer.addClassName("channel-card-footer");
        footer.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);
        footer.setSpacing(true);
        footer.setPadding(false);
        footer.getStyle().set("padding", "var(--lumo-space-xs) var(--lumo-space-m)");

        form.setVisible(isEnabled);
        footer.setVisible(isEnabled);
        enabledCheckbox.addValueChangeListener(e -> {
            boolean enabled = e.getValue();
            form.setVisible(enabled);
            footer.setVisible(enabled);
            if (!enabled) {
                globalConfig.setEnabled(false);
                configService.saveGlobalConfig(globalConfig);
                updateStatusBadge(statusBadge, false);
                Notification.show(channel.displayName() + " disabled", 2000, Notification.Position.BOTTOM_START);
            }
        });

        card.add(header, new Hr(), enabledCheckbox, form, footer);
        return card;
    }

    private Span buildStatusBadge(boolean enabled) {
        var badge = new Span(enabled ? "Active" : "Inactive");
        badge.addClassName("channel-status-badge");
        badge.addClassName(enabled ? "channel-status-badge--active" : "channel-status-badge--inactive");
        return badge;
    }

    private void updateStatusBadge(Span badge, boolean enabled) {
        badge.setText(enabled ? "Active" : "Inactive");
        badge.removeClassNames("channel-status-badge--active", "channel-status-badge--inactive");
        badge.addClassName(enabled ? "channel-status-badge--active" : "channel-status-badge--inactive");
    }

    // ── service overrides grid ────────────────────────────────────────────────

    private Grid<MonitoredService> createServiceOverridesGrid() {
        var grid = new Grid<MonitoredService>();
        grid.setWidthFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER);

        grid.addColumn(service -> service.getName() != null ? service.getName() : service.getUrl())
                .setHeader("Service")
                .setFlexGrow(2);

        for (NotificationChannel channel : channels) {
            grid.addComponentColumn(service ->
                    overrideBadge(getEffectiveState(service, channel.channelType()))
            ).setHeader(channel.displayName()).setFlexGrow(1).setWidth("130px");
        }

        grid.addComponentColumn(service -> {
            var btn = new Button("Configure", _ -> openOverrideDialog(service, grid));
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            return btn;
        }).setFlexGrow(0).setWidth("120px");

        grid.setItems(monitoringService.getServices());
        grid.setAllRowsVisible(true);
        grid.setHeight(null);
        return grid;
    }

    private Span overrideBadge(String state) {
        var badge = new Span(state);
        badge.addClassName("override-badge");
        badge.addClassName(switch (state) {
            case "Enabled"  -> "override-badge--enabled";
            case "Disabled" -> "override-badge--disabled";
            default         -> "override-badge--inherit";
        });
        return badge;
    }

    private String getEffectiveState(MonitoredService service, String channelType) {
        if (service.getId() == null) return "Inherit";
        Optional<NotificationServiceOverride> override =
                configService.getOverride(service.getId(), channelType);
        if (override.isPresent() && override.get().getEnabled() != null) {
            return override.get().getEnabled() ? "Enabled" : "Disabled";
        }
        return "Inherit";
    }

    // ── override dialog ───────────────────────────────────────────────────────

    private void openOverrideDialog(MonitoredService service, Grid<MonitoredService> grid) {
        new ServiceOverrideDialog(service, channels, configService, grid).open();
    }

    private static class ServiceOverrideDialog extends BaseDialog {

        ServiceOverrideDialog(MonitoredService service, List<NotificationChannel> channels,
                              NotificationConfigService configService, Grid<MonitoredService> grid) {
            super("Overrides \u2014 " + (service.getName() != null ? service.getName() : service.getUrl()));

            var content = new VerticalLayout();
            content.setPadding(false);
            content.setSpacing(true);

            Map<String, NotificationServiceOverride> existingOverrides =
                    configService.getOverridesForService(service.getId()).stream()
                            .collect(Collectors.toMap(NotificationServiceOverride::getChannelType, o -> o));

            boolean first = true;
            for (NotificationChannel channel : channels) {
                if (!first) content.add(new Hr());
                first = false;

                var channelTitle = new Span(channel.displayName());
                channelTitle.addClassName("channel-card-title");

                var stateSelect = new Select<String>();
                stateSelect.setLabel("Notification state");
                stateSelect.setItems("Inherit", "Enabled", "Disabled");
                stateSelect.setHelperText("Inherit uses the channel\u2019s global setting");

                NotificationServiceOverride existing = existingOverrides.get(channel.channelType());
                stateSelect.setValue(existing != null && existing.getEnabled() != null
                        ? (existing.getEnabled() ? "Enabled" : "Disabled")
                        : "Inherit");

                String existingJson = existing != null ? existing.getConfigJson() : null;
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

                final NotificationServiceOverride captured = existing;
                var saveBtn = new Button("Apply", VaadinIcon.CHECK.create(), _ -> {
                    String selected = stateSelect.getValue();
                    if ("Inherit".equals(selected)) {
                        if (captured != null) configService.deleteOverride(captured);
                    } else {
                        NotificationServiceOverride override = captured != null ? captured
                                : new NotificationServiceOverride(service, channel.channelType());
                        override.setEnabled("Enabled".equals(selected));
                        String json = fieldsToJson(overrideResult.valueGetters());
                        override.setConfigJson("{}".equals(json) ? null : json);
                        configService.saveOverride(override);
                    }
                    grid.getDataProvider().refreshAll();
                    Notification.show("Override saved for " + channel.displayName(),
                            3000, Notification.Position.BOTTOM_START)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                });
                saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

                content.add(channelTitle, stateSelect, overrideForm, saveBtn);
            }

            var scrollable = new VerticalLayout(content);
            scrollable.setPadding(false);
            scrollable.addClassName("dialog-scroll-content");
            getFooter().add(closeButton());
            add(scrollable);
        }
    }

    // ── form helpers ──────────────────────────────────────────────────────────

    private record ConfigFormResult(Map<String, Component> components, Map<String, Supplier<String>> valueGetters) {}

    private static ConfigFormResult buildConfigForm(List<ConfigField> fields, String json) {
        JsonNode node = null;
        if (json != null && !json.isBlank()) {
            try { node = JSON_MAPPER.readTree(json); } catch (Exception ignored) {}
        }
        Map<String, Component> components = new LinkedHashMap<>();
        Map<String, Supplier<String>> valueGetters = new LinkedHashMap<>();
        for (ConfigField field : fields) {
            String value = node != null && node.has(field.key()) ? node.path(field.key()).asString("") : "";
            if (field.type() == FieldType.EMAIL_LIST) {
                var emailList = new EmailListField(field.label());
                emailList.setValue(value);
                components.put(field.key(), emailList);
                valueGetters.put(field.key(), emailList::getValue);
            } else if (field.type() == FieldType.SECRET) {
                var tf = new TextField(field.label());
                tf.setRequiredIndicatorVisible(field.required());
                if (field.description() != null) tf.setHelperText(field.description());
                String fullValue = value;
                if (!value.isEmpty()) tf.setValue(maskSecret(value));
                tf.setWidthFull();
                components.put(field.key(), tf);
                valueGetters.put(field.key(), () -> {
                    String current = tf.getValue().trim();
                    return current.endsWith("**********") && !fullValue.isEmpty() ? fullValue : current;
                });
            } else {
                var tf = new TextField(field.label());
                tf.setRequiredIndicatorVisible(field.required());
                if (field.description() != null) tf.setHelperText(field.description());
                if (value.isEmpty() && field.defaultValue() != null) tf.setPlaceholder(field.defaultValue());
                tf.setValue(value);
                tf.setWidthFull();
                components.put(field.key(), tf);
                valueGetters.put(field.key(), () -> tf.getValue().trim());
            }
        }
        return new ConfigFormResult(components, valueGetters);
    }

    private static String fieldsToJson(Map<String, Supplier<String>> valueGetters) {
        ObjectNode node = JSON_MAPPER.createObjectNode();
        valueGetters.forEach((key, getter) -> {
            String val = getter.get();
            if (!val.isEmpty()) node.put(key, val);
        });
        return node.toString();
    }

    private static String maskSecret(String value) {
        if (value.length() <= 10) return "*".repeat(value.length());
        return value.substring(0, value.length() - 10) + "**********";
    }
}
