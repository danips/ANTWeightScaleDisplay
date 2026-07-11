package com.quantrity.antscaledisplay;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

final class WeightJsonCodec {
    RepositoryResult<List<Weight>> decode(String json) {
        try {
            JSONArray array = new JSONArray(json);
            ArrayList<Weight> weights = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); i++) weights.add(new Weight(array.getJSONObject(i)));
            return RepositoryResult.success(weights);
        } catch (Exception e) {
            return RepositoryResult.failure("Could not decode weight history", e);
        }
    }

    RepositoryResult<String> encode(List<Weight> weights) {
        try {
            JSONArray array = new JSONArray();
            for (Weight weight : new ArrayList<>(weights)) array.put(weight.serializeToObj());
            return RepositoryResult.success(array.toString());
        } catch (Exception e) {
            return RepositoryResult.failure("Could not encode weight history", e);
        }
    }
}
