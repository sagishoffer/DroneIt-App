package com.o3dr.droneit;

import com.google.android.gms.maps.model.LatLng;
import com.parse.ParseObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import bolts.Task;


public interface DroneItDB {
    public ArrayList<LatLng> getPath(ParseObject levelObj);
    public boolean insertScore(int time, String levelId);
    public Task<ParseObject> getLevelObject(int level);
    public Task<List<ParseObject>> getUserLevelsTask();
    public HashSet<Integer> getUserLevels(List<ParseObject> recordsList);
    public Task<List<ParseObject>> getDroneLevelsTask();
    public HashSet<Integer> getDroneLevels(List<ParseObject> recordsList);
    public Task<List<ParseObject>> getLevelHighScoresTask(int levelNumber);
    public ArrayList<Score> getLevelHighScores(List<ParseObject> recordsList);

}
