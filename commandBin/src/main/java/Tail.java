import java.io.IOException;

/**
 * {@code tail [-n <count>] [<file>...]}: prints the last {@code count} lines (default {@code 10})
 * of each file.
 *
 * <p>With more than one file, each file's output is preceded by an {@code "==> <file> <=="}
 * header line, with a blank line separating consecutive files' output (no header for a single
 * file or standard input).
 */
public class Tail extends ShellCommand {
    private static final int DEFAULT_COUNT = 10;

    public Tail(String[] args) {
        super(args);
    }

    public static void main(String[] args) {
        ShellCommand.start(Tail.class, args);
    }

    @Override
    protected void runCommand() throws IOException {
        int count = DEFAULT_COUNT;
        int argIndex = 0;
        if (cmdArgs.length > 1 && cmdArgs[0].equals("-n")) {
            try {
                count = Integer.parseInt(cmdArgs[1]);
                if (count < 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                System.err.println("Usage: tail [-n <count>] [<file>...]");
                return;
            }
            argIndex += 2;
        }
        if (argIndex == cmdArgs.length) {
            // No files given, read from standard input.
            // Keep only the last count lines; readLineRaw keeps each line's own "\n".
            java.util.LinkedList<String> queue = new java.util.LinkedList<>();
            String line;
            while ((line = readLineRaw(System.in)) != null) {
                queue.add(line);
                if (queue.size() > count) {
                    queue.removeFirst();
                }
            }
            for (String l : queue) {
                System.out.print(l);
            }
        } else {
            boolean multipleFiles = cmdArgs.length - argIndex > 1;
            boolean printedAFile = false;
            for (int i = argIndex; i < cmdArgs.length; i++) {
                java.nio.file.Path filePath = java.nio.file.Path.of(cmdArgs[i]);
                java.util.LinkedList<String> queue = new java.util.LinkedList<>();
                try (java.io.InputStream input = getFileInput(filePath)) {
                    String line;
                    while ((line = readLineRaw(input)) != null) {
                        queue.add(line);
                        if (queue.size() > count) {
                            queue.removeFirst();
                        }
                    }
                } catch (IllegalArgumentException e) {
                    // "tail: <file>: No such file or directory" / "Is a directory"
                    System.err.println(e.getMessage());
                    continue;
                }
                if (multipleFiles) {
                    // blank line between files, but not before the first one printed
                    if (printedAFile) {
                        System.out.println();
                    }
                    System.out.println("==> " + filePath + " <==");
                }
                for (String l : queue) {
                    System.out.print(l);
                }
                printedAFile = true;
            }
        }
    }
}


