package info.kgeorgiy.ja.goge.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

public class BaseWalk {
    private final HashMaker hashMaker;
    private final Path inputFile;
    private final Path outputFile;

    public BaseWalk(String[] args) throws WalkException {
        if (args == null || args.length < 2 || args.length > 3 || args[0] == null || args[1] == null) {
            throw new WalkException("Illegal arguments or number of arguments");
        }

        // :NOTE: copy-paste
        try {
            inputFile = Path.of(args[0]);
        } catch (InvalidPathException e) {
            throw new WalkException("Illegal path to input file");
        }
        try {
            outputFile = Path.of(args[1]);
        } catch (InvalidPathException e) {
            throw new WalkException("Illegal path to output file");
        }

        final String algorithm = args.length == 3 ? args[2] : "jenkins";
        try {
            hashMaker = new HashMaker(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new WalkException("Illegal algorithm " + algorithm);
        }
    }

    public void walk(int depth) throws WalkException {
        try {
            Path outputFileParent = outputFile.getParent();
            if (outputFileParent == null) {
                throw new WalkException("output file is null");
            }
            Files.createDirectories(outputFileParent);
        } catch (InvalidPathException e) {
            throw new WalkException("Output file cannot be created");
        } catch (SecurityException | IOException ignored) {}

        // :NOTE: encoding
        try (BufferedReader reader = Files.newBufferedReader(inputFile)) {
            try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
                FileVisitor<Path> fileVisitor = getFileVisitor(writer);

                String line;
                try {
                    while ((line = reader.readLine()) != null) { // :NOTE: Output file exception тут Input
                        try {
                            Files.walkFileTree(Path.of(line), Set.of(), depth, fileVisitor);
                        } catch (InvalidPathException e) {
                            // :NOTE: misleading message
                            writer.write(outputFormat(hashMaker.zeroHash(), line));
                        } catch (SecurityException | IOException e) {
                            throw new WalkException("Output file exception", e);
                        }
                    }
                } catch (SecurityException | IOException e) {
                    // :NOTE: e.getMessage()
                    throw new WalkException("Input file exception", e);
                }
            } catch (SecurityException | IOException e) {
                throw new WalkException("Output file exception", e);
            }
        } catch (SecurityException | IOException e) {
            throw new WalkException("Can not open input file to write", e);
        }
    }

    private SimpleFileVisitor<Path> getFileVisitor(BufferedWriter writer) {
        return new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                write(hashMaker.countHash(file), file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                write(hashMaker.zeroHash(), file);
                return FileVisitResult.CONTINUE;
            }

            private void write(String hash, Path file) throws IOException {
                writer.write(outputFormat(hash, file.toString()));
            }
        };
    }

    private String outputFormat(String hash, String file) {
        return String.format("%s %s%n", hash, file);
    }
}
