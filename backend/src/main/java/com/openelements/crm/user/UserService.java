package com.openelements.crm.user;

import org.springframework.stereotype.Service;

/**
 * Provides the current user's information.
 * Currently returns a hardcoded dummy user.
 * Will be replaced with Authentik/OIDC token extraction when SSO is integrated.
 */
@Service
public class UserService {

    private static final UserInfo DUMMY_USER = new UserInfo("Demo User", "demo@example.com");

    /**
     * Returns the current user's information.
     *
     * @return the current user info
     */
    public UserInfo getCurrentUser() {
        return DUMMY_USER;
    }
}
