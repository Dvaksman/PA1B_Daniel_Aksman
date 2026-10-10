import com.github.stefanbirkner.systemlambda.SystemLambda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@Timeout(value = 3, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
public class ShellTest {
    private final Path cwd = Paths.get(System.getProperty("user.dir"));
    private Shell shell;

    @BeforeEach
    public void setQuietOutput() {
        // Create new Shell
        shell = new Shell();
        // Create in-memory buffers to catch the output quietly
        ByteArrayOutputStream outputBuffer1 = new ByteArrayOutputStream();
        PrintStream quietPrintStream1 = new PrintStream(outputBuffer1);
        System.setOut(quietPrintStream1);
        ByteArrayOutputStream outputBuffer2 = new ByteArrayOutputStream();
        PrintStream quietPrintStream2 = new PrintStream(outputBuffer2);
        System.setErr(quietPrintStream2);
    }


    private void interceptStream(InputStream stream, boolean error) throws IOException {
        PrintStream out = error ? System.err : System.out;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Push back to JVM System.out/err so System Lambda intercepts it
                out.println(line);
                out.flush();
            }
        }
    }

    private String interceptOut(InputStream stream) throws Exception {
        return SystemLambda.tapSystemOut(() -> interceptStream(stream, false));
    }

    private String interceptErr(InputStream stream) throws Exception {
        return SystemLambda.tapSystemErr(() -> interceptStream(stream, true));
    }


    @Nested
    class BasicTests {

        private String interceptShell(String[] commands, boolean out, boolean error) throws Exception {
            // Create a partial mock (spy) of the actual shell class
            Shell shellSpy = Mockito.spy(new Shell());

            // Intercept executeProcess to catch its output
            doAnswer(invocation -> {
                Shell.Pipeline pipeline = shellSpy.new Pipeline(invocation.getArgument(0));
                List<ProcessBuilder> pbs = pipeline.getPbs();
                List<Integer> outIdx = new ArrayList<>();
                List<Integer> errIdx = new ArrayList<>();
                for (int i = 0; i < pbs.size(); i++) {
                    ProcessBuilder pb = pbs.get(i);
                    if (pb.redirectOutput() == ProcessBuilder.Redirect.INHERIT) {
                        outIdx.add(i);
                        pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
                    }
                    if (pb.redirectError() == ProcessBuilder.Redirect.INHERIT) {
                        errIdx.add(i);
                        pb.redirectError(ProcessBuilder.Redirect.PIPE);
                    }
                }
                // Now execute the pipeline
                List<Process> processes = pipeline.executePipeline();
                for (int idx : outIdx) {
                    interceptStream(processes.get(idx).getInputStream(), false);
                }
                for (int idx : errIdx) {
                    interceptStream(processes.get(idx).getErrorStream(), true);
                }
                return pipeline;
            }).when(shellSpy).processInput(any(String.class));


            InputStream originalIn = System.in;
            String simulatedInput = String.format("%s%n", String.join(System.lineSeparator(), commands));
            try {
                ByteArrayInputStream testInput = new ByteArrayInputStream(simulatedInput.getBytes());
                System.setIn(testInput);
                String output;
                if (out && error) {
                    output = SystemLambda.tapSystemErrAndOut(shellSpy::runRepl);
                } else if (error) {
                    output = SystemLambda.tapSystemErr(shellSpy::runRepl);
                } else {
                    output = SystemLambda.tapSystemOut(shellSpy::runRepl);
                }
                return output;
            } finally {
                System.setIn(originalIn);
            }
        }

        public String tapShellErrAndOut(String[] commands) throws Exception {
            return interceptShell(commands, true, true);
        }

        public String tapShellErr(String[] commands) throws Exception {
            return interceptShell(commands, false,true);
        }

        public String tapShellOut(String[] commands) throws Exception {
            return interceptShell(commands, true, false);
        }

        @Test
        public void testGetPath() {
            Path cwd = Paths.get(System.getProperty("user.dir"));
            Path expected = cwd.resolve("../commandBin/target/classes").toAbsolutePath().normalize();
            Path actual = Shell.getPath();
            assertTrue(actual.isAbsolute());
            assertEquals(expected, actual);
        }

        @Test
        public void testGetPathFromDifferentCwd() {
            shell.setCwd(cwd.resolve("..").normalize());
            Path expected = cwd.resolve("../commandBin/target/classes").toAbsolutePath().normalize();
            assertEquals(expected, Shell.getPath());
        }

        @Test
        public void testExit() throws Exception {
            String[] input = {"exit"};
            // Test that shell exits and doesn't stay in loop
            String output = tapShellOut(input);
            assertEquals(Shell.COMMAND_PROMPT, output);
        }

        @Test
        public void testCommands() throws Exception {
            String[] input = {"ls", "wc ../test-resources/file-1.txt", "exit"};
            String expectedLs = String.format("pom.xml%nsrc%ntarget%ntest-resources%n");
            String expectedWc = String.format("%8d %8d %8d ../test-resources/file-1.txt%n", 6, 5, 23);
            String expected = Shell.COMMAND_PROMPT + expectedLs + Shell.COMMAND_PROMPT + expectedWc + Shell.COMMAND_PROMPT;
            String output = tapShellErrAndOut(input);
            assertEquals(expected, output);
        }

        @Test
        public void testInvalidCommand() throws Exception {
            String[] input = {"x", "exit"};
            String output = tapShellErr(input);
            assertEquals(String.format("x: command not found%n"), output);
            output = tapShellOut(input);
            assertEquals("> > ", output);
        }

        @Test
        public void testCommandAfterError() throws Exception {
            String[] input = {"x", "ls", "exit"};
            String expectedErr = String.format("x: command not found%n");
            String expectedLs = String.format("pom.xml%nsrc%ntarget%ntest-resources%n");
            String expected = Shell.COMMAND_PROMPT + expectedErr + Shell.COMMAND_PROMPT + expectedLs + Shell.COMMAND_PROMPT;

            String err = tapShellErr(input);
            assertEquals(expectedErr, err);

            String errAndOut = tapShellErrAndOut(input);
            assertEquals(expected, errAndOut);
        }
    }

    @Nested
    class ForkTests {

        @Test
        public void testForkedShell() {
            ProcessBuilder pb = shell.buildForkedShell("ls test-resources");
            assertSame(ProcessBuilder.Redirect.INHERIT, pb.redirectError());
            assertSame(ProcessBuilder.Redirect.PIPE, pb.redirectInput());
            assertSame(ProcessBuilder.Redirect.PIPE, pb.redirectOutput());
            List<String> cmd = pb.command();
            assertSame(5, cmd.size());
            assertEquals("java", cmd.getFirst());
            assertEquals("Shell", cmd.get(cmd.size() - 2));
            assertEquals("ls test-resources", cmd.getLast());
        }

        @Test
        public void testForkedShellCwd() {
            Path newCwd = cwd.resolve("test-resources");
            shell.setCwd(newCwd);
            ProcessBuilder pb = shell.buildForkedShell("ls test-resources");
            assertEquals(newCwd, pb.directory().toPath());
        }
    }

    @Nested
    class DirectoryTests {

        @Test
        public void testWorkingDirectory() throws Exception {
            assertEquals(cwd, shell.getCwd());
            assertTrue(shell.getCwd().isAbsolute());
        }

        @Test
        public void testCdAbsolute() {
            Path target = cwd.getParent().getParent();
            shell.changeDirectory(target.toString());
            assertEquals(target, shell.getCwd());
            assertTrue(shell.getCwd().isAbsolute());
        }

        @Test
        public void testCdRelative() {
            shell.changeDirectory("test-resources");
            assertEquals(cwd.resolve("test-resources"), shell.getCwd());
            shell.changeDirectory("..");
            assertEquals(cwd, shell.getCwd());
            assertTrue(shell.getCwd().isAbsolute());
        }

        @Test
        public void testCdMultiRelative() {
            String relativePath = "test-resources/../test-resources/dir-1";
            Path target = cwd.resolve(relativePath).normalize();
            shell.changeDirectory(relativePath);
            assertEquals(target, shell.getCwd());
        }

        @Test
        public void testCdInvalidFile() throws Exception {
            String err = SystemLambda.tapSystemErr(() -> shell.processInput("cd invalid-dir"));
            assertEquals(String.format("cd: invalid-dir: No such file or directory%n"), err);
            assertEquals(cwd, shell.getCwd());
        }

        @Test
        public void testCommandCwdAfterCd() {
            shell.changeDirectory("test-resources");
            ProcessBuilder pb = shell.buildForkedShell("ls");
            assertEquals(cwd.resolve("test-resources"), pb.directory().toPath());
        }
    }

    @Nested
    class PipelineTests {

        @Test
        public void testPipeline() throws Exception {
            Shell.Pipeline pipeline = shell.new Pipeline("ls | grep \\.");
            pipeline.getPbs().getLast().redirectOutput(ProcessBuilder.Redirect.PIPE);
            pipeline.executePipeline();
            String output = interceptOut(pipeline.getProcesses().getLast().getInputStream());
            assertEquals(String.format("pom.xml%n"), output);
        }

        @Test
        public void testErrorInPipeline() throws Exception {
            Shell.Pipeline pipeline = shell.new Pipeline("ls | grep");
            pipeline.getPbs().get(1).redirectError(ProcessBuilder.Redirect.PIPE);
            pipeline.getPbs().getLast().redirectOutput(ProcessBuilder.Redirect.PIPE);
            pipeline.executePipeline();
            String err = interceptErr(pipeline.getProcesses().getLast().getErrorStream());
            assertEquals(String.format("Usage: grep <pattern> [<file>...]%n"), err);
            String output = interceptOut(pipeline.getProcesses().getLast().getInputStream());
            assertEquals("", output);
        }

        @Test
        public void testInvalidCommandInPipeline() throws Exception {
            Shell.Pipeline pipeline = shell.new Pipeline("ls | x | grep \\.");
            pipeline.getPbs().get(1).redirectError(ProcessBuilder.Redirect.PIPE);
            pipeline.getPbs().getLast().redirectOutput(ProcessBuilder.Redirect.PIPE);
            pipeline.executePipeline();
            String err = interceptErr(pipeline.getProcesses().get(1).getErrorStream());
            assertEquals(String.format("x: command not found%n"), err);
            String output = interceptOut(pipeline.getProcesses().getLast().getInputStream());
            assertEquals("", output);
        }
    }

    @Nested
    class JobTests {
        private static final String formatSpecifier = "%-10s%-20s %s%n";

        @Test
        public void testForegroundJob() throws Exception {
            Shell.Pipeline pipeline = shell.new Pipeline("sleep 1");
            pipeline.executePipeline();
            assertFalse(pipeline.getProcesses().getFirst().isAlive());
        }

        @Test
        public void testBackgroundJob() throws Exception {
            Shell.Pipeline pipeline = shell.new Pipeline("sleep 1 &");
            pipeline.executePipeline();
            assertTrue(pipeline.getProcesses().getFirst().isAlive() && pipeline.isBackground());
        }

        @Test
        public void testCommandAfterBackgroundJob() throws Exception {
            Shell.Pipeline pipe1 = shell.processInput("sleep 1 &");
            Shell.Pipeline pipe2 = shell.processInput("sleep 0");
            TimeUnit.MILLISECONDS.sleep(10);
            assertFalse(pipe2.getProcesses().getLast().isAlive());
            assertTrue(pipe1.getProcesses().getLast().isAlive());
        }

        @Test
        public void testListJobs() throws Exception {
            String cmd1 = "sleep 0.01 &", cmd2 = "sleep 1 &", cmd3 = "sleep 0.1 &";
            Shell.Pipeline pipe1 = shell.processInput(cmd1);
            Shell.Pipeline pipe2 = shell.processInput(cmd2);

            pipe1.waitFor();
            String output = SystemLambda.tapSystemOut(() -> shell.processInput("jobs"));
            String expected1 = String.format(formatSpecifier, "[1]", "Done", cmd1);
            String expected2 = String.format(formatSpecifier, "[2]", "Running", cmd2);
            assertEquals(expected1 + expected2, output);

            Shell.Pipeline pipe3 = shell.processInput(cmd3);
            pipe2.waitFor();
            pipe3.waitFor();
            output = SystemLambda.tapSystemOut(() -> shell.processInput("jobs"));
            expected2 = String.format(formatSpecifier, "[2]", "Done", cmd2);
            String expected3 = String.format(formatSpecifier, "[3]", "Done", cmd3);
            assertEquals(expected2 + expected3, output);

            output = SystemLambda.tapSystemOut(() -> shell.processInput("jobs"));
            assertEquals("", output);

            pipe1 = shell.processInput(cmd1);
            pipe1.waitFor();
            output = SystemLambda.tapSystemOut(() -> shell.processInput("jobs"));
            assertEquals(expected1, output);
        }

        @Test
        public void testKillJob() throws Exception {
            Shell.Pipeline pipe1 = shell.processInput("sleep 10 &");
            Shell.Pipeline pipe2 = shell.processInput("kill 1");
            assertNull(pipe2);
            assertFalse(pipe1.getProcesses().getLast().isAlive());
            assertTrue(pipe1.isDone() && pipe1.isTerminated());
        }

        @Test
        public void testKill2Jobs() throws Exception {
            Shell.Pipeline pipe2 = shell.processInput("sleep 15 &");
            Shell.Pipeline pipe1 = shell.processInput("sleep 10 &");
            shell.processInput("kill 1 2");
            assertFalse(pipe1.getProcesses().getLast().isAlive());
            assertFalse(pipe2.getProcesses().getLast().isAlive());
            assertTrue(pipe1.isDone() && pipe1.isTerminated());
            assertTrue(pipe2.isDone() && pipe2.isTerminated());
        }

        @Test
        public void testKillInvalidArgs() throws Exception {
            shell.processInput("sleep 5 &");
            String err1 = SystemLambda.tapSystemErr(() -> shell.processInput("kill 2"));
            assertEquals(String.format("kill: 2: no such job%n"), err1);
            String err2 = SystemLambda.tapSystemErr(() -> shell.processInput("kill 0"));
            assertEquals(String.format("kill: arguments must be job IDs%n"), err2);
            err2 = SystemLambda.tapSystemErr(() -> shell.processInput("kill -1"));
            assertEquals(String.format("kill: arguments must be job IDs%n"), err2);
            err2 = SystemLambda.tapSystemErr(() -> shell.processInput("kill x"));
            assertEquals(String.format("kill: arguments must be job IDs%n"), err2);
        }

        @Test
        public void testKillInvalidThenValidArg() throws Exception {
            Shell.Pipeline pipe1 = shell.processInput("sleep 5 &");
            String err = SystemLambda.tapSystemErr(() -> shell.processInput("kill 999 1"));
            assertEquals(String.format("kill: 999: no such job%n"), err);
            assertFalse(pipe1.getProcesses().getLast().isAlive());
            assertTrue(pipe1.isDone() && pipe1.isTerminated());
        }

        @Test
        public void testListJobsAfterKill() throws Exception {
            String cmd1 = "sleep 0.01 &", cmd2 = "sleep 10 &", cmd3 = "sleep 0.05 &";
            Shell.Pipeline pipe1 = shell.processInput(cmd1);
            shell.processInput(cmd2);
            Shell.Pipeline pipe3 = shell.processInput(cmd3);
            shell.processInput("kill 2");

            pipe1.waitFor();
            pipe3.waitFor();

            String output = SystemLambda.tapSystemOut(() -> shell.processInput("jobs"));
            String expected = String.format(formatSpecifier, "[1]", "Done", cmd1)
                    + String.format(formatSpecifier, "[2]", "Terminated", cmd2)
                    + String.format(formatSpecifier, "[3]", "Done", cmd3);
            assertEquals(expected, output);

            output = SystemLambda.tapSystemOut(() -> shell.processInput("jobs"));
            assertEquals("", output);
        }
    }

    @Nested
    class RedirectTests {

        private String getFileContent(String filename) throws Exception {
            return Files.readString(Path.of(filename));
        }

        @Test
        public void testRedirectRelative() throws Exception {
            Path target = Paths.get("test-resources/test-1.txt");
            String output = SystemLambda.tapSystemErrAndOut(() -> shell.processInput(String.format("pwd > %s", target)));
            assertEquals("", output);
            assertEquals(String.format("%s%n", shell.getCwd()), getFileContent(target.toString()));
        }

        @Test
        public void testRedirectAbsolute() throws Exception {
            Path target = cwd.resolve("test-resources/test-2.txt");
            assertFalse(Files.exists(target));
            assertTrue(target.isAbsolute());
            String output = SystemLambda.tapSystemErrAndOut(() -> shell.processInput(String.format("pwd > %s", target)));
            assertEquals("", output);
            assertEquals(String.format("%s%n", shell.getCwd()), getFileContent(target.toString()));
        }

        @Test
        public void testRedirectAfterCd() throws Exception {
            Path target = Paths.get("create-after-cd");
            Path expectedDest = cwd.resolve("test-resources/dir-2").resolve(target);
            assertFalse(Files.exists(expectedDest));
            shell.processInput("cd test-resources/dir-2");
            assertEquals(expectedDest.getParent(), shell.getCwd());
            shell.processInput(String.format("pwd > %s", target));
            assertTrue(Files.exists(expectedDest));
            assertEquals(String.format("%s%n", shell.getCwd()), getFileContent(expectedDest.toString()));
        }

        @Test
        public void testRedirectInMultiCommandPipeline() throws Exception {
            Path target = Paths.get("test-resources/list");
            String cmd = "ls test-resources | grep file";
            String output = SystemLambda.tapSystemErrAndOut(() -> shell.processInput(String.format("%s > %s", cmd, target)));
            assertEquals("", output);
            String expected = String.format("file-1.txt%nfile-2.txt%nfile-3.txt%n");
            assertEquals(expected, getFileContent(target.toString()));
        }

        @Test
        public void testRedirectNoCommand() throws Exception {
            Path target = Paths.get("test-resources/new-file");
            assertFalse(Files.exists(target));
            String output = SystemLambda.tapSystemErrAndOut(() -> shell.processInput(String.format("> %s", target)));
            assertEquals("", output);
            assertTrue(Files.exists(target) && Files.isRegularFile(target));
        }

        @Test
        public void testRedirectIsDirectoryError() throws Exception {
            String output = SystemLambda.tapSystemOut(() -> shell.processInput("pwd > test-resources/dir-1"));
            String err = SystemLambda.tapSystemErr(() -> shell.processInput("pwd > test-resources/dir-1"));
            assertEquals("", output);
            assertEquals(String.format("test-resources/dir-1: Is a directory%n"), err);
        }
    }

}
