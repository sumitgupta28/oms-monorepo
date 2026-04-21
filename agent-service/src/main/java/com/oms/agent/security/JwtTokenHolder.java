package com.oms.agent.security;

public class JwtTokenHolder {

    public static final String CONTEXT_KEY = "jwt-token";
    private static final ThreadLocal<String> TOKEN = new ThreadLocal<>();

    public static String get() { return TOKEN.get(); }
    public static void set(String token) { TOKEN.set(token); }
    public static void clear() { TOKEN.remove(); }
}
