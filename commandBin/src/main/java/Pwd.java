/**
 * {@code pwd}: prints the current working directory.
 *
 * <p><b>Given to you as a worked example</b> — the simplest possible command. Runs as its own OS
 * process (see {@code README.md}), so it can just read {@code System.getProperty("user.dir")};
 * {@code Shell.buildForkedShell} already arranges for that to be correct.
 */
public class Pwd extends ShellCommand {

    public Pwd(String[] args) {
        super(args);
    }

    public static void main(String[] args) {
        ShellCommand.start(Pwd.class, args);
    }

    @Override
    protected void runCommand() {
        System.out.println(System.getProperty("user.dir"));
    }
}
