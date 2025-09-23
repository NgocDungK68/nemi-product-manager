package com.nemi.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LanguageChecker {

    public static boolean isEn(HttpServletRequest request) {
        String language = request.getHeader("Accept-Language");
        if (language != null && !language.isEmpty()) {
            String[] languages = language.split(",");
            for (String lang : languages) {
                if (lang.trim().startsWith("en")) {
                    return true;
                }
            }
        }
        return false;
    }
}
