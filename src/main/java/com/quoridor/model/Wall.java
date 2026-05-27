package com.quoridor.model;

public class Wall {

    public enum Orientation {
        HORIZONTAL, VERTICAL
    }

    // Fields 
    private final Orientation orientation;
    private final Position anchor;   // top-left cell of the wall's span

    // Constructor 
    public Wall(Orientation orientation, Position anchor) {
        this.orientation = orientation;
        this.anchor      = anchor;
    }

    //Getters 
    public Orientation getOrientation() { return orientation; }
    public Position    getAnchor()      { return anchor; }

    //Convenience 
    public boolean isHorizontal() { return orientation == Orientation.HORIZONTAL; }
    public boolean isVertical()   { return orientation == Orientation.VERTICAL;   }

    @Override
    public String toString() {
        return orientation + " wall @ " + anchor;
    }
}
