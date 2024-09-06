package by.imsha.utils;

import by.imsha.domain.City;
import by.imsha.domain.LocalizedCity;
import by.imsha.domain.LocalizedMass;
import by.imsha.domain.LocalizedParish;
import by.imsha.domain.Mass;
import by.imsha.domain.Parish;
import lombok.experimental.UtilityClass;

import java.util.Optional;

/**
 * @deprecated необходимо или изменить в будущем подход к работе с локализацией или ничего не делать)
 */
@UtilityClass
@Deprecated
public class LocalizedUtils {

    public static String getLocalizedCityName(final City city, final String locale) {
        return Optional.ofNullable(city.getLocalizedInfo().get(locale))
                .map(localizedBaseInfo -> ((LocalizedCity) localizedBaseInfo).getName())
                .orElseGet(city::getName);
    }

    public static String getLocalizedMassNotes(final Mass mass, final String locale) {
        LocalizedMass localizedMass = mass.getLocalizedInfo().get(locale);
        String calculatedNotes = null;
        if (localizedMass != null) {
            calculatedNotes = localizedMass.getNotes();
        } else if (Constants.DEFAULT_LANG.equalsIgnoreCase(locale)) {
            calculatedNotes = mass.getNotes();
        }
        return calculatedNotes;
    }

    public static String getLocalizedParishName(final Parish parish, final String locale) {
        return Optional.ofNullable(parish.getLocalizedInfo().get(locale))
                .map(localizedBaseInfo -> ((LocalizedParish) localizedBaseInfo).getName())
                .orElseGet(parish::getName);
    }

    public static String getLocalizedParishShortName(final Parish parish, final String locale) {
        return Optional.ofNullable(parish.getLocalizedInfo().get(locale))
                .map(localizedBaseInfo -> ((LocalizedParish) localizedBaseInfo).getShortName())
                .orElseGet(parish::getShortName);
    }

    public static String getLocalizedParishAddress(final Parish parish, final String locale) {
        return Optional.ofNullable(parish.getLocalizedInfo().get(locale))
                .map(localizedBaseInfo -> ((LocalizedParish) localizedBaseInfo).getAddress())
                .orElseGet(parish::getAddress);
    }
}
