package com.sky.context;

public class AuthContext {

    private static final ThreadLocal<String> CURRENT_IDENTITY = new ThreadLocal<>();

    public static void setCurrentIdentity(String identity) {
        CURRENT_IDENTITY.set(identity);
    }

    public static String getCurrentIdentity() {
        return CURRENT_IDENTITY.get();
    }

    public static void remove() {
        CURRENT_IDENTITY.remove();
    }
}
