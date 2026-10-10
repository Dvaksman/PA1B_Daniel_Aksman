import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static com.github.stefanbirkner.systemlambda.SystemLambda.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class LsTest extends ShellCommandTest {

    @Test
    public void testLsOneFile() throws Exception {
        String filename = Paths.get("test-resources/file-1.txt").toString();
        String[] args = {"ls", filename};
        Ls cmd = new Ls(args);
        String output = tapSystemOut(cmd::runCommand);

        assertEquals(String.format("%s%n", filename), output);
    }

    @Test
    public void testLsOneDir() throws Exception {
        String dir = Paths.get("test-resources/dir-1").toString();
        String[] args = {"ls", dir};
        Ls cmd = new Ls(args);
        String output = tapSystemOut(cmd::runCommand);

        assertEquals(String.format("dir-1-1%nfile-1-1.txt%n"), output);
    }

    @Test
    public void testLsCurrentDir() throws Exception {
        String expected = String.format("pom.xml%nsrc%ntarget%ntest-resources%n");

        String[] args1 = {"ls"};
        Ls cmd1 = new Ls(args1);
        String output1 = tapSystemOut(cmd1::runCommand);
        assertEquals(expected, output1);

        String[] args2 = {"ls", "."};
        Ls cmd2 = new Ls(args2);
        String output2 = tapSystemOut(cmd2::runCommand);
        assertEquals(expected, output2);
    }

    @Test
    public void testLsTwoDirs() throws Exception {
        String dir1 = Paths.get("test-resources/dir-1").toString();
        String dir2 = Paths.get("test-resources/dir-2").toString();
        String[] args = {"ls", dir2, dir1};
        Ls cmd = new Ls(args);
        String output = tapSystemOut(cmd::runCommand);

        String expected = String.format("%s:%ndir-1-1%nfile-1-1.txt%n%n%s:%nfile-2-1.txt%n", dir1, dir2);

        assertEquals(expected, output);
    }

    @Test
    public void testLsFilesAndDirs() throws Exception {
        String dir1 = Paths.get("test-resources/dir-1").toString();
        String dir2 = Paths.get("test-resources/dir-2").toString();
        String file1 = Paths.get("test-resources/file-1.txt").toString();
        String file2 = Paths.get("test-resources/a.txt").toString();
        String[] args = {"ls", dir2, file1, dir1, file2};
        Ls cmd = new Ls(args);
        String output = tapSystemOut(cmd::runCommand);

        String expected = String.format("%s%n%s%n%n%s:%ndir-1-1%nfile-1-1.txt%n%n%s:%nfile-2-1.txt%n", file2, file1, dir1, dir2);

        assertEquals(expected, output);
    }

    @Test
    public void testLsInvalidFiles() throws Exception {
        String file1 = Paths.get("test-resources/file-1.txt").toString();
        String file2 = Paths.get("test-resources/file-2.txt").toString();
        String invalid1 = Paths.get("test-resources/file-invalid.txt").toString();
        String invalid2 = Paths.get("test-resources/does-not-exist").toString();

        String[] args = {"ls", file1, invalid1, file2, invalid2};
        Ls cmd = new Ls(args);

        String errorString1 = String.format("ls: %s: No such file or directory%n", invalid1);
        String errorString2 = String.format("ls: %s: No such file or directory%n", invalid2);
        String expected = String.format("%s%s%s%n%s%n", errorString1, errorString2, file1, file2);

        String errOutput = tapSystemErr(cmd::runCommand);
        assertEquals(errorString1 + errorString2, errOutput);
        String output = tapSystemErrAndOut(cmd::runCommand);
        assertEquals(expected, output);
    }
}
