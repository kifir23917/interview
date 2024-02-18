import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class SolutionWithParallelStream {
    private static final Logger logger = Logger.getLogger(SolutionWithParallelStream.class.getName());

    private final Consumer<Path> fileProcessor;

    SolutionWithParallelStream(final Consumer<Path> fileProcessor) {
        this.fileProcessor = fileProcessor;
    }

    public void traverse(final Path rootPath) {
        validatePath(rootPath);
        processEntry(rootPath);
    }

    private void processEntry(final Path entryPath) {
        if (Files.isDirectory(entryPath)) {
            System.out.printf("Starting processing of directory: %s...%n", entryPath);
            try (final var childrenStream = Files.list(entryPath)) {
                try (final var childrenParallelStream = childrenStream.parallel()) {
                    childrenParallelStream.forEach(this::processEntry);
                }
            } catch (final IOException e) {
                logger.warning("newDirectoryStream(%s) method failed: %s".formatted(entryPath, e.getMessage()));
            }
        } else {
            System.out.printf("Starting processing of file: %s...%n", entryPath);
            fileProcessor.accept(entryPath);
        }
    }

    private void validatePath(final Path path) {
        if (!Files.exists(path)) {
            throw new RuntimeException("Path doesn't exists: %s".formatted(path));
        }
    }

    public static void main(final String[] args) {
        SolutionWithParallelStream solution = new SolutionWithParallelStream(
                (file) -> {
                    System.out.printf("        Processing of file: %s started%n", file);
                    try {
                        Thread.sleep(file.toString().length());
                    } catch (InterruptedException e) {
                        logger.warning("Sleep was interrupted for file %s".formatted(file));
                    }
                    System.out.printf("        Processing of file: %s finished%n", file);
                }
        );
        System.out.println("Starting files traverse...");
        final long start = System.currentTimeMillis();
        solution.traverse(Path.of("C:\\Windows\\System32\\Drivers"));
        final long finish = System.currentTimeMillis();
        System.out.printf("Files traverse finished in %d ms...%n", finish - start);
    }
}
