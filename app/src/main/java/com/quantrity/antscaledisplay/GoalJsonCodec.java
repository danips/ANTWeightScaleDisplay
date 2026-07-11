package com.quantrity.antscaledisplay;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class GoalJsonCodec {
    RepositoryResult<List<Goal>> decode(String json) {
        try {
            JSONArray array = new JSONArray(json);
            ArrayList<Goal> goals = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); i++) goals.add(new Goal(array.getJSONObject(i)));
            return RepositoryResult.success(goals);
        } catch (Exception e) {
            return RepositoryResult.failure("Could not decode goals", e);
        }
    }

    RepositoryResult<String> encode(List<Goal> goals) {
        try {
            ArrayList<Goal> sorted = new ArrayList<>(goals);
            Collections.sort(sorted, new Goal.EndDateComparator());
            JSONArray array = new JSONArray();
            for (Goal goal : sorted) array.put(goal.serializeToObj());
            return RepositoryResult.success(array.toString());
        } catch (Exception e) {
            return RepositoryResult.failure("Could not encode goals", e);
        }
    }
}
