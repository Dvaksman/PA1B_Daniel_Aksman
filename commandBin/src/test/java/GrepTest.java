import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static com.github.stefanbirkner.systemlambda.SystemLambda.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GrepTest extends ShellCommandTest {

    @Test
    public void testGrepOneFile() throws Exception {
        String filename = Paths.get("test-resources/file-1.txt").toString();
        String[] args = {"grep", "i", filename};
        Grep cmd = new Grep(args);
        String output = tapSystemOut(cmd::runCommand);

        String expected = String.format("This is%nfile.%n");

        assertEquals(expected, output);
    }

    @Test
    public void testGrepTwoFiles() throws Exception {
        String file1 = Paths.get("test-resources/file-1.txt").toString();
        String file2 = Paths.get("test-resources/file-2.txt").toString();
        String[] args = {"grep", "i", file1, file2};
        Grep cmd = new Grep(args);
        String output = tapSystemOut(cmd::runCommand);

        String expected = String.format("%s:This is%n%s:file.%n%s:Wow! Here's another test file!%n", file1, file1, file2);

        assertEquals(expected, output);
    }

    @Test
    public void testGrepKeyboardInput() throws Exception {
        String[] args = {"grep", "only"};
        Grep cmd = new Grep(args);

        String input = String.format("Pretend I'm typing this manually.%nGrep should only output this line.%n");
        String expected = String.format("Grep should only output this line.%n");
        String output = simulateInput(cmd, input);

        assertEquals(expected, output);
    }

    @Test
    public void testGrepNoPattern() throws Exception {
        String[] args = {"grep"};
        Grep cmd = new Grep(args);

        String errorString = String.format("Usage: grep <pattern> [<file>...]%n");
        String errOut = tapSystemErr(cmd::runCommand);

        assertEquals(errorString, errOut);

        String output = tapSystemOut(cmd::runCommand);
        assertEquals("", output);
    }

    @Test
    public void testGrepRegexPattern() throws Exception {
        String filename = Paths.get("test-resources/file-1.txt").toString();
        String[] args = {"grep", "^.e", filename};
        Grep cmd = new Grep(args);
        String output = tapSystemOut(cmd::runCommand);

        String expected = String.format("test%n");

        assertEquals(expected, output);
    }

    @Test
    public void testGrepInvalidFile() throws Exception {
        String filename = Paths.get("test-resources/file-invalid.txt").toString();
        String[] args = {"grep", "i", filename};
        Grep cmd = new Grep(args);

        String output = tapSystemOut(cmd::runCommand);
        assertEquals("", output);

        String errOut = tapSystemErr(cmd::runCommand);
        String errorString = String.format("grep: %s: No such file or directory%n", filename);
        assertEquals(errorString, errOut);
    }

    @Test
    public void testGrepDirectory() throws Exception {
        String dir = Paths.get("test-resources/dir-1").toString();
        String[] args = {"grep", "i", dir};
        Grep cmd = new Grep(args);

        String output = tapSystemOut(cmd::runCommand);
        assertEquals("", output);

        String errOut = tapSystemErr(cmd::runCommand);
        String errorString = String.format("grep: %s: Is a directory%n", dir);
        assertEquals(errorString, errOut);
    }

    @Test
    public void testGrepWithErrors() throws Exception {
        String file1 = Paths.get("test-resources/file-1.txt").toString();
        String dir = Paths.get("test-resources/dir-1").toString();
        String invalidFile = Paths.get("test-resources/file-invalid.txt").toString();
        String file2 = Paths.get("test-resources/file-2.txt").toString();
        String[] args = {"grep", "i", file1, dir, invalidFile, file2};
        Grep cmd = new Grep(args);

        String errorString1 = String.format("grep: %s: Is a directory%n", dir);
        String errorString2 = String.format("grep: %s: No such file or directory%n", invalidFile);
        String expected1 = String.format("%s:This is%n%s:file.%n", file1, file1);
        String expected2 = String.format("%s:Wow! Here's another test file!%n", file2);
        String expected = expected1 + errorString1 + errorString2 + expected2;

        String errOutput = tapSystemErr(cmd::runCommand);
        assertEquals(errorString1 + errorString2, errOutput);

        String output = tapSystemErrAndOut(cmd::runCommand);
        assertEquals(expected, output);
    }
}
