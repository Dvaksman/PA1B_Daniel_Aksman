import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErr;
import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;

public class UniqTest extends ShellCommandTest {

    @Test
    public void testUniqCollapsesAdjacentDuplicatesOnly() throws Exception {
        String filename = Paths.get("test-resources/unsorted.txt").toString();
        String[] args = {"uniq", filename};
        Uniq cmd = new Uniq(args);
        String output = tapSystemOut(cmd::runCommand);

        // banana, apple, apple, cherry, apple, banana, banana
        // -> only the *adjacent* apple pair and banana pair collapse; the lone middle apple stays.
        String expected = String.format("banana%napple%ncherry%napple%nbanana%n");

        assertEquals(expected, output);
    }

    @Test
    public void testUniqWithCount() throws Exception {
        String filename = Paths.get("test-resources/unsorted.txt").toString();
        String[] args = {"uniq", "-c", filename};
        Uniq cmd = new Uniq(args);
        String output = tapSystemOut(cmd::runCommand);

        String expected = String.format("1 banana%n2 apple%n1 cherry%n1 apple%n2 banana%n");

        assertEquals(expected, output);
    }

    @Test
    public void testUniqNoAdjacentDuplicates() throws Exception {
        String filename = Paths.get("test-resources/file-1.txt").toString();
        String[] args = {"uniq", filename};
        Uniq cmd = new Uniq(args);
        String output = tapSystemOut(cmd::runCommand);

        // No two adjacent lines are identical, so the file is reproduced unchanged.
        String expected = String.format("This is%na%n%ntest%nfile.%n%n");

        assertEquals(expected, output);
    }

    @Test
    public void testUniqKeyboardInput() throws Exception {
        String[] args = {"uniq"};
        Uniq cmd = new Uniq(args);

        String input = String.format("a%na%nb%n");
        String expected = String.format("a%nb%n");
        String output = simulateInput(cmd, input);

        assertEquals(expected, output);
    }

    @Test
    public void testUniqTooManyFiles() throws Exception {
        String file1 = Paths.get("test-resources/file-1.txt").toString();
        String file2 = Paths.get("test-resources/file-2.txt").toString();
        String[] args = {"uniq", file1, file2};
        Uniq cmd = new Uniq(args);

        String errorString = String.format("Usage: uniq [-c] [<file>]%n");
        String errOut = tapSystemErr(cmd::runCommand);

        assertEquals(errorString, errOut);

        String output = tapSystemOut(cmd::runCommand);
        assertEquals("", output);
    }

    @Test
    public void testUniqInvalidFile() throws Exception {
        String filename = Paths.get("test-resources/file-invalid.txt").toString();
        String[] args = {"uniq", filename};
        Uniq cmd = new Uniq(args);

        String output = tapSystemOut(cmd::runCommand);
        assertEquals("", output);

        String errOut = tapSystemErr(cmd::runCommand);
        String errorString = String.format("uniq: %s: No such file or directory%n", filename);
        assertEquals(errorString, errOut);
    }

    @Test
    public void testUniqDirectory() throws Exception {
        String dir = Paths.get("test-resources/dir-1").toString();
        String[] args = {"uniq", dir};
        Uniq cmd = new Uniq(args);

        String output = tapSystemOut(cmd::runCommand);
        assertEquals("", output);

        String errOut = tapSystemErr(cmd::runCommand);
        String errorString = String.format("uniq: %s: Is a directory%n", dir);
        assertEquals(errorString, errOut);
    }
}
