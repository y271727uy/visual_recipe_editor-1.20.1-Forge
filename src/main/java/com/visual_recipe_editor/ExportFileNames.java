package com.visual_recipe_editor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

public final class ExportFileNames {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder FILE_NAME_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final int RANDOM_BYTE_COUNT = 9;
    private static final int MAX_ATTEMPTS = 32;

    private ExportFileNames() {
    }

    public static Path createUniqueJsonExportPath(Path directory) throws IOException {
        Files.createDirectories(directory);

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            byte[] randomBytes = new byte[RANDOM_BYTE_COUNT];
            RANDOM.nextBytes(randomBytes);

            // Encode with URL-safe Base64, then convert to a lowercase-only filename
            // and replace any '-' with '_' so the only allowed symbol is '_'.
            String encoded = FILE_NAME_ENCODER.encodeToString(randomBytes)
                    .replace('-', '_')
                    .toLowerCase(Locale.ROOT);
            Path candidate = directory.resolve(encoded + ".json");
            if (Files.notExists(candidate)) {
                return candidate;
            }
        }

        throw new IOException("Unable to generate a unique exported recipe file name.");
    }
}

