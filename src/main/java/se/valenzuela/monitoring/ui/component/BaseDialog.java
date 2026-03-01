package se.valenzuela.monitoring.ui.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;

public abstract class BaseDialog extends Dialog {

    protected BaseDialog(String title) {
        this(title, "480px");
    }

    protected BaseDialog(String title, String width) {
        setHeaderTitle(title);
        setWidth(width);
    }

    protected Button closeButton() {
        return new Button("Close", _ -> close());
    }

    protected Button cancelButton() {
        return new Button("Cancel", _ -> close());
    }
}
