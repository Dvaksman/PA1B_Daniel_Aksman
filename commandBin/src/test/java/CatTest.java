import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static com.github.stefanbirkner.systemlambda.SystemLambda.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CatTest extends ShellCommandTest {

    @Test
    public void testCatOneFile() throws Exception {
        String filename = "test-resources/file-1.txt";
        String[] args = {"cat", filename};
        Cat cmd = new Cat(args);
        String output = tapSystemOut(cmd::runCommand);

        String expected = getFileContent(filename);

        assertEquals(expected, output);
    }

    @Test
    public void testCatTwoFiles() throws Exception {
        String file1 = "test-resources/file-1.txt";
        String file2 = "test-resources/file-2.txt";
        String[] args = {"cat", file2, file1};
        Cat cmd = new Cat(args);
        String output = tapSystemOut(cmd::runCommand);

        String file1Content = getFileContent(file1);
        String file2Content = getFileContent(file2);
        String expected = String.format("%s%s", file2Content, file1Content);

        assertEquals(expected, output);
    }

    @Test
    public void testCatKeyboardInput() throws Exception {
        String[] args = {"cat"};
        Cat cmd = new Cat(args);

        String input = String.format("Pretend I'm typing this manually.%nCat should output these lines.%n");
        String output = simulateInput(cmd, input);

        assertEquals(input, output);
    }

    @Test
    public void testCatInvalidFile() throws Exception {
        String invalidFile = Paths.get("test-resources/file-invalid.txt").toString();
        String[] args = {"cat", invalidFile};
        Cat cmd = new Cat(args);

        String errorString = String.format("cat: %s: No such file or directory%n", invalidFile);

        String output = tapSystemOut(cmd::runCommand);
        assertEquals("", output);
        String errOutput = tapSystemErr(cmd::runCommand);
        assertEquals(errorString, errOutput);
    }

    @Test
    public void testCatDirectory() throws Exception {
        String dir = Paths.get("test-resources/dir-1").toString();
        String[] args = {"cat", dir};
        Cat cmd = new Cat(args);

        String errorString = String.format("cat: %s: Is a directory%n", dir);

        String output = tapSystemOut(cmd::runCommand);
        assertEquals("", output);
        String errOutput = tapSystemErr(cmd::runCommand);
        assertEquals(errorString, errOutput);
    }

    @Test
    public void testCatWithErrors() throws Exception {
        String file1 = "test-resources/file-1.txt";
        String dir = Paths.get("test-resources/dir-1").toString();
        String invalidFile = Paths.get("test-resources/file-invalid.txt").toString();
        String file2 = "test-resources/file-2.txt";

        String[] args = {"cat", file1, dir, invalidFile, file2};
        Cat cmd = new Cat(args);

        String file1Content = getFileContent(file1);
        String file2Content = getFileContent(file2);
        String errorString1 = String.format("cat: %s: Is a directory%n", dir);
        String errorString2= String.format("cat: %s: No such file or directory%n", invalidFile);
        String expected = file1Content + errorString1 + errorString2 + file2Content;

        String errOutput = tapSystemErr(cmd::runCommand);
        assertEquals(errorString1 + errorString2, errOutput);
        String output = tapSystemErrAndOut(cmd::runCommand);
        assertEquals(expected, output);
    }
}
