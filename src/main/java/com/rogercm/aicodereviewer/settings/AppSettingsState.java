package com.rogercm.aicodereviewer.settings;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
        name = "com.rogercm.aicodereviewer.AppSettingsState",
        storages = @Storage("AiCodeReviewerSettings.xml")
)
public class AppSettingsState implements PersistentStateComponent<AppSettingsState.State> {

    private static final String SERVICE_NAME = "AiCodeReviewer";
    private static final String API_KEY_ACCOUNT = "groqApiKey";

    private State myState = new State();

    public static class State {
        public String model = "llama-3.3-70b-versatile";
    }

    public static AppSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(AppSettingsState.class);
    }

    @Override
    public @Nullable State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        myState = state;
    }

    public String getApiKey() {
        CredentialAttributes attributes = createCredentialAttributes();
        Credentials credentials = PasswordSafe.getInstance().get(attributes);
        return credentials != null ? credentials.getPasswordAsString() : "";
    }

    public void setApiKey(String apiKey) {
        CredentialAttributes attributes = createCredentialAttributes();
        if (apiKey == null || apiKey.isBlank()) {
            PasswordSafe.getInstance().set(attributes, null);
        } else {
            PasswordSafe.getInstance().set(attributes, new Credentials(API_KEY_ACCOUNT, apiKey));
        }
    }

    public String getModel() {
        return myState.model != null ? myState.model : "llama-3.3-70b-versatile";
    }

    public void setModel(String model) {
        myState.model = model;
    }

    private CredentialAttributes createCredentialAttributes() {
        return new CredentialAttributes(
                CredentialAttributesKt.generateServiceName(SERVICE_NAME, API_KEY_ACCOUNT)
        );
    }
}
