import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static com.github.stefanbirkner.systemlambda.SystemLambda.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TailTest extends ShellCommandTest {

    @Test
    public void testTailDefaultCountShorterFile() throws Exception {
        String filename = Paths.get("test-resources/file-1.txt").toString();
        String[] args = {"tail", filename};
        Tail cmd = new Tail(args);
        String output = tapSystemOut(cmd::runCommand);

        String expected = String.format("This is%na%n%ntest%nfile.%n%n");

        assertEquals(expected, output);
    }

    @Test
    public void testTailWithCount() throws Exception {
        String filename = Paths.get("test-resources/file-1.txt").toString();
        String[] args = {"tail", "-n", "2", filename};
        Tail cmd = new Tail(args);
        String output = tapSystemOut(cmd::runCommand);

        String expected = String.format("file.%n%n");

        assertEquals(expected, output);
    }

    @Test
    public void testTailTwoFiles() throws Exception {
        // Neither file-2.txt nor file-3.txt ends in a trailing newline, so each one's last line
        // prints without one - the separator blank line between files supplies the only "%n".
        String file2 = Paths.get("test-resources/file-2.txt").toString();
        String file3 = Paths.get("test-resources/file-3.txt").toString();
        String[] args = {"tail", "-n", "1", file2, file3};
        Tail cmd = new Tail(args);
        String output = tapSystemOut(cmd::runCommand);

        String expected = String.format("==> %s <==%nI hope I pass all the tests.%n==> %s <==%n", file2, file3)
                + "Don't you have enough of these by now?";

        assertEquals(expected, output);
    }

    @Test
    public void testTailKeyboardInput() throws Exception {
        String[] args = {"tail", "-n", "1"};
        Tail cmd = new Tail(args);

        String input = String.format("Tail should not print this line.%nPretend I'm typing this manually.%n");
        String expected = String.format("Pretend I'm typing this manually.%n");
        String output = simulateInput(cmd, input);

        assertEquals(expected, output);
    }

    @Test
    public void testTailInvalidCount() throws Exception {
        String filename = Paths.get("test-resources/file-1.txt").toString();
        String[] args = {"tail", "-n", "not-a-number", filename};
        Tail cmd = new Tail(args);

        String errorString = String.format("Usage: tail [-n <count>] [<file>...]%n");
        String errOut = tapSystemErr(cmd::runCommand);

        assertEquals(errorString, errOut);

        String output = tapSystemOut(cmd::runCommand);
        assertEquals("", output);
    }

    @Test
    public void testTailInvalidFile() throws Exception {
        String filename = Paths.get("test-resources/file-invalid.txt").toString();
        String[] args = {"tail", filename};
        Tail cmd = new Tail(args);

        String output = tapSystemOut(cmd::runCommand);
        assertEquals("", output);

        String errOut = tapSystemErr(cmd::runCommand);
        String errorString = String.format("tail: %s: No such file or directory%n", filename);
        assertEquals(errorString, errOut);
    }

    @Test
    public void testTailDirectory() throws Exception {
        String dir = Paths.get("test-resources/dir-1").toString();
        String[] args = {"tail", dir};
        Tail cmd = new Tail(args);

        String output = tapSystemOut(cmd::runCommand);
        assertEquals("", output);

        String errOut = tapSystemErr(cmd::runCommand);
        String errorString = String.format("tail: %s: Is a directory%n", dir);
        assertEquals(errorString, errOut);
    }

    @Test
    public void testTailWithErrors() throws Exception {
        String file1 = Paths.get("test-resources/file-1.txt").toString();
        String dir = Paths.get("test-resources/dir-1").toString();
        String invalidFile = Paths.get("test-resources/file-invalid.txt").toString();
        String file2 = Paths.get("test-resources/file-2.txt").toString();
        String[] args = {"tail", "-n", "1", file1, dir, invalidFile, file2};
        Tail cmd = new Tail(args);

        String errorString1 = String.format("tail: %s: Is a directory%n", dir);
        String errorString2 = String.format("tail: %s: No such file or directory%n", invalidFile);
        String expected = String.format("==> %s <==%n%n", file1)
                + errorString1 + errorString2
                + String.format("%n==> %s <==%n", file2)
                + "I hope I pass all the tests.";

        String errOutput = tapSystemErr(cmd::runCommand);
        assertEquals(errorString1 + errorString2, errOutput);

        String output = tapSystemErrAndOut(cmd::runCommand);
        assertEquals(expected, output);
    }
}
