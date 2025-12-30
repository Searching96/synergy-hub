package com.synergyhub.util;

import jakarta.servlet.http.HttpServletRequest;

public class WebUtils {
    private WebUtils() {}

    public static String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
