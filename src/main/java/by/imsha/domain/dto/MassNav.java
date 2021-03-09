package by.imsha.domain.dto;

import lombok.Data;

import java.util.Set;
import java.util.TreeMap;

@Data
public class MassNav {
    private TreeMap<String, Set<MassFilterValue>> selected;
    private TreeMap<String, Set<MassFilterValue>> guided;
}
