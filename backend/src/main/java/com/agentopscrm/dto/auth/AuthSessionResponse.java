package com.agentopscrm.dto.auth;

public class AuthSessionResponse {

    private String token;
    private AuthUserResponse user;

    public AuthSessionResponse() {
    }

    public AuthSessionResponse(String token, AuthUserResponse user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public AuthUserResponse getUser() {
        return user;
    }

    public void setUser(AuthUserResponse user) {
        this.user = user;
    }
}
