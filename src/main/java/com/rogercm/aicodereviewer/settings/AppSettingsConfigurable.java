package com.rogercm.aicodereviewer.settings;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

public class AppSettingsConfigurable implements Configurable {

    private AppSettingsComponent component;

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "AI Code Reviewer";
    }

    @Override
    public @Nullable JComponent createComponent() {
        component = new AppSettingsComponent();
        return component.getPanel();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return component.getPreferredFocusedComponent();
    }

    @Override
    public boolean isModified() {
        AppSettingsState settings = AppSettingsState.getInstance();
        return !Objects.equals(component.getApiKey(), settings.getApiKey())
                || !Objects.equals(component.getModel(), settings.getModel());
    }

    @Override
    public void apply() {
        AppSettingsState settings = AppSettingsState.getInstance();
        settings.setApiKey(component.getApiKey());
        settings.setModel(component.getModel());
    }

    @Override
    public void reset() {
        AppSettingsState settings = AppSettingsState.getInstance();
        component.setApiKey(settings.getApiKey());
        component.setModel(settings.getModel());
    }

    @Override
    public void disposeUIResources() {
        component = null;
    }
}
