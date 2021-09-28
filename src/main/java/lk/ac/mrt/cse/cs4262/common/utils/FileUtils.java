package lk.ac.mrt.cse.cs4262.common.utils;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Utility class to read resource files.
 */
public final class FileUtils {
    private static final String SPECIAL_DELIMITER = "\\A";

    private FileUtils() {
    }

    /**
     * Reads the file content from the resources.
     *
     * @param path Path of the file to read.
     * @return Read text.
     */
    public static String readResource(String path) {
        ClassLoader classLoader = FileUtils.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(path);
        Objects.requireNonNull(inputStream, "File does not exist: " + path);
        InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        Scanner scanner = new Scanner(reader).useDelimiter(SPECIAL_DELIMITER);
        return scanner.hasNext() ? scanner.next() : "";
    }

    /**
     * Reads a tsv file.
     *
     * @param path TSV file path.
     * @return TSV content.
     * @throws IOException If file opening failed.
     */
    public static List<String[]> readTsv(String path) throws IOException {
        return Files.lines(Path.of(path))
                .map(line -> line.split("\t"))
                .collect(Collectors.toList());
    }
}
