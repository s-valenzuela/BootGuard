package se.valenzuela.monitoring.ui.component;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.textfield.TextField;
import lombok.Getter;

public class ColorPickerField extends Div {

    private final Div colorInput = new Div();
    private final TextField hexField = new TextField();
    @Getter
    private String value = "";

    public ColorPickerField(String label) {
        var labelComponent = new NativeLabel(label);
        labelComponent.addClassName("form-label");

        colorInput.getElement().setProperty("innerHTML", "<input type='color'>");
        colorInput.addClassName("color-picker-input");

        hexField.setPlaceholder("#3B82F6");
        hexField.setMaxLength(7);
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
        row.addClassName("color-picker-row");

        add(labelComponent, row);
        addClassName("color-picker");
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
