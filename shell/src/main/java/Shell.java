import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A simple shell that mimics some basic features of bash. Java has no real fork/exec, so each
 * command runs as a separate forked {@code java} process (see {@link ProcessBuilder}) that locates
 * and loads the real command class at runtime via a {@link URLClassLoader} and
 * <a href="https://docs.oracle.com/javase/tutorial/reflect/">reflection</a> — {@code shell} has no
 * compile-time dependency on {@code commandBin}. See {@code README.md} for the full picture;
 * {@link #executeCommand} is where it comes together.
 */
public class Shell {
    public static final String COMMAND_PROMPT = "> ";
    private static final Path PATH = getPath();
    private final List<Pipeline> jobs = new ArrayList<>();
    /**
     * Represents the current working directory of the shell
     * (since the actual current working directory of a JVM cannot be changed).
     * This is the directory that command processes will be run in.
     */
    private Path cwd = Paths.get(System.getProperty("user.dir"));

    /**
     * Returns the path of the {@code Shell.class} file, via {@code getProtectionDomain()
     * .getCodeSource().getLocation()} and a
     * <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/net/URI.html">URI</a>
     * conversion ({@link Paths#get(java.net.URI)} needs a {@code URI}, not a {@link URL}).
     */
    public static Path getCurrentClassPath() {
        try {
            return Paths.get(Shell.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Absolute path of {@code commandBin}'s compiled classes, computed relative to
     * {@link #getCurrentClassPath} so it isn't hardcoded to one machine's checkout location.
     */
    public static Path getPath() {
        return getCurrentClassPath().resolve("../../../commandBin/target/classes").toAbsolutePath().normalize();
    }

    public static void main(String[] args) {
        Shell shell = new Shell();
        if (args.length > 0) {
            shell.executeCommand(args[0]);
        } else {
            shell.runRepl();
        }
    }

    /**
     * Searches the PATH directory for a .class file corresponding to the given command.
     * Class names are converted from PascalCase to kebab-case before attempting to match.
     *
     * @param command the command name
     * @return the matched class name
     * @throws Exception if a match class cannot be found
     */
    private static String findCommandClass(String command) throws Exception {
        // Convert command name to PascalCase
        String[] parts = command.split("-");
        StringBuilder classNameBuilder = new StringBuilder();
        for (String part : parts) {
            classNameBuilder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        String className = classNameBuilder.toString();

        // Check if the corresponding .class file exists in the PATH directory
        Path classFilePath = PATH.resolve(className + ".class");
        if (!classFilePath.toFile().exists()) {
            throw new Exception(command + ": command not found");
        }

        return className;
    }

    /**
     * Converts a PascalCase class name to kebab-case (e.g. {@code "WordCount"} to
     * {@code "word-count"}), splitting on lower-to-upper boundaries via
     * <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html">lookaround regex</a>
     * so acronym runs stay together.
     *
     * @param className the java class name
     * @return the kebab-case equivalent command name
     */
    public static String classNameToCommandName(String className) {
        // Split on uppercase letter boundaries, handling acronyms safely
        String[] words = className.split("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])");
        // Lowercase and join with hyphens
        return Arrays.stream(words)
                .map(String::toLowerCase)
                .collect(Collectors.joining("-"));
    }

    public Path getCwd() {
        return cwd;
    }

    public void setCwd(Path path) {
        cwd = path;
    }

    /**
     * Runs the Read-Eval-Print Loop of the Shell. The command "exit" ends the loop.
     */
    public void runRepl() {
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        while (true) {
            System.out.print(COMMAND_PROMPT);
            String command = scanner.nextLine();
            if (command.equals("exit")) {
                break;
            }
            processInput(command);
        }
    }

    /**
     * Takes the input string and parses it to execute either a shell builtin or a pipeline of separate processes.
     *
     * @param input the command to execute
     * @return null if the input was a builtin command or if there was an error,
     * otherwise the pipeline that was executed (for testing purposes)
     */
    Pipeline processInput(String input) {
        // a blank line does nothing
        if (input == null || input.isBlank()) {
            return null;
        }
        // builtins (cd, jobs, kill) run here in the shell itself
        if (tryExecuteBuiltin(input)) {
            return null;
        }
        // everything else runs as a pipeline of forked processes
        try {
            Pipeline pipeline = new Pipeline(input);
            pipeline.executePipeline();
            if (pipeline.isBackground()) {
                jobs.add(pipeline);
            }
            return pipeline;
        } catch (IllegalArgumentException e) {
            // e.g. "test-resources/dir-1: Is a directory" from a bad redirect
            System.err.println(e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

    /**
     * Executes the main method of the given command, passing along any additional args.
     * Should only be called when {@code Shell.main()} is called with non-zero number of args.
     * Does nothing if the given command is blank (only whitespace).
     *
     * <p><b>Given to you as a worked example</b> — this is where the class Javadoc's three ideas
     * (URI, ClassLoader, reflection) actually get used; you don't need to write this kind of code
     * yourself, but understanding it will help you debug {@link #findCommandClass} and, later,
     * extend the shell:
     * <ol>
     *     <li>{@link #findCommandClass} resolves the command name to a class name.</li>
     *     <li>{@link #PATH} (the directory holding {@code commandBin}'s compiled classes) is
     *     turned into a {@link URL}.</li>
     *     <li>A {@link URLClassLoader} opened on that URL loads the class — this is what makes
     *     the class reachable at all, since {@code commandBin} isn't on this module's classpath.</li>
     *     <li>{@code loadedClass.getMethod("main", String[].class)} then
     *     {@code .invoke(null, (Object) commandArgs)} calls that class's {@code main} via
     *     reflection — the same cast trick as {@code ShellCommand.start()}.</li>
     *     <li>Because this only ever runs inside a freshly-forked child JVM whose whole purpose
     *     was to run this one command, it reports its own exit status
     *     ({@code System.exit(0)}/{@code System.exit(1)}) like any Unix process would.</li>
     * </ol>
     * See {@code README.md} for a plainer walkthrough of the four ideas.
     *
     * @param command the command to execute
     */
    void executeCommand(String command) {
        if (command.isBlank()) {
            return;
        }
        String[] commandArgs = command.split("\\s+");
        String commandName = commandArgs[0];
        try {
            // Get Class name for the command
            String commandClass = findCommandClass(commandName);
            // Create URL pointing to the classpath location
            File file = new File(String.valueOf(PATH));
            URL url = file.toURI().toURL();

            // Define the isolated ClassLoader
            try (URLClassLoader loader = new URLClassLoader(new URL[]{url}, Thread.currentThread().getContextClassLoader())) {
                // Load the target class
                Class<?> loadedClass = Class.forName(commandClass, true, loader);
                java.lang.reflect.Method mainMethod = loadedClass.getMethod("main", String[].class);
                mainMethod.invoke(null, (Object) commandArgs);
            }
            System.exit(0);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Runs {@code input} as a builtin ({@code cd}, {@code kill}, {@code jobs}) if it is one.
     * Builtins run in the main shell process, not forked, since they mutate the shell's own state
     * ({@code cwd}, {@code jobs}).
     *
     * @param input the command string
     * @return true if the command was a builtin, false otherwise
     */
    private boolean tryExecuteBuiltin(String input) {
        String[] args = input.trim().split("\\s+");
        String command = args[0];
        switch (command) {
            case "cd":
                if (args.length > 1) {
                    changeDirectory(args[1]);
                } else {
                    System.err.println("cd: missing argument");
                }
                return true;
            case "kill":
                // every argument must be a positive whole number before anything is killed
                List<Integer> ids = new ArrayList<>();
                for (int i = 1; i < args.length; i++) {
                    int id;
                    try {
                        id = Integer.parseInt(args[i]);
                    } catch (NumberFormatException e) {
                        id = -1;
                    }
                    if (id <= 0) {
                        System.err.println("kill: arguments must be job IDs");
                        return true;
                    }
                    ids.add(id);
                }
                if (ids.isEmpty()) {
                    System.err.println("kill: arguments must be job IDs");
                    return true;
                }
                killJobs(ids);
                return true;
            case "jobs":
                listJobs();
                return true;
            default:
                return false;
        }
    }

    /**
     * Builds the {@code ProcessBuilder} for
     * {@code java -classpath <Shell's own classes> Shell "<command>"} — the fork workaround
     * described in the class Javadoc. Sets the child's working directory to {@link #cwd}, and
     * redirects output to a file if {@code command} ends in {@code > <file>}.
     *
     * @param command the string passed to the new {@code Shell} process as an argument
     *                (after removing the redirect if there is one)
     * @return the {@code ProcessBuilder} for the "forked" process
     * @throws IllegalArgumentException if a redirect file is given and it is a directory
     */
    ProcessBuilder buildForkedShell(String command) throws IllegalArgumentException {
        // split off an output redirect: "<command> > <file>"
        String fileName = null;
        int redirectIndex = command.indexOf('>');
        if (redirectIndex >= 0) {
            fileName = command.substring(redirectIndex + 1).trim();
            command = command.substring(0, redirectIndex).trim();
        }

        ProcessBuilder pb = new ProcessBuilder(
                "java", "-classpath", getCurrentClassPath().toString(), "Shell", command);
        // run the child in the shell's tracked working directory
        pb.directory(cwd.toFile());
        // errors go straight to the terminal; stdin/stdout stay PIPE until the Pipeline wires them
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);

        if (fileName != null) {
            // resolve against the shell's cwd, not the JVM's
            Path target = cwd.resolve(fileName);
            if (java.nio.file.Files.isDirectory(target)) {
                throw new IllegalArgumentException(fileName + ": Is a directory");
            }
            pb.redirectOutput(target.toFile());
        }
        return pb;
    }

    /**
     * Changes the shell's working directory by updating the value of the {@code cwd} field.
     *
     * @param pathString the absolute or relative path string to change to
     */
    void changeDirectory(String pathString) {
        Path newPath = cwd.resolve(pathString).normalize();
        if (java.nio.file.Files.isDirectory(newPath)) {
            cwd = newPath;
        } else {
            System.err.println("cd: " + pathString + ": No such file or directory");
        }
    }

    /**
     * Prints background jobs and their status to standard out.
     * A job should only be removed from the list if it was shown in a previous call with the "Done" or "Terminated" status.
     * Job numbering does not restart unless the job list becomes empty.
     */
    void listJobs() {
        // job id = index + 1; a removed job leaves a null in its slot so later ids don't shift
        for (int i = 0; i < jobs.size(); i++) {
            Pipeline job = jobs.get(i);
            if (job == null) {
                continue;
            }
            // read the status once, so what is printed matches what is decided below
            String status = job.getStatus();
            System.out.printf("%-10s%-20s %s%n", "[" + (i + 1) + "]", status, job.getPipelineCommand());
            // a finished job is removed only after it has been shown once
            if (status.equals("Done") || status.equals("Terminated")) {
                jobs.set(i, null);
            }
        }
        // numbering restarts only when the list becomes empty
        boolean allRemoved = true;
        for (Pipeline job : jobs) {
            if (job != null) {
                allRemoved = false;
                break;
            }
        }
        if (allRemoved) {
            jobs.clear();
        }
    }


    /**
     * Kills the given jobs. The ids must be existing job ids.
     *
     * @param ids the ids of the jobs to kill
     */
    void killJobs(List<Integer> ids) {
        for (int id : ids) {
            int index = id - 1;
            if (index < 0 || index >= jobs.size() || jobs.get(index) == null) {
                System.err.println("kill: " + id + ": no such job");
                continue;
            }
            // stays in the list so the next "jobs" shows it as Terminated
            jobs.get(index).killPipeline();
        }
    }

    /**
     * Represents a pipeline: a list of commands that run as separate processes, with their inputs and outputs linked
     * by pipes.
     */
    public class Pipeline {
        private final String pipelineCommand;
        private final List<ProcessBuilder> pbs;
        private final boolean isBackground;
        private List<Process> processes = null;
        private boolean isDone = false;
        private boolean terminated = false;

        /**
         * Splits {@code input} on {@code |}, builds a forked process for each subcommand via
         * {@link #buildForkedShell}, and wires the first process's stdin and (unless already
         * redirected to a file) the last process's stdout to this shell's own. A trailing
         * {@code &} marks the pipeline as a background job.
         *
         * @param input the string command for the pipeline
         */
        public Pipeline(String input) throws IllegalArgumentException {
            // keep the command exactly as typed (with any "&"): "jobs" prints it back
            this.pipelineCommand = input;
            String commandLine = input.trim();
            this.isBackground = commandLine.endsWith("&");
            if (isBackground) {
                commandLine = commandLine.substring(0, commandLine.length() - 1).trim();
            }

            List<ProcessBuilder> builders = new ArrayList<>();
            for (String cmd : commandLine.split("\\|")) {
                builders.add(buildForkedShell(cmd.trim()));
            }
            // first process reads from the shell's stdin
            builders.getFirst().redirectInput(ProcessBuilder.Redirect.INHERIT);
            // last process writes to the shell's stdout, unless it already redirects to a file
            ProcessBuilder last = builders.getLast();
            if (last.redirectOutput() == ProcessBuilder.Redirect.PIPE) {
                last.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            }
            this.pbs = builders;
        }

        /**
         * Starts every process in the pipeline, connected by real OS pipes via
         * <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ProcessBuilder.html#startPipeline(java.util.List)">ProcessBuilder#startPipeline</a>,
         * and waits for them to finish unless this is a background job.
         *
         * @return the list of processes in the pipeline
         */
        public List<Process> executePipeline() throws Exception {
            // start every process, connected to each other by real OS pipes
            processes = ProcessBuilder.startPipeline(pbs);
            // a foreground job blocks the shell until it finishes
            if (!isBackground) {
                waitFor();
            }
            return processes;
        }

        /**
         * Waits for all processes in the pipeline to complete.
         * Does nothing if {@code executePipeline()} has not been called.
         */
        void waitFor() throws InterruptedException {
            if (processes == null) {
                return;
            }
            for (Process p : processes) {
                p.waitFor();
            }
        }

        /**
         * Terminates all processes in the pipeline. {@link Process#destroy()} only requests
         * termination asynchronously, so this also waits for each process to actually exit
         * before returning.
         */
        public void killPipeline() {
            if (processes != null) {
                // destroy() only requests termination...
                for (Process p : processes) {
                    p.destroy();
                }
                // ...so wait until each process has actually exited
                for (Process p : processes) {
                    try {
                        p.waitFor();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            terminated = true;
        }

        /**
         * Checks if all processes in the pipeline have finished execution.
         *
         * @return false if any processes are still alive, otherwise true
         */
        public boolean isDone() {
            if (isDone) {
                return true;
            }
            // not started yet
            if (processes == null) {
                return false;
            }
            for (Process p : processes) {
                if (p.isAlive()) {
                    return false;
                }
            }
            isDone = true;
            return true;
        }

        /**
         * Checks if the pipeline command is a background job.
         *
         * @return true if pipeline runs in the background, false if runs in the foreground
         */
        public boolean isBackground() {
            return isBackground;
        }

        /**
         * Checks if the pipeline command was terminated with a call to {@code killPipeline}.
         *
         * @return true if {@code killPipeline} was called, otherwise false
         */
        public boolean isTerminated() {
            return terminated;
        }

        public String getPipelineCommand() {
            return pipelineCommand;
        }

        public List<ProcessBuilder> getPbs() {
            return pbs;
        }

        public List<Process> getProcesses() {
            return processes;
        }

        /** This job's {@code jobs} status: {@code "Terminated"}, {@code "Done"}, or {@code "Running"}. */
        public String getStatus() {
            return terminated ? "Terminated" : (isDone() ? "Done" : "Running");
        }

        @Override
        public String toString() {
            return String.format("%-20s %s", getStatus(), pipelineCommand);
        }
    }
}
