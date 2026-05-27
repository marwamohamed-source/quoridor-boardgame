package com.quoridor.model;

public class Player {

    //  Fields 
    private final int    id;          // 1 or 2
    private final String name;        // display name
    private final int    goalRow;     // row the player must reach to win

    private Position position;        // current pawn cell
    private int      wallsRemaining;  // walls left to place (starts at 10)

    //Constructor
    public Player(int id, String name, Position start, int goalRow) {
        this.id             = id;
        this.name           = name;
        this.goalRow        = goalRow;
        this.position       = start;
        this.wallsRemaining = 10;
    }

    //Getters & Setters
    public int      getId()             { return id; }
    public String   getName()           { return name; }
    public int      getGoalRow()        { return goalRow; }
    public Position getPosition()       { return position; }
    public int      getWallsRemaining() { return wallsRemaining; }

    public void setPosition(Position position) {
        this.position = position;
    }

    public void useWall() {
        if (wallsRemaining <= 0) {
            throw new IllegalStateException(
                name + " has no walls remaining to place."
            );
        }
        wallsRemaining--;
    }

    public boolean hasWon() {
        return position.row == goalRow;
    }

    //Deep copy 
    public Player copy() {
        Player copy = new Player(id, name, new Position(position.row, position.col), goalRow);
        copy.wallsRemaining = this.wallsRemaining;
        return copy;
    }

    @Override
    public String toString() {
        return name + " @ " + position + " | walls left: " + wallsRemaining;
    }
}
