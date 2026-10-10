import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * {@code head [-n <count>] [<file>...]}: prints the first {@code count} lines (default {@code 10})
 * of each file.
 *
 * <p>With more than one file, each file's output is preceded by an {@code "==> <file> <=="}
 * header line, with a blank line separating consecutive files' output (no header for a single
 * file or standard input).
 */
public class Head extends ShellCommand {
    private static final int DEFAULT_COUNT = 10;

    public Head(String[] args) {
        super(args);
    }

    public static void main(String[] args) {
        ShellCommand.start(Head.class, args);
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
                System.err.println("Usage: head [-n <count>] [<file>...]");
                return;
            }
            argIndex += 2;
        }
        if (argIndex == cmdArgs.length) {
            // No files given, read from standard input.
            // Print each line as soon as it's read, up to count lines.
            int lineCount = 0;
            String line;
            while (lineCount < count && (line = readLineRaw(System.in)) != null) {
                System.out.print(line);
                lineCount++;
            }
        } else {
            // Read from each file given in the arguments.
            // Each file's output (or error) is printed right away, so everything
            // comes out in the same order the files were given.
            boolean multipleFiles = cmdArgs.length - argIndex > 1;
            boolean printedAFile = false;
            for (int i = argIndex; i < cmdArgs.length; i++) {
                Path filePath = Path.of(cmdArgs[i]);

                try (InputStream input = getFileInput(filePath)) {
                    if (multipleFiles) {
                        // blank line between files, but not before the first one printed
                        if (printedAFile) {
                            System.out.println();
                        }
                        System.out.println("==> " + filePath + " <==");
                    }
                    // print up to count lines; readLineRaw keeps each line's own "\n"
                    int lineCount = 0;
                    String line;
                    while (lineCount < count && (line = readLineRaw(input)) != null) {
                        System.out.print(line);
                        lineCount++;
                    }
                    printedAFile = true;
                } catch (IllegalArgumentException e) {
                    // "head: <file>: No such file or directory" / "Is a directory"
                    System.err.println(e.getMessage());
                }
            }
        }
    }
}
