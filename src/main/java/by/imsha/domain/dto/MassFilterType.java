package by.imsha.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum MassFilterType  {
    DAY ("day", 0),
    CITY ("city", 1),
    PARISH ("parish", 2),
    ONLINE ("online", 3),
    LANG ("lang", 4);
    @Getter  private String name;
    @Getter private int priority;

}
