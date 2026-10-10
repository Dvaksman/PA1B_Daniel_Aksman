import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static com.github.stefanbirkner.systemlambda.SystemLambda.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HeadTest extends ShellCommandTest {

    @Test
    public void testHeadDefaultCountShorterFile() throws Exception {
        String filename = Paths.get("test-resources/file-1.txt").toString();
        String[] args = {"head", filename};
        Head cmd = new Head(args);
        String output = tapSystemOut(cmd::runCommand);

        String expected = String.format("This is%na%n%ntest%nfile.%n%n");

        assertEquals(expected, output);
    }

    @Test
    public void testHeadWithCount() throws Exception {
        String filename = Paths.get("test-resources/file-1.txt").toString();
        String[] args = {"head", "-n", "2", filename};
        Head cmd = new Head(args);
        String output = tapSystemOut(cmd::runCommand);

        String expected = String.format("This is%na%n");

        assertEquals(expected, output);
    }

    @Test
    public void testHeadTwoFiles() throws Exception {
        String file1 = Paths.get("test-resources/file-1.txt").toString();
        String file2 = Paths.get("test-resources/file-2.txt").toString();
        String[] args = {"head", "-n", "1", file1, file2};
        Head cmd = new Head(args);
        String output = tapSystemOut(cmd::runCommand);

        String expected = String.format("==> %s <==%nThis is%n%n==> %s <==%nWow! Here's another test file!%n", file1, file2);

        assertEquals(expected, output);
    }

    @Test
    public void testHeadKeyboardInput() throws Exception {
        String[] args = {"head", "-n", "1"};
        Head cmd = new Head(args);

        String input = String.format("Pretend I'm typing this manually.%nHead should not print this line.%n");
        String expected = String.format("Pretend I'm typing this manually.%n");
        String output = simulateInput(cmd, input);

        assertEquals(expected, output);
    }

    @Test
    public void testHeadInvalidCount() throws Exception {
        String filename = Paths.get("test-resources/file-1.txt").toString();
        String[] args = {"head", "-n", "not-a-number", filename};
        Head cmd = new Head(args);

        String errorString = String.format("Usage: head [-n <count>] [<file>...]%n");
        String errOut = tapSystemErr(cmd::runCommand);

        assertEquals(errorString, errOut);

        String output = tapSystemOut(cmd::runCommand);
        assertEquals("", output);
    }

    @Test
    public void testHeadInvalidFile() throws Exception {
        String filename = Paths.get("test-resources/file-invalid.txt").toString();
        String[] args = {"head", filename};
        Head cmd = new Head(args);

        String output = tapSystemOut(cmd::runCommand);
        assertEquals("", output);

        String errOut = tapSystemErr(cmd::runCommand);
        String errorString = String.format("head: %s: No such file or directory%n", filename);
        assertEquals(errorString, errOut);
    }

    @Test
    public void testHeadDirectory() throws Exception {
        String dir = Paths.get("test-resources/dir-1").toString();
        String[] args = {"head", dir};
        Head cmd = new Head(args);

        String output = tapSystemOut(cmd::runCommand);
        assertEquals("", output);

        String errOut = tapSystemErr(cmd::runCommand);
        String errorString = String.format("head: %s: Is a directory%n", dir);
        assertEquals(errorString, errOut);
    }

    @Test
    public void testHeadWithErrors() throws Exception {
        String file1 = Paths.get("test-resources/file-1.txt").toString();
        String dir = Paths.get("test-resources/dir-1").toString();
        String invalidFile = Paths.get("test-resources/file-invalid.txt").toString();
        String file2 = Paths.get("test-resources/file-2.txt").toString();
        String[] args = {"head", "-n", "1", file1, dir, invalidFile, file2};
        Head cmd = new Head(args);

        String errorString1 = String.format("head: %s: Is a directory%n", dir);
        String errorString2 = String.format("head: %s: No such file or directory%n", invalidFile);
        String expected = String.format("==> %s <==%nThis is%n", file1)
                + errorString1 + errorString2
                + String.format("%n==> %s <==%nWow! Here's another test file!%n", file2);

        String errOutput = tapSystemErr(cmd::runCommand);
        assertEquals(errorString1 + errorString2, errOutput);

        String output = tapSystemErrAndOut(cmd::runCommand);
        assertEquals(expected, output);
    }
}
