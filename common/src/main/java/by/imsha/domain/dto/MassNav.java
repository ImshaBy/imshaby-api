package by.imsha.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.TreeMap;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MassNav {
    public final static MassNav EMPTY_NAV = new MassNav(new TreeMap<>(), new TreeMap<>());
    private TreeMap<String, MassFilterValue> selected;
    private TreeMap<String, Set<MassFilterValue>> guided;
}
