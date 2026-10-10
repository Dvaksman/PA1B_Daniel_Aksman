import java.io.IOException;

/**
 * {@code wc [<file>...]}: prints newline, word, and byte counts for each file, and a total line
 * if more than one file is specified. A word is a non-zero-length sequence of characters
 * delimited by white space.
 */
public class Wc extends ShellCommand {
    /**
     * Format specifier for printing output counts. Do not change.
     */
    public static final String formatSpecifier = "%8d %8d %8d %s%n";

    public Wc(String[] args) {
        super(args);
    }

    public static void main(String[] args) {
        ShellCommand.start(Wc.class, args);
    }

    @Override
    protected void runCommand() throws IOException {
        if (cmdArgs.length == 0) {
            Counts counts = count(System.in);
            System.out.printf(formatSpecifier, counts.lines, counts.words, counts.bytes, "");
            return;
        }

        Counts total = new Counts();
        for (String filename : cmdArgs) {
            java.nio.file.Path file = java.nio.file.Paths.get(filename);
            try {
                try (java.io.InputStream input = getFileInput(file)) {
                    Counts counts = count(input);
                    printCounts(counts, filename);
                    total.add(counts);
                }
            } catch (IllegalArgumentException e) {
                System.err.println(e.getMessage());
                if (java.nio.file.Files.isDirectory(file)) {
                    printCounts(new Counts(), filename);
                }
            }
        }

        if (cmdArgs.length > 1) {
            printCounts(total, "total");
        }
    }

    private Counts count(java.io.InputStream input) throws IOException {
        Counts counts = new Counts();
        String line;
        while ((line = readLineRaw(input)) != null) {
            counts.lines++;
            counts.bytes += line.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            java.util.regex.Matcher words = java.util.regex.Pattern.compile("\\S+").matcher(line);
            while (words.find()) {
                counts.words++;
            }
        }
        return counts;
    }

    private void printCounts(Counts counts, String filename) {
        System.out.printf(formatSpecifier, counts.lines, counts.words, counts.bytes, filename);
    }

    private static class Counts {
        int lines;
        int words;
        int bytes;

        void add(Counts other) {
            lines += other.lines;
            words += other.words;
            bytes += other.bytes;
        }
    }
}
