import java.io.IOException;
/**
 * {@code cat}: prints the contents of one or more files to standard out, in order.
 *
 * <p>On a missing file or directory, print the error and keep going with the rest —
 * {@link #getFileInput} already throws a ready-to-print {@link IllegalArgumentException}.
 */
public class Cat extends ShellCommand {

    public Cat(String[] args) {
        super(args);
    }

    public static void main(String[] args) throws Exception {
        ShellCommand.start(Cat.class, args);
    }

    @Override
    protected void runCommand() throws IOException {
        // if there are more than 0 arguements, print the contents of each file in order
        // if there are no arguements, print the contents of standard input
        if (cmdArgs.length == 0) {
            // no arguments, use standard input
            String line;
            while ((line = readLineRaw(System.in)) != null) {
                System.out.print(line);
            }
        } else {
            // print the contents of each file in order
            for (String filename : cmdArgs) {
                try {
                    java.nio.file.Path file = java.nio.file.Paths.get(filename);
                    try (java.io.InputStream input = getFileInput(file)) {
                        input.transferTo(System.out);
                    }
                } catch (IllegalArgumentException e) {
                    System.err.println(e.getMessage());
                }
            }
        }
    }
}
