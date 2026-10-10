/**
 * {@code grep <pattern> [<file>...]}: prints every line, from the given files or from standard
 * input, that matches the {@link java.util.regex.Pattern regular expression} {@code pattern}.
 *
 * <p>With more than one file, each matching line is prefixed with {@code "<filename>:"}.
 */
public class Grep extends ShellCommand {

    public Grep(String[] args) {
        super(args);
    }

    public static void main(String[] args) {
        ShellCommand.start(Grep.class, args);
    }

    @Override
    protected void runCommand() throws java.io.IOException {
        if (cmdArgs.length == 0) {
            System.err.println("Usage: grep <pattern> [<file>...]");
            return;
        }

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(cmdArgs[0]);
        boolean hasFiles = cmdArgs.length > 1;
        boolean prefixMatches = cmdArgs.length > 2;

        if (!hasFiles) {
            printMatches(System.in, pattern, null);
            return;
        }

        for (int index = 1; index < cmdArgs.length; index++) {
            String filename = cmdArgs[index];
            try {
                java.nio.file.Path file = java.nio.file.Paths.get(filename);
                try (java.io.InputStream input = getFileInput(file)) {
                    printMatches(input, pattern, prefixMatches ? filename : null);
                }
            } catch (IllegalArgumentException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private void printMatches(java.io.InputStream input, java.util.regex.Pattern pattern,
            String filename) throws java.io.IOException {
        String line;
        while ((line = readLineRaw(input)) != null) {
            if (pattern.matcher(line).find()) {
                if (filename != null) {
                    System.out.print(filename + ":");
                }
                System.out.print(line);
            }
        }
    }
}
