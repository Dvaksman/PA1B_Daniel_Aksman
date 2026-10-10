import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static com.github.stefanbirkner.systemlambda.SystemLambda.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WcTest extends ShellCommandTest {
    private static final String formatSpecifier = "%8d %8d %8d %s%n";

    @Test
    public void testWcOneFile() throws Exception {
        String filename = Paths.get("test-resources/file-1.txt").toString();
        String[] args = {"wc", filename};
        Wc cmd = new Wc(args);
        String output = tapSystemOut(cmd::runCommand);

        String expected = String.format(formatSpecifier, 6, 5, 23, filename);

        assertEquals(expected, output);
    }

    @Test
    public void testWcTwoFiles() throws Exception {
        String file1 = Paths.get("test-resources/file-1.txt").toString();
        String file2 = Paths.get("test-resources/file-2.txt").toString();
        String[] args = {"wc", file2, file1};
        Wc cmd = new Wc(args);
        String output = tapSystemOut(cmd::runCommand);

        String expected1 = String.format(formatSpecifier, 6, 5, 23, file1);
        String expected2 = String.format(formatSpecifier, 2, 12, 59, file2);
        String expectedTotal = String.format(formatSpecifier, 8, 17, 82, "total");
        String expected = expected2 + expected1 + expectedTotal;

        assertEquals(expected, output);
    }

    @Test
    public void testWcKeyboardInput() throws Exception {
        String[] args = {"wc"};
        Wc cmd = new Wc(args);

        String input = "Pretend I'm typing this manually.\nWc should count these lines.\n";
        String expected = String.format(formatSpecifier, 2, 10, 63, "");
        String output = simulateInput(cmd, input);

        assertEquals(expected, output);
    }

    @Test
    public void testWcInvalidFile() throws Exception {
        String invalidFile = Paths.get("test-resources/file-invalid.txt").toString();
        String[] args = {"wc", invalidFile};
        Wc cmd = new Wc(args);

        String errorString = String.format("wc: %s: No such file or directory%n", invalidFile);

        String output = tapSystemOut(cmd::runCommand);
        assertEquals("", output);

        String errOutput = tapSystemErr(cmd::runCommand);
        assertEquals(errorString, errOutput);
    }

    @Test
    public void testWcDirectory() throws Exception {
        String dir = Paths.get("test-resources/dir-1").toString();
        String[] args = {"wc", dir};
        Wc cmd = new Wc(args);

        String errorString = String.format("wc: %s: Is a directory%n", dir);
        String expectedDir = String.format(formatSpecifier, 0, 0, 0, dir);

        String errOutput = tapSystemErr(cmd::runCommand);
        assertEquals(errorString, errOutput);

        String output = tapSystemErrAndOut(cmd::runCommand);
        assertEquals(errorString + expectedDir, output);
    }

    @Test
    public void testWcWithErrors() throws Exception {
        String file1 = Paths.get("test-resources/file-1.txt").toString();
        String dir = Paths.get("test-resources/dir-1").toString();
        String invalidFile = Paths.get("test-resources/file-invalid.txt").toString();
        String file2 = Paths.get("test-resources/file-2.txt").toString();
        String[] args = {"wc", file1, dir, invalidFile, file2};
        Wc cmd = new Wc(args);

        String expected1 = String.format(formatSpecifier, 6, 5, 23, file1);
        String expectedDir = String.format(formatSpecifier, 0, 0, 0, dir);
        String expected2 = String.format(formatSpecifier, 2, 12, 59, file2);
        String expectedTotal = String.format(formatSpecifier, 8, 17, 82, "total");
        String errorString1 = String.format("wc: %s: Is a directory%n", dir);
        String errorString2 = String.format("wc: %s: No such file or directory%n", invalidFile);
        String expected = expected1 + errorString1 + expectedDir + errorString2 + expected2 + expectedTotal;

        String errOutput = tapSystemErr(cmd::runCommand);
        assertEquals(errorString1 + errorString2, errOutput);

        String output = tapSystemErrAndOut(cmd::runCommand);
        assertEquals(expected, output);
    }
}
