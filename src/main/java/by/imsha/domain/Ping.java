package by.imsha.domain;

import java.io.Serializable;

/**
 * @author Alena Misan
 */

public class Ping implements Serializable {

    private static final long serialVersionUID = 1L;


    public Ping() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ping)) return false;

        Ping ping1 = (Ping) o;

        if (!name.equals(ping1.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    private String name;

    public Ping(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
