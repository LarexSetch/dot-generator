package ru.airux.dot_generator;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FilesCollector {
    public static List<File> getFiles(String directory) throws IOException {
        return new LinkedList<>(walkRec(Paths.get(directory)));
    }

    private static List<File> walkRec(Path path) throws IOException {
        try (Stream<Path> paths = Files.walk(path)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(item -> Files.isRegularFile(item) && FilenameUtils.getExtension(item.toFile().getName()).equals("php"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        }
    }
}
