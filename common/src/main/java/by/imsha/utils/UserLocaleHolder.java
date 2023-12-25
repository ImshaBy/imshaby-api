package by.imsha.utils;


import lombok.experimental.UtilityClass;

import java.util.Optional;

@UtilityClass
public class UserLocaleHolder {

    private static final ThreadLocal<String> USER_LOCALE = new ThreadLocal<>();

    public static Optional<String> getUserLocale() {
        return Optional.ofNullable(USER_LOCALE.get());
    }

    public static void setUserLocale(final String locale) {
        USER_LOCALE.set(locale);
    }

    public static void resetUserLocale() {
        USER_LOCALE.remove();
    }
}
