package by.imsha.domain;

import by.imsha.utils.CoordinateTools;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import java.text.DecimalFormat;

/**
 * A class to represent a latitude and longitude
 *
 *
 */
@Getter
public class Coordinate implements Comparable<Coordinate>{

    // declare private class level variables
    private Float latitude;
    private Float longitude;

    @JsonIgnore
    private final static DecimalFormat format = new DecimalFormat("##.######");

    /**
     * Constructor for this class
     *
     * @param latitude a latitude coordinate in decimal notation
     * @param longitude a longitude coordinate in decimal notation
     */
    public Coordinate(float latitude, float longitude) {

        if(CoordinateTools.isValidLatitude(latitude) == true && CoordinateTools.isValidLongitude(longitude) == true) {
            this.latitude = latitude;
            this.longitude = longitude;
        } else {
            throw new IllegalArgumentException("The parameters did not pass validation as defined by the CoordinateTools class");
        }

    }

    public Coordinate() {
    }


    public void setLatitude(Float latitude) {
        if(latitude!= null && CoordinateTools.isValidLatitude(latitude) == true) {
            this.latitude = latitude;
        } else {
            throw new IllegalArgumentException("The parameter did not pass validation as defined by the CoordinateTools class");
        }
    }

    public void setLongitude(Float longitude) {
        if(longitude != null && CoordinateTools.isValidLongitude(longitude) == true) {
            this.longitude = longitude;
        } else {
            throw new IllegalArgumentException("The parameter did not pass validation as defined by the CoordinateTools class");
        }
    }

    public String getLatitudeAsString() {

        return format.format(latitude);
    }

    public String getLongitudeAsString() {
        return format.format(longitude);
    }

    public String toString() {
        return format.format(latitude) + ", " + format.format(longitude);
    }


    /**
     * A method to determine if one event is the same as another
     *
     * @param o the object to compare this one to
     *
     * @return  true if they are equal, false if they are not
     */
    public boolean equals(Object o) {
        // check to make sure the object is an event
        if ((o instanceof Coordinate) == false) {
            // o is not an event object
            return false;
        }

        // compare these two events
        Coordinate c = (Coordinate)o;

        // build items for comparison
        String me  = this.getLatitudeAsString() + this.getLongitudeAsString();
        String you = c.getLatitudeAsString() + c.getLongitudeAsString();

        return me.equals(you);

    } // end equals method

    /**
     * Overide the default hashcode method
     *
     * @return a hashcode for this object
     */
    public int hashCode() {

        String me = this.getLatitudeAsString() + this.getLongitudeAsString();
        return 31*me.hashCode();
    }

    /**
     * The compareTo method compares the receiving object with the specified object and returns a
     * negative integer, 0, or a positive integer depending on whether the receiving object is
     * less than, equal to, or greater than the specified object.
     *
     * @param c the event to compare this one to
     *
     * @return  an integer indicating comparison result
     */
    public int compareTo(Coordinate c) {

        String me  = this.getLatitudeAsString() + this.getLongitudeAsString();
        String you = c.getLatitudeAsString() + c.getLongitudeAsString();

        Double meDbl  = Double.valueOf(me);
        Double youDbl = Double.valueOf(you);

        if(meDbl == youDbl) {
            return 0;
        } else {
            Double tmp = Math.floor(meDbl - youDbl);
            return tmp.intValue();
        }

    } // end compareTo method
}


