package com.example.injehealth;

import java.util.List;

public class SetData {
    public String routineName;
    public List<String> exerciseName;
    public int    setNumber;
    public int    plannedReps;
    public double plannedWeight;
    public int    actualReps;
    public double actualWeight;
    public boolean isDone;

    public SetData(String routineName, List<String>exerciseName, int setNumber, int plannedReps, double plannedWeight) {
        this.routineName   = routineName;
        this.exerciseName  = exerciseName;
        this.setNumber     = setNumber;
        this.plannedReps   = plannedReps;
        this.plannedWeight = plannedWeight;
    }
}