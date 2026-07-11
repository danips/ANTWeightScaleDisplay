package com.quantrity.antscaledisplay;

import org.json.JSONArray;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class UserJsonCodec {
    RepositoryResult<List<User>> decode(String json) {
        try {
            JSONArray array = new JSONArray(json);
            ArrayList<User> users = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); i++) users.add(new User(array.getJSONObject(i)));
            return RepositoryResult.success(users);
        } catch (Exception e) {
            return RepositoryResult.failure("Could not decode users", e);
        }
    }

    RepositoryResult<String> encode(List<User> users) {
        try {
            ArrayList<User> sorted = new ArrayList<>(users);
            Collator collator = Collator.getInstance();
            Collections.sort(sorted, (first, second) -> collator.compare(first.name, second.name));
            JSONArray array = new JSONArray();
            for (User user : sorted) array.put(user.serializeToObj());
            return RepositoryResult.success(array.toString());
        } catch (Exception e) {
            return RepositoryResult.failure("Could not encode users", e);
        }
    }
}
