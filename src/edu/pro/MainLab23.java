package edu.pro;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MainLab23 {
    private static final Pattern SPLIT_PATTERN = Pattern.compile("\\s+");
    private static final int POOL_SIZE = 4;
    private static final ExecutorService es = Executors.newFixedThreadPool(POOL_SIZE);


    public static void main(String[] args) throws Exception {
        long start = System.nanoTime();
        // before total 208ms
         execute(); // 92ms
        // before total 240m
        // executeLines(); // 116ms

        long finish = System.nanoTime();

        System.out.println("------");
        System.out.println("Execution Time: " + TimeUnit.NANOSECONDS.toMillis(finish - start) + "ms");

    }

    private static void execute() throws Exception {
        Path path = Paths.get("src/edu/pro/txt/harry.txt");
        String content = Files.readString(path);

        List<String> lines = content.lines().toList();
        int numberOfLines = lines.size();

        ConcurrentHashMap<String, Long> wordCounts = new ConcurrentHashMap<>();

        int chunkSize = (int) Math.ceil((double) numberOfLines / POOL_SIZE);

        List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < POOL_SIZE; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, numberOfLines);

            Callable<Void> task = createTask(lines.subList(start, end), wordCounts);
            futures.add(es.submit(task));
        }

        for (Future<Void> future : futures) {
            future.get();
        }

        es.shutdown();

        wordCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(30)
                .forEach(entry ->
                        System.out.println(entry.getKey() + ": " + entry.getValue()));
    }

    private static void executeLines() throws IOException {
        Path path = Paths.get("src/edu/pro/txt/harry.txt");
        String content = Files.readString(path);
        content.lines().parallel()
                .map(MainLab23::clean)
                .flatMap(SPLIT_PATTERN::splitAsStream)
                .filter(word -> !word.isEmpty())
                .map(String::intern)
                .collect(Collectors.groupingBy(w -> w, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(30)
                .forEach(entry ->
                        System.out.println(entry.getKey() + ": " + entry.getValue()));

    }

    private static Callable<Void> createTask(List<String> lines, ConcurrentHashMap<String, Long> wordCounts) {
        return () -> {
            for (String line : lines) {
                String[] words = SPLIT_PATTERN.split(clean(line));  // Split line into words
                for (String word : words) {
                    if (!word.isBlank()) {
                        wordCounts.merge(word, 1L, Long::sum);
                    }
                }
            }
            return null;
        };
    }

    private static StringBuilder clean(String line) {
        char[] chars = line.toCharArray();
        StringBuilder sb = new StringBuilder();
        for (char c : chars) {
            if (Character.isLetter(c)) {
                if (Character.isUpperCase(c)) {
                    c = Character.toLowerCase(c);
                }
                sb.append(c);
            } else if (Character.isWhitespace(c)) {
                sb.append(c);
            }
        }
        return sb;
    }
}
