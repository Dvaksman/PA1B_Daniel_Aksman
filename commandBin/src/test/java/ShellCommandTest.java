import org.junit.jupiter.api.BeforeEach;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;

public abstract class ShellCommandTest {

    @BeforeEach
    public void setQuietOutput() {
        // Create in-memory buffers to catch the output quietly
        ByteArrayOutputStream outputBuffer1 = new ByteArrayOutputStream();
        PrintStream quietPrintStream1 = new PrintStream(outputBuffer1);
        System.setOut(quietPrintStream1);
        ByteArrayOutputStream outputBuffer2 = new ByteArrayOutputStream();
        PrintStream quietPrintStream2 = new PrintStream(outputBuffer2);
        System.setErr(quietPrintStream2);
    }

    public String getFileContent(String filename) throws Exception {
        return Files.readString(Path.of(filename));
    }

    public String simulateInput(ShellCommand cmd, String input) throws Exception{
        InputStream originalSystemIn = System.in;
        String simulatedInput = input + "\u0004";
        try {
            ByteArrayInputStream testInput = new ByteArrayInputStream(simulatedInput.getBytes());
            System.setIn(testInput);
            return tapSystemOut(cmd::runCommand);
        } finally {
            System.setIn(originalSystemIn);
        }
    }
}
