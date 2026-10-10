import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Arrays;


/**
 * Abstract class for shared logic of shell commands.
 * Must be extended to implement each command.
 *
 * <p><b>Given to you — do not modify.</b> Every command extends this class and implements
 * {@link #runCommand()}; {@link #readLineRaw}, {@link #checkExists}, {@link #checkIsNotDir}, and
 * {@link #getFileInput} are the shared building blocks you'll use to write it.
 */
public abstract class ShellCommand {
    protected final String cmdName;
    protected final String[] cmdArgs;

    /**
     * Creates a shell command and separates the command name (1st arg) from the rest of the args
     * @param args the arguments passed to the command
     */
    public ShellCommand(String[] args) {
        cmdName = args[0];
        cmdArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
    }

    /**
     * Creates an instance of the given shell command class with the given args and runs the command.
     *
     * <p>Uses <a href="https://docs.oracle.com/javase/tutorial/reflect/">reflection</a> to call the
     * constructor of a class only known at runtime — the same trick {@code Shell.executeCommand()}
     * uses on {@code main}, at a larger scale. The {@code (Object)} cast on {@code args} stops it
     * being spread as varargs.
     *
     * @param clazz the {@code ShellCommand} subclass to create
     * @param args the arguments for the command
     */
    protected static void start(Class<? extends ShellCommand> clazz, String[] args) {
        try {
            ShellCommand command = clazz.getDeclaredConstructor(String[].class).newInstance((Object) args);
            command.runCommand();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Runs the shell command.
     */
    protected abstract void runCommand() throws Exception;

    /**
     * Reads one line from the given stream, including any newline characters.
     *
     * <p>Reads byte-by-byte (rather than e.g. {@code BufferedReader.readLine()}) so callers get
     * the exact original bytes, newline included. Treats byte {@code 4} (ASCII EOT, i.e. Ctrl-D)
     * the same as {@code -1} (real EOF), since that's how a terminal signals "done typing" — and
     * how this project's tests simulate keyboard input (they append {@code ""}).
     *
     * @param stream the Input stream to read from
     * @return the raw bytes in the string
     */
    protected static String readLineRaw(InputStream stream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int b;

        while (true) {
            b = stream.read();
            // Immediate EOF/EOT check
            if (b == -1 || b == 4) {
                // If the file ended abruptly without a newline, return the accumulated text.
                // On the next call, size will be 0, and it will return null to break the main loop.
                if (buffer.size() == 0) {
                    return null;
                }
                break;
            }
            // Write the byte (including \r and \n to keep the raw string intact)
            buffer.write(b);
            // Break out on newline
            if (b == '\n') {
                break;
            }
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    /**
     * Checks if a file exists and throws an exception otherwise.
     * The error message of this exception should be used for printing errors.
     * @param file the file to check
     * @throws IllegalArgumentException if the file does not exist
     */
    protected void checkExists(Path file) throws IllegalArgumentException {
        if (!Files.exists(file)) {
            throw new IllegalArgumentException(String.format("%s: %s: No such file or directory", cmdName, file));
        }
    }

    /**
     * Checks if a file is a not directory and throws an exception otherwise.
     * The error message of this exception should be used for printing errors.
     * @param file the file to check
     * @throws IllegalArgumentException if the file is a directory
     */
    protected void checkIsNotDir(Path file) throws IllegalArgumentException {
        if (Files.isDirectory(file)) {
            throw new IllegalArgumentException(String.format("%s: %s: Is a directory", cmdName, file));
        }
    }

    /**
     * Checks if the file exists and is not a directory, and creates an {@code InputStream} to read that file.
     * @param file the file to create a stream for
     * @return the created stream
     * @throws IllegalArgumentException if the file does not exist or is a directory
     */
    protected FileInputStream getFileInput(Path file) throws IllegalArgumentException, FileNotFoundException {
        checkExists(file);
        checkIsNotDir(file);
        return new FileInputStream(file.toFile());
    }
}
