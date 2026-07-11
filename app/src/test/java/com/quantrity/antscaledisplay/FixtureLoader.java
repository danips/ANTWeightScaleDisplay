package com.quantrity.antscaledisplay;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class FixtureLoader {
    private FixtureLoader() {}

    static String load(String name) throws IOException {
        try (InputStream input = FixtureLoader.class.getResourceAsStream("/fixtures/" + name)) {
            if (input == null) throw new IOException("Missing fixture: " + name);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }
}
