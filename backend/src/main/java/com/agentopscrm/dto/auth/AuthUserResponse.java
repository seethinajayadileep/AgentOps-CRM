package com.agentopscrm.dto.auth;

import java.util.UUID;

public class AuthUserResponse {

    private UUID id;
    private String fullName;
    private String email;
    private boolean externalActionsDisabled;
    private boolean sharedWorkspace;

    public AuthUserResponse() {
    }

    public AuthUserResponse(
            UUID id,
            String fullName,
            String email,
            boolean externalActionsDisabled,
            boolean sharedWorkspace) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.externalActionsDisabled = externalActionsDisabled;
        this.sharedWorkspace = sharedWorkspace;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isExternalActionsDisabled() {
        return externalActionsDisabled;
    }

    public void setExternalActionsDisabled(boolean externalActionsDisabled) {
        this.externalActionsDisabled = externalActionsDisabled;
    }

    public boolean isSharedWorkspace() {
        return sharedWorkspace;
    }

    public void setSharedWorkspace(boolean sharedWorkspace) {
        this.sharedWorkspace = sharedWorkspace;
    }
}
