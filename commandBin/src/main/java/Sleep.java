import java.util.concurrent.TimeUnit;

/**
 * {@code sleep <seconds>}: blocks for the given (fractional) number of seconds, then exits.
 *
 * <p><b>Given to you as a worked example.</b> Used to give {@code Shell.java}'s job control
 * ({@code jobs}/{@code kill}/background {@code &}) something slow to test against — see
 * {@code JobTests} in {@code ShellTest.java}.
 */
public class Sleep extends ShellCommand {

    public Sleep(String[] args) {
        super(args);
    }

    public static void main(String[] args) {
        ShellCommand.start(Sleep.class, args);
    }

    @Override
    protected void runCommand() {
        double seconds = Double.parseDouble(cmdArgs[0]);
        try {
            TimeUnit.MILLISECONDS.sleep((long) (seconds * 1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
