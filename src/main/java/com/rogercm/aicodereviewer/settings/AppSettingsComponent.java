package com.rogercm.aicodereviewer.settings;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;

import javax.swing.*;

public class AppSettingsComponent {

    private final JPanel mainPanel;
    private final JBPasswordField apiKeyField = new JBPasswordField();
    private final JBTextField modelField = new JBTextField();

    public AppSettingsComponent() {
        apiKeyField.setColumns(40);
        modelField.setColumns(40);
        modelField.setToolTipText("e.g. llama-3.3-70b-versatile");

        mainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Groq API Key:"), apiKeyField, 1, false)
                .addLabeledComponent(new JBLabel("Model:"), modelField, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public JComponent getPreferredFocusedComponent() {
        return apiKeyField;
    }

    public String getApiKey() {
        return new String(apiKeyField.getPassword());
    }

    public void setApiKey(String apiKey) {
        apiKeyField.setText(apiKey != null ? apiKey : "");
    }

    public String getModel() {
        return modelField.getText();
    }

    public void setModel(String model) {
        modelField.setText(model != null ? model : "");
    }
}
