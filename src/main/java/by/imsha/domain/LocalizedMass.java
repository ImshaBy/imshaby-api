package by.imsha.domain;

import lombok.Data;

@Data
public class LocalizedMass {

    private String notes;

    public LocalizedMass() {
    }
    public LocalizedMass(String notes) {
        this.notes = notes;
    }
}
