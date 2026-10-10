import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


/**
 * {@code sort [-r] [<file>...]}: reads every line from all given files (or standard input, if
 * none given), concatenated in argument order, and prints them sorted lexicographically —
 * ascending by default, descending with {@code -r}.
 *
 * <p>Unlike {@code head}/{@code tail}, files are merged into one sorted stream rather than
 * processed (and headed) independently — that's how real {@code sort} behaves too.
 */
public class Sort extends ShellCommand {

    public Sort(String[] args) {
        super(args);
    }

    public static void main(String[] args) {
        ShellCommand.start(Sort.class, args);
    }

    @Override
    protected void runCommand() throws IOException {
        boolean reverse = false;
        int argIndex = 0;
        if (cmdArgs.length > 0 && cmdArgs[0].equals("-r")) {
            reverse = true;
            argIndex++;
        }

        List<String> lines = new ArrayList<>();
        if (argIndex == cmdArgs.length) {
            // No files given, read from standard input.
            // Loop until readLineRaw returns null (end of input / Ctrl-D).
            String line;
            while ((line = readLineRaw(System.in)) != null) {
                lines.add(line);
            }
        } else {
            // Read from each file given in the arguments.
            // readLineRaw keeps each line's own "\n" (or lack of one on the last line),
            // so a file with no trailing newline runs into the next line when printed.
            for (int i = argIndex; i < cmdArgs.length; i++) {
                Path filePath = Paths.get(cmdArgs[i]);
                try (InputStream input = getFileInput(filePath)) {
                    String line;
                    while ((line = readLineRaw(input)) != null) {
                        lines.add(line);
                    }
                } catch (IllegalArgumentException e) {
                    // "sort: <file>: No such file or directory" / "Is a directory"
                    System.err.println(e.getMessage());
                }
            }
        }

        // Sort the lines
        if (reverse) {
            lines.sort(java.util.Collections.reverseOrder());
        } else {
            lines.sort(String::compareTo);
        }

        // Print the sorted lines exactly as read (each keeps its own line ending)
        for (String line : lines) {
            System.out.print(line);
        }
    }
}
