package com.competition.hub.auth;

import com.competition.hub.store.DomainModels.Role;

public record AuthPrincipal(long id, String username, Role role) {
}

