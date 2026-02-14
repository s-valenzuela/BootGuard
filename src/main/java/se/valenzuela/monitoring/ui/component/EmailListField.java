package se.valenzuela.monitoring.ui.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class EmailListField extends VerticalLayout {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    private final List<String> emails = new ArrayList<>();
    private final FlexLayout chipContainer = new FlexLayout();
    private final TextField inputField = new TextField();

    public EmailListField(String label) {
        setPadding(false);
        setSpacing(false);
        setWidthFull();

        var labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "500")
                .set("color", "var(--lumo-secondary-text-color)");

        chipContainer.getStyle()
                .set("flex-wrap", "wrap")
                .set("gap", "var(--lumo-space-xs)");

        inputField.setPlaceholder("Enter email address");
        inputField.setClearButtonVisible(true);

        var addButton = new Button(VaadinIcon.PLUS.create(), _ -> addFromInput());
        addButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        addButton.getElement().setAttribute("aria-label", "Add email");

        inputField.addKeyPressListener(com.vaadin.flow.component.Key.ENTER, _ -> addFromInput());

        var inputRow = new HorizontalLayout(inputField, addButton);
        inputRow.setAlignItems(Alignment.BASELINE);
        inputRow.setPadding(false);
        inputRow.setWidthFull();
        inputRow.setFlexGrow(1, inputField);

        add(labelSpan, chipContainer, inputRow);
    }

    public String getValue() {
        return String.join(",", emails);
    }

    public void setValue(String commaSeparated) {
        emails.clear();
        if (commaSeparated != null && !commaSeparated.isBlank()) {
            Arrays.stream(commaSeparated.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(emails::add);
        }
        refreshChips();
    }

    private void addFromInput() {
        String email = inputField.getValue().trim();
        if (email.isEmpty()) {
            return;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            inputField.setInvalid(true);
            inputField.setErrorMessage("Invalid email address");
            return;
        }
        if (emails.contains(email)) {
            Notification.show("Email already added", 2000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
            return;
        }
        inputField.setInvalid(false);
        inputField.clear();
        emails.add(email);
        refreshChips();
    }

    private void refreshChips() {
        chipContainer.removeAll();
        for (String email : emails) {
            chipContainer.add(createChip(email));
        }
    }

    private HorizontalLayout createChip(String email) {
        var label = new Span(email);
        label.getStyle().set("font-size", "var(--lumo-font-size-s)");

        var removeBtn = new Button(VaadinIcon.CLOSE_SMALL.create(), _ -> {
            emails.remove(email);
            refreshChips();
        });
        removeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        removeBtn.getStyle().set("min-width", "0");
        removeBtn.getElement().setAttribute("aria-label", "Remove " + email);

        var chip = new HorizontalLayout(label, removeBtn);
        chip.setAlignItems(Alignment.CENTER);
        chip.setSpacing(false);
        chip.setPadding(false);
        chip.getStyle()
                .set("background-color", "var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "2px 4px 2px 8px")
                .set("gap", "2px");
        return chip;
    }
}
