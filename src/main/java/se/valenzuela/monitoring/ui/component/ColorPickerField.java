package se.valenzuela.monitoring.ui.component;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.textfield.TextField;

public class ColorPickerField extends Div {

    private final Div colorInput = new Div();
    private final TextField hexField = new TextField();
    private String value = "";

    public ColorPickerField(String label) {
        var labelComponent = new NativeLabel(label);
        labelComponent.getStyle().set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "500")
                .set("color", "var(--lumo-secondary-text-color)");

        colorInput.getElement().setProperty("innerHTML",
                "<input type='color' style='width:40px;height:40px;border:none;padding:0;cursor:pointer;background:none;'>");
        colorInput.getStyle().set("display", "inline-block");

        hexField.setPlaceholder("#3B82F6");
        hexField.setMaxLength(7);
        hexField.setWidth("120px");
        hexField.setClearButtonVisible(true);

        colorInput.getElement().addEventListener("input", e -> {
            String color = e.getEventData().path("event.target.value").asText();
            if (color != null && !color.isBlank()) {
                value = color;
                hexField.setValue(color);
            }
        }).addEventData("event.target.value");

        hexField.addValueChangeListener(e -> {
            String hex = e.getValue().trim();
            if (hex.matches("#[0-9a-fA-F]{6}")) {
                value = hex;
                colorInput.getElement().executeJs(
                        "this.querySelector('input').value = $0", hex);
            } else if (hex.isEmpty()) {
                value = "";
            }
        });

        var row = new Div(colorInput, hexField);
        row.getStyle().set("display", "flex")
                .set("align-items", "center")
                .set("gap", "var(--lumo-space-s)");

        add(labelComponent, row);
        getStyle().set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "var(--lumo-space-xs)");
    }

    public String getValue() {
        return value;
    }

    public void setValue(String color) {
        this.value = color != null ? color : "";
        if (color != null && !color.isBlank()) {
            hexField.setValue(color);
            colorInput.getElement().executeJs(
                    "this.querySelector('input').value = $0", color);
        } else {
            hexField.clear();
        }
    }
}
