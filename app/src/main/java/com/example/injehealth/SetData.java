package com.example.injehealth;

public class SetData {
    public String exerciseName;
    public int    setNumber;
    public int    plannedReps;
    public double plannedWeight;
    public int    actualReps;
    public double actualWeight;
    public boolean isDone;

    public SetData(String exerciseName, int setNumber, int plannedReps, double plannedWeight) {
        this.exerciseName  = exerciseName;
        this.setNumber     = setNumber;
        this.plannedReps   = plannedReps;
        this.plannedWeight = plannedWeight;
    }
}