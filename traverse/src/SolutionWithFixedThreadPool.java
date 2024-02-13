import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static java.nio.file.Files.newDirectoryStream;

public class SolutionWithFixedThreadPool {
    private static final Logger logger = Logger.getLogger(SolutionWithFixedThreadPool.class.getName());
    private final ExecutorService executorService;
    private final Consumer<Path> fileProcessor;

    SolutionWithFixedThreadPool(
            final ExecutorService executorService,
            final Consumer<Path> fileProcessor
    ) {
        this.executorService = executorService;
        this.fileProcessor = fileProcessor;
    }

    public void traverse(final Path rootPath) {
        validatePath(rootPath);
        final Stack<Path> pathsToProcess = new Stack<>();
        pathsToProcess.push(rootPath);
        final List<Future<Void>> futures = new ArrayList<>();
        while (!pathsToProcess.isEmpty()) {
            final var pathToProcess = pathsToProcess.pop();
            if (Files.isDirectory(pathToProcess)) {
                System.out.printf("Starting processing of directory: %s...%n", pathToProcess);
                try (final var directoryStream = newDirectoryStream(pathToProcess)) {
                    for (final var entry : directoryStream) {
                        pathsToProcess.push(entry);
                    }
                } catch (final IOException e) {
                    logger.warning("newDirectoryStream(%s) method failed: %s".formatted(rootPath, e.getMessage()));
                }
            } else {
                System.out.printf("Starting processing of file: %s...%n", pathToProcess);
                futures.add(submitFileProcessing(pathToProcess));
            }
        }
        waitFor(futures);
    }

    private Future<Void> submitFileProcessing(final Path pathToProcess) {
        return executorService.submit(() -> {
             fileProcessor.accept(pathToProcess);
             return null;
        });
    }

    private void waitFor(final List<Future<Void>> futures) {
        for (final var futureToWait: futures) {
            try {
                futureToWait.get();
            } catch (final InterruptedException | ExecutionException e) {
                logger.warning("Exception on getting result from future: %s".formatted(e.getMessage()));
            }
        }
    }

    private void validatePath(final Path path) {
        if (!Files.exists(path)) {
            throw new RuntimeException("Path doesn't exists: %s".formatted(path));
        }
    }

    public static void main(final String[] args) {
        try (var executorService = Executors.newFixedThreadPool(12)) {
            SolutionWithFixedThreadPool solution = new SolutionWithFixedThreadPool(
                    executorService,
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
}