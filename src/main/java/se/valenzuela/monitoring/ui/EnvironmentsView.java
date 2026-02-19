package se.valenzuela.monitoring.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import se.valenzuela.monitoring.ui.component.ColorPickerField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import se.valenzuela.monitoring.core.model.Environment;
import se.valenzuela.monitoring.core.service.EnvironmentService;
import se.valenzuela.monitoring.ui.component.ViewToolbar;

import java.util.ArrayList;

@Route("environments")
@Menu(order = 5, icon = "vaadin:tags", title = "Environments")
public class EnvironmentsView extends Main {

    private final EnvironmentService environmentService;
    private final Grid<Environment> grid;

    public EnvironmentsView(EnvironmentService environmentService) {
        this.environmentService = environmentService;

        addClassNames("view-content", LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.BoxSizing.BORDER);
        setSizeFull();

        var addButton = new Button("Add Environment", VaadinIcon.PLUS.create(), _ -> openDialog(null));
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(new ViewToolbar("Environments", addButton));

        grid = createGrid();
        add(grid);
        refreshGrid();
    }

    private Grid<Environment> createGrid() {
        var g = new Grid<Environment>();
        g.setSizeFull();

        g.addColumn(Environment::getName).setHeader("Name").setSortable(true).setFlexGrow(2);

        g.addComponentColumn(env -> {
            if (env.getColor() == null || env.getColor().isBlank()) {
                return new Span("-");
            }
            var swatch = new Span();
            swatch.addClassName("color-swatch");
            swatch.getStyle().set("background-color", env.getColor());
            var label = new Span(env.getColor());
            label.addClassNames(LumoUtility.Margin.Left.XSMALL);
            var layout = new HorizontalLayout(swatch, label);
            layout.setAlignItems(HorizontalLayout.Alignment.CENTER);
            layout.setPadding(false);
            layout.setSpacing(false);
            return layout;
        }).setHeader("Color").setFlexGrow(1);

        g.addColumn(Environment::getDisplayOrder).setHeader("Display Order").setSortable(true).setFlexGrow(0).setWidth("140px").setKey("displayOrder");
        g.sort(GridSortOrder.asc(g.getColumnByKey("displayOrder")).build());

        g.addColumn(new ComponentRenderer<>(env -> {
            var editButton = new Button(VaadinIcon.EDIT.create(), _ -> openDialog(env));
            editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            var deleteButton = new Button(VaadinIcon.TRASH.create(), _ -> openDeleteDialog(env));
            deleteButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            var actions = new HorizontalLayout(editButton, deleteButton);
            actions.setSpacing(true);
            actions.setPadding(false);
            return actions;
        })).setHeader("Actions").setFlexGrow(0).setWidth("120px");

        return g;
    }

    private void openDialog(Environment existing) {
        var dialog = new Dialog();
        dialog.setHeaderTitle(existing == null ? "Add Environment" : "Edit Environment");
        dialog.setWidth("400px");

        var nameField = new TextField("Name");
        nameField.setRequired(true);
        nameField.setWidthFull();

        var colorField = new ColorPickerField("Color");
        colorField.setWidthFull();

        var orderField = new IntegerField("Display Order");
        orderField.setValue(0);
        orderField.setStepButtonsVisible(true);
        orderField.setWidthFull();

        if (existing != null) {
            nameField.setValue(existing.getName());
            colorField.setValue(existing.getColor());
            orderField.setValue(existing.getDisplayOrder());
        }

        var content = new VerticalLayout(nameField, colorField, orderField);
        content.setPadding(false);
        content.setSpacing(true);
        dialog.add(content);

        var saveButton = new Button("Save", _ -> {
            String name = nameField.getValue().trim();
            if (name.isEmpty()) {
                nameField.setInvalid(true);
                nameField.setErrorMessage("Name is required");
                return;
            }

            if (existing == null && environmentService.existsByName(name)) {
                nameField.setInvalid(true);
                nameField.setErrorMessage("Environment already exists");
                return;
            }

            if (existing != null && !existing.getName().equals(name) && environmentService.existsByName(name)) {
                nameField.setInvalid(true);
                nameField.setErrorMessage("Environment already exists");
                return;
            }

            String color = colorField.getValue() != null ? colorField.getValue().trim() : "";
            int order = orderField.getValue() != null ? orderField.getValue() : 0;

            if (existing == null) {
                environmentService.createEnvironment(name, color.isEmpty() ? null : color, order);
            } else {
                existing.setName(name);
                existing.setColor(color.isEmpty() ? null : color);
                existing.setDisplayOrder(order);
                environmentService.updateEnvironment(existing);
            }

            dialog.close();
            refreshGrid();
            Notification.show("Environment saved", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var cancelButton = new Button("Cancel", _ -> dialog.close());

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }

    private void openDeleteDialog(Environment environment) {
        var dialog = new Dialog();
        dialog.setHeaderTitle("Delete Environment");

        var content = new VerticalLayout();
        content.add("Are you sure you want to delete:");
        content.add(new Span(environment.getName()));
        dialog.add(content);

        var confirmButton = new Button("Delete", _ -> {
            environmentService.deleteEnvironment(environment);
            dialog.close();
            refreshGrid();
            Notification.show("Environment deleted", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        var cancelButton = new Button("Cancel", _ -> dialog.close());

        dialog.getFooter().add(cancelButton, confirmButton);
        dialog.open();
    }

    private void refreshGrid() {
        grid.setItems(new ArrayList<>(environmentService.getAllEnvironments()));
    }
}
