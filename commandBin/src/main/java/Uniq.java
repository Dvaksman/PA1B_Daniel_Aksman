import java.io.IOException;
/**
 * {@code uniq [-c] [<file>]}: collapses runs of <b>adjacent</b> duplicate lines into a single
 * line — non-adjacent duplicates are left alone, same as real {@code uniq} (pair with
 * {@code sort} first to dedupe an entire file). With {@code -c}, each output line is prefixed
 * with the number of times it occurred in its run.
 *
 * <p>Unlike the other commands, {@code uniq} takes at most <b>one</b> file argument — that's a
 * real constraint of the actual Unix command, not a simplification for this project.
 */
public class Uniq extends ShellCommand {

    public Uniq(String[] args) {
        super(args);
    }

    public static void main(String[] args) {
        ShellCommand.start(Uniq.class, args);
    }

    @Override
    protected void runCommand() throws IOException {
        // Parse the arguments. If cmdArgs[0] is "-c", set countFlag to true and increment argIndex.
        boolean countFlag = false;
        int argIndex = 0;
        if (cmdArgs.length > 0 && cmdArgs[0].equals("-c")) {
            countFlag = true;
            argIndex++;
        }
        // uniq takes at most one file
        if (cmdArgs.length - argIndex > 1) {
            System.err.println("Usage: uniq [-c] [<file>]");
            return;
        }

        // Determine the input source: either the file or standard input
        java.io.InputStream input;
        if (cmdArgs.length == argIndex) {
            input = System.in;
        } else {
            try {
                input = getFileInput(java.nio.file.Path.of(cmdArgs[argIndex]));
            } catch (IllegalArgumentException e) {
                // "uniq: <file>: No such file or directory" / "uniq: <file>: Is a directory"
                System.err.println(e.getMessage());
                return;
            }
        }

        String previous = null;
        boolean start = false;
        int repeat = 1;
        String line;
        while ((line = readLineRaw(input)) != null) {
            boolean dupe = line.equals(previous);

            // check if it's not the first line
            if (start) {
                if (dupe) {
                    repeat++;
                } else {
                    // the run of "previous" just ended, so print it
                    if (countFlag) {
                        System.out.print(repeat + " " + previous);
                    } else {
                        System.out.print(previous);
                    }
                    repeat = 1;
                }
            }
            previous = line;
            start = true;
        }

        // print the last run (skip if the input was empty)
        if (start) {
            if (countFlag) {
                System.out.print(repeat + " " + previous);
            } else {
                System.out.print(previous);
            }
        }
    }
}
