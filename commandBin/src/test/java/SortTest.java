import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static com.github.stefanbirkner.systemlambda.SystemLambda.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SortTest extends ShellCommandTest {

    @Test
    public void testSortOneFile() throws Exception {
        String filename = Paths.get("test-resources/unsorted.txt").toString();
        String[] args = {"sort", filename};
        Sort cmd = new Sort(args);
        String output = tapSystemOut(cmd::runCommand);

        String expected = String.format("apple%napple%napple%nbanana%nbanana%nbanana%ncherry%n");

        assertEquals(expected, output);
    }

    @Test
    public void testSortReverse() throws Exception {
        String filename = Paths.get("test-resources/unsorted.txt").toString();
        String[] args = {"sort", "-r", filename};
        Sort cmd = new Sort(args);
        String output = tapSystemOut(cmd::runCommand);

        String expected = String.format("cherry%nbanana%nbanana%nbanana%napple%napple%napple%n");

        assertEquals(expected, output);
    }

    @Test
    public void testSortTwoFilesMergedNotHeaded() throws Exception {
        String file1 = Paths.get("test-resources/unsorted.txt").toString();
        String file2 = Paths.get("test-resources/file-2.txt").toString();
        String[] args = {"sort", file1, file2};
        Sort cmd = new Sort(args);
        String output = tapSystemOut(cmd::runCommand);

        // Uppercase letters sort before lowercase ones (plain String ordering) - no per-file headers.
        // file-2.txt has no trailing newline, so its "I hope..." line runs straight into "Wow!..."
        // once sorting moves it out of last place - sort doesn't insert one, same as readLineRaw.
        String expected = String.format("I hope I pass all the tests.Wow! Here's another test file!%n")
                + String.format("apple%napple%napple%nbanana%nbanana%nbanana%ncherry%n");

        assertEquals(expected, output);
    }

    @Test
    public void testSortKeyboardInput() throws Exception {
        String[] args = {"sort"};
        Sort cmd = new Sort(args);

        String input = String.format("banana%napple%ncherry%n");
        String expected = String.format("apple%nbanana%ncherry%n");
        String output = simulateInput(cmd, input);

        assertEquals(expected, output);
    }

    @Test
    public void testSortInvalidFile() throws Exception {
        String filename = Paths.get("test-resources/file-invalid.txt").toString();
        String[] args = {"sort", filename};
        Sort cmd = new Sort(args);

        String output = tapSystemOut(cmd::runCommand);
        assertEquals("", output);

        String errOut = tapSystemErr(cmd::runCommand);
        String errorString = String.format("sort: %s: No such file or directory%n", filename);
        assertEquals(errorString, errOut);
    }

    @Test
    public void testSortDirectory() throws Exception {
        String dir = Paths.get("test-resources/dir-1").toString();
        String[] args = {"sort", dir};
        Sort cmd = new Sort(args);

        String output = tapSystemOut(cmd::runCommand);
        assertEquals("", output);

        String errOut = tapSystemErr(cmd::runCommand);
        String errorString = String.format("sort: %s: Is a directory%n", dir);
        assertEquals(errorString, errOut);
    }

    @Test
    public void testSortWithErrors() throws Exception {
        String file1 = Paths.get("test-resources/unsorted.txt").toString();
        String dir = Paths.get("test-resources/dir-1").toString();
        String invalidFile = Paths.get("test-resources/file-invalid.txt").toString();
        String[] args = {"sort", file1, dir, invalidFile};
        Sort cmd = new Sort(args);

        String errorString1 = String.format("sort: %s: Is a directory%n", dir);
        String errorString2 = String.format("sort: %s: No such file or directory%n", invalidFile);
        String expected = errorString1 + errorString2
                + String.format("apple%napple%napple%nbanana%nbanana%nbanana%ncherry%n");

        String errOutput = tapSystemErr(cmd::runCommand);
        assertEquals(errorString1 + errorString2, errOutput);

        String output = tapSystemErrAndOut(cmd::runCommand);
        assertEquals(expected, output);
    }
}
