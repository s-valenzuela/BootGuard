package se.valenzuela.monitoring.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.theme.lumo.Lumo;

@Layout
public final class MainLayout extends AppLayout implements BeforeEnterObserver {

    private boolean darkMode = false;

    private RouterLink servicesLink;
    private RouterLink environmentsLink;
    private RouterLink notificationsLink;
    private RouterLink settingsLink;

    MainLayout() {
        setPrimarySection(Section.NAVBAR);
        addToNavbar(true, createNavbar());
    }

    private Div createNavbar() {
        var navbar = new Div();
        navbar.addClassName("app-navbar");

        // Brand
        var brandIcon = VaadinIcon.CUBES.create();
        brandIcon.addClassName("navbar-brand-icon");
        var brandName = new Span("BootGuard");
        brandName.addClassName("navbar-brand-name");
        var brand = new Div(brandIcon, brandName);
        brand.addClassName("navbar-brand");

        // Nav links
        servicesLink = new RouterLink("Services", MainView.class);
        servicesLink.addClassName("navbar-link");

        environmentsLink = new RouterLink("Environments", EnvironmentsView.class);
        environmentsLink.addClassName("navbar-link");

        notificationsLink = new RouterLink("Notifications", NotificationSettingsView.class);
        notificationsLink.addClassName("navbar-link");

        settingsLink = new RouterLink("Settings", SettingsView.class);
        settingsLink.addClassName("navbar-link");

        var nav = new Div(servicesLink, environmentsLink, notificationsLink, settingsLink);
        nav.addClassName("navbar-nav");

        // Actions
        var darkModeBtn = new Button(VaadinIcon.MOON.create(), _ -> toggleDarkMode());
        darkModeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        darkModeBtn.addClassName("navbar-dark-toggle");

        var actions = new Div(darkModeBtn);
        actions.addClassName("navbar-actions");

        navbar.add(brand, nav, actions);
        return navbar;
    }

    private void toggleDarkMode() {
        darkMode = !darkMode;
        getElement().executeJs(
                "document.documentElement.setAttribute('theme', $0)",
                darkMode ? Lumo.DARK : Lumo.LIGHT
        );
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Class<?> target = event.getNavigationTarget();
        setActiveLink(servicesLink, target == MainView.class);
        setActiveLink(environmentsLink, target == EnvironmentsView.class);
        setActiveLink(notificationsLink, target == NotificationSettingsView.class);
        setActiveLink(settingsLink, target == SettingsView.class);
    }

    private void setActiveLink(RouterLink link, boolean active) {
        if (active) {
            link.addClassName("navbar-link--active");
        } else {
            link.removeClassName("navbar-link--active");
        }
    }
}
