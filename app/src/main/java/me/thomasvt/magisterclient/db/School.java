package me.thomasvt.magisterclient.db;

/**
 * Created by me on 4/1/2015.
 */
public class School {
    public School() {}

    public School(String name, String host) {
        this.name = name;
        this.host = host;
    }

    public String host;
    public String name;
    public int id;
    public boolean favourite;

    // Compatibiliteit met o.a. ArrayAdapter
    public String toString() {
        return name;
    }
}
