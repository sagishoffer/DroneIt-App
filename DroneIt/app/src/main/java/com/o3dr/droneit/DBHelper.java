package com.o3dr.droneit;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import bolts.Task;

public class DBHelper implements DroneItDB {

    public Task<ParseObject> getLevelObject(int level) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Level").whereEqualTo("levelNumber", level);
        return query.getFirstInBackground();
    }

    public ArrayList<LatLng> getPath(ParseObject levelObj) {

        //ParseObject levelObj = getLevelObject(level);

        if(levelObj == null)
            return null;

        try {
            JSONArray pathArr = levelObj.getJSONArray("Path");
            ArrayList<LatLng> points = null;
            if(pathArr != null) {
                points = new ArrayList<>();
                for (int j = 0; j < pathArr.length(); j++) {
                    JSONArray point = pathArr.getJSONArray(j);
                    points.add(new LatLng(point.getDouble(0), point.getDouble(1)));
                }
            }
            return points;
        }
        catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Task<List<ParseObject>> getUserLevelsTask() {
        ParseUser userId = ParseUser.getCurrentUser();
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Scores").whereEqualTo("user", userId);
        query.include("level");
        return query.findInBackground();
    }

    public HashSet<Integer> getUserLevels(List<ParseObject> recordsList) {
        HashSet<Integer> levels = new HashSet<>();

        for (int i=0; i<recordsList.size(); i++) {
            ParseObject record = recordsList.get(i);

            ParseObject levelObject = record.getParseObject("level");
            int levelNum = levelObject.getInt("levelNumber");
            levels.add(levelNum);
        }

        return levels;
    }

    @Override
    public Task<List<ParseObject>> getDroneLevelsTask() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Level");
        return query.findInBackground();
    }

    @Override
    public HashSet<Integer> getDroneLevels(List<ParseObject> recordsList) {
        HashSet<Integer> levels = new HashSet<>();

        for (int i=0; i<recordsList.size(); i++) {
            ParseObject record = recordsList.get(i);
            int levelNum = record.getInt("levelNumber");
            levels.add(levelNum);
        }

        return levels;
    }

    public Task<List<ParseObject>> getLevelHighScoresTask(int levelNumber) {
        ParseQuery<ParseObject> innerQuery = ParseQuery.getQuery("Level");
        innerQuery.whereEqualTo("levelNumber", levelNumber);

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Scores").whereMatchesQuery("level", innerQuery).orderByAscending("timeScore");
        query.include("user");
        return query.findInBackground();
    }

    public ArrayList<Score> getLevelHighScores(List<ParseObject> recordsList) {
        ArrayList<Score> scores = new ArrayList<>();

        for (int i=0; i<recordsList.size(); i++) {
            ParseObject record = recordsList.get(i);

            ParseObject userObject = record.getParseObject("user");
            String username = userObject.getString("username");
            int time = record.getInt("timeScore");
            scores.add(new Score((i+1),username, time));
        }

        return scores;
    }

    public boolean insertScore(int time, String levelId)
    {
        String userId = ParseUser.getCurrentUser().getObjectId();
        ParseObject user = ParseObject.createWithoutData("_User", userId);
        ParseObject level = ParseObject.createWithoutData("Level", levelId);

        ParseObject gameScore = new ParseObject("Scores");
        gameScore.put("timeScore", time);
        gameScore.put("level", level);
        gameScore.put("user", user);
        gameScore.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                Log.i("DBHelper", "Score was saved");
            }
        });

        return true;
    }

    public ArrayList<HashMap<String, String>> getAllHighScores(int level)
    {
        return null;
    }

}
