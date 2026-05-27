package com.quoridor.model;

import java.util.Objects;

public class Position {

    public final int row;
    public final int col;

    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    //Returns the position one step in the given direction
    public Position step(Direction dir) {
        return switch (dir) {
            case UP    -> new Position(row - 1, col);
            case DOWN  -> new Position(row + 1, col);
            case LEFT  -> new Position(row,     col - 1);
            case RIGHT -> new Position(row,     col + 1);
        };
    }

    //Returns true if this position is inside the 9x9 board
    public boolean isValid() {
        return row >= 0 && row <= 8 && col >= 0 && col <= 8;
    }

    //Object overrides
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position p)) return false;
        return row == p.row && col == p.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }

    @Override
    public String toString() {
        return "(" + row + ", " + col + ")";
    }
}
