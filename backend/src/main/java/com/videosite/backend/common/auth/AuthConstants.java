package com.videosite.backend.common.auth;

public final class AuthConstants {

    public static final String VISITOR_ID_ATTR = "visitorId";
    public static final String VISITOR_ID_HEADER = "X-Visitor-Id";
    public static final String VISITOR_ID_COOKIE = "visitor_id";

    public static final String ADMIN_SESSION_KEY = "ADMIN_LOGIN_USER";

    public static final String USER_SESSION_USER_ID_KEY = "USER_LOGIN_ID";
    public static final String USER_SESSION_USERNAME_KEY = "USER_LOGIN_NAME";

    private AuthConstants() {
    }
}
