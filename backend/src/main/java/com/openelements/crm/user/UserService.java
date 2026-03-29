package com.openelements.crm.user;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    public UserInfo getCurrentUser() {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof final Jwt jwt)) {
            throw new IllegalStateException("No authenticated user in SecurityContext");
        }
        final String name = jwt.getClaimAsString("name");
        final String email = jwt.getClaimAsString("email");
        return new UserInfo(
                name != null ? name : "Unknown",
                email != null ? email : ""
        );
    }
}
