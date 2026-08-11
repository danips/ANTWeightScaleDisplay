package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;

public class CsvExporterTest {
    @Test
    public void exportsRepresentativeHistorySizesAndReportsTheirCounts() {
        for (int count : new int[] {100, 1_000, 10_000}) {
            ArrayList<Weight> weights = weights(count);
            StringWriter output = new StringWriter();

            RepositoryResult<Integer> result = CsvExporter.writeWithLabels(
                    output, resourceId -> "label-" + resourceId, user(), weights);

            assertTrue(result.isSuccess());
            assertEquals(Integer.valueOf(count), result.value);
            assertEquals(count + 1, output.toString().split("\n").length);
            assertTrue(output.toString().contains("person,"));
        }
    }

    @Test
    public void writeFailureNeverReturnsSuccess() {
        Writer failing = new Writer() {
            @Override public void write(char[] chars, int offset, int length) throws IOException {
                throw new IOException("destination rejected write");
            }

            @Override public void flush() {}

            @Override public void close() {}
        };

        RepositoryResult<Integer> result = CsvExporter.writeWithLabels(
                failing, resourceId -> "label", user(), Collections.singletonList(weight(0)));

        assertFalse(result.isSuccess());
        assertEquals("Could not export CSV history", result.message);
    }

    private static ArrayList<Weight> weights(int count) {
        ArrayList<Weight> weights = new ArrayList<>();
        for (int index = 0; index < count; index++) weights.add(weight(index * 1_000L));
        return weights;
    }

    private static Weight weight(long date) {
        Weight weight = new Weight();
        weight.date = date;
        weight.uuid = "user";
        weight.weight = 70;
        return weight;
    }

    private static User user() {
        User user = new User();
        user.name = "person";
        user.mass_unit = User.MassUnit.KG;
        return user;
    }
}
