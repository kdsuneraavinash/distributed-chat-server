package lk.ac.mrt.cse.cs4262.common.utils;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Utility class to read resource files.
 */
public final class FileUtils {
    private static final String SPECIAL_DELIMITER = "\\A";
    private static final String TAB_DELIMITER = "\t";

    private FileUtils() {
    }

    /**
     * Reads the file content from the resources.
     *
     * @param path Path of the file to read.
     * @return Read text. Empty if reading failed.
     */
    public static Optional<String> readResource(String path) {
        ClassLoader classLoader = FileUtils.class.getClassLoader();
        if (classLoader != null) {
            InputStream inputStream = classLoader.getResourceAsStream(path);
            if (inputStream != null) {
                Objects.requireNonNull(inputStream, "File does not exist: " + path);
                InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                Scanner scanner = new Scanner(reader).useDelimiter(SPECIAL_DELIMITER);
                String content = scanner.hasNext() ? scanner.next() : "";
                return Optional.of(content);
            }
        }
        // Fallback to empty string
        return Optional.empty();
    }

    /**
     * Reads a tsv file.
     *
     * @param path    TSV file path.
     * @param columns Number of columns to expect.
     * @return TSV content.
     * @throws IOException If file opening failed.
     */
    public static List<List<String>> readTsv(Path path, int columns) throws IOException {
        return Files.lines(path).filter(line -> !line.isBlank()).map(line -> {
            String[] rowArray = line.split(TAB_DELIMITER);
            if (rowArray.length != columns) {
                String errorMessage = "expected " + columns + " columns in row: " + Arrays.deepToString(rowArray);
                throw new IllegalArgumentException(errorMessage);
            }
            return Arrays.asList(rowArray);
        }).collect(Collectors.toList());
    }
}
