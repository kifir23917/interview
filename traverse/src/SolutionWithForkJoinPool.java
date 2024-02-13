import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static java.nio.file.Files.newDirectoryStream;

public class SolutionWithForkJoinPool {
    static Logger logger = Logger.getLogger(SolutionWithForkJoinPool.class.getName());
    private final Consumer<Path> fileProcessor;

    SolutionWithForkJoinPool(final Consumer<Path> fileProcessor) {
        this.fileProcessor = fileProcessor;
    }

    public void traverse(final Path rootPath) {
        validatePath(rootPath);
        try (ForkJoinPool pool = new ForkJoinPool()) {
            pool.invoke(new EntryProcessingTask(rootPath, fileProcessor));
        }
    }

    private void validatePath(Path path) {
        if (!Files.exists(path)) {
            throw new RuntimeException("Path doesn't exists: %s".formatted(path));
        }
    }

    private static class EntryProcessingTask extends RecursiveTask<Void> {
        static Logger logger = Logger.getLogger(EntryProcessingTask.class.getName());
        private final Path entryPath;
        private final Consumer<Path> fileProcessor;

        EntryProcessingTask(
                final Path entryPath,
                final Consumer<Path> fileProcessor
        ) {
            this.entryPath = entryPath;
            this.fileProcessor = fileProcessor;
        }

        @Override
        protected Void compute() {
            if (Files.isDirectory(entryPath)) {
                System.out.printf("Starting processing of directory: %s...%n", entryPath);
                try (final var directoryStream = newDirectoryStream(entryPath)) {
                    for (final var entry : directoryStream) {
                        EntryProcessingTask subTask = new EntryProcessingTask(entry, fileProcessor);
                        subTask.fork();
                    }
                } catch (IOException e) {
                    logger.warning("newDirectoryStream(%s) method failed: %s".formatted(entryPath, e.getMessage()));
                }

            } else {
                System.out.printf("Starting processing of file: %s...%n", entryPath);
                fileProcessor.accept(entryPath);
            }
            return null;
        }
    }

    public static void main(final String[] args) {
        SolutionWithForkJoinPool solution = new SolutionWithForkJoinPool(
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
