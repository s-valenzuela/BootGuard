package se.valenzuela.monitoring.ui.component;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import se.valenzuela.monitoring.core.model.Environment;

import java.time.Duration;
import java.time.Instant;

public final class ServiceViewUtils {

    private ServiceViewUtils() {}

    public static Icon statusIcon(boolean healthy, boolean warning) {
        var icon = VaadinIcon.CIRCLE.create();
        icon.addClassName("status-dot");
        if (!healthy)     icon.addClassName("status-dot--down");
        else if (warning) icon.addClassName("status-dot--warning");
        else              icon.addClassName("status-dot--healthy");
        return icon;
    }

    public static Span envBadge(Environment env) {
        var badge = new Span(env.getName());
        badge.addClassName("env-badge");
        if (env.getColor() != null && !env.getColor().isBlank()) {
            badge.addClassName("env-badge-colored");
            badge.getStyle().set("background-color", env.getColor());
        } else {
            badge.addClassName("env-badge-default");
        }
        return badge;
    }

    public static String relativeTime(Instant instant) {
        if (instant == null) return "never";
        long seconds = Duration.between(instant, Instant.now()).toSeconds();
        if (seconds < 60) return seconds + "s ago";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + " min ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        return (hours / 24) + "d ago";
    }

    public static Span buildLiveCheckedSpan(Instant lastUpdated) {
        var span = new Span();
        span.addClassName("service-card-time");

        if (lastUpdated == null) {
            span.setText("Never checked");
            return span;
        }

        span.getElement().setAttribute("data-checked-at", String.valueOf(lastUpdated.toEpochMilli()));
        span.setText("Checked " + relativeTime(lastUpdated));

        span.getElement().executeJs("""
            (function(el) {
                if (el._tick) clearInterval(el._tick);
                function fmt(ms) {
                    var s = Math.floor((Date.now() - ms) / 1000);
                    if (s < 60)   return s + 's ago';
                    if (s < 3600) return Math.floor(s / 60) + ' min ago';
                    if (s < 86400) return Math.floor(s / 3600) + 'h ago';
                    return Math.floor(s / 86400) + 'd ago';
                }
                var t = parseInt(el.getAttribute('data-checked-at'));
                el._tick = setInterval(function() {
                    el.textContent = 'Checked ' + fmt(t);
                }, 1000);
            })(this);
            """);

        return span;
    }
}
