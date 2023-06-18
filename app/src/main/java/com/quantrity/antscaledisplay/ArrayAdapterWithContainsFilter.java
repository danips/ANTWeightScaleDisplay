package com.quantrity.antscaledisplay;

import android.app.Activity;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ArrayAdapterWithContainsFilter extends ArrayAdapter {

    private final List<String> items;
    private final ArrayList<String> arraylist;

    ArrayAdapterWithContainsFilter(Activity context, int items_view, ArrayList<String> items) {
        super(context, items_view, items);
        this.items = items;
        this.arraylist = new ArrayList<>();
        this.arraylist.addAll(items);
    }

    // Filter Class
    void getContainsFilter(String charText) {
        charText = charText.toLowerCase(Locale.getDefault());
        items.clear();
        if (charText.length() == 0) {
            items.addAll(arraylist);
        }
        else
        {
            for (String item : arraylist)
            {
                if (item.toLowerCase(Locale.getDefault()).contains(charText))
                {
                    items.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }
}
