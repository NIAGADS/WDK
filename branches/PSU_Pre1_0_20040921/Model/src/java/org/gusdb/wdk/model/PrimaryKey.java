package org.gusdb.wdk.model;

public class PrimaryKey {

    private String key;
    
    public PrimaryKey(Object o) {
        if (o instanceof String) {
            key = (String) o;
        } else {
            key = o.toString();
        }
    }
    
    public String toString() {
        return key;
    }
    
}
