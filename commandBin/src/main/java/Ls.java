/**
 * {@code ls [<path>...]}: lists files and directories. With no arguments, lists the current
 * directory.
 *
 * <p>Files print first (sorted by name), then directories (sorted by name), blank-line separated.
 * A directory's contents get a {@code "<dir>:"} header only when more than one argument was given.
 */
public class Ls extends ShellCommand {

    public Ls(String[] args) {
        super(args);
    }

    public static void main(String[] args) {
        ShellCommand.start(Ls.class, args);
    }

    public static void listFilesAndDirectories(String path) {
        java.io.File dir = new java.io.File(path);
        if (dir.isDirectory()) {
            java.io.File[] files = dir.listFiles();
            if (files != null) {
                // One list of everything in the directory, sorted by name
                java.util.List<java.io.File> entries = new java.util.ArrayList<>();
                for (java.io.File file : files) {
                    // skip hidden files like .DS_Store, as real ls does
                    if (!file.getName().startsWith(".")) {
                        entries.add(file);
                    }
                }
                entries.sort(java.util.Comparator.comparing(java.io.File::getName));

                for (java.io.File entry : entries) {
                    System.out.println(entry.getName());
                }
            }
        } else {
            System.err.println("ls: cannot access '" + path + "': Not a directory");
        }
    }

    @Override
    protected void runCommand() {

        // check if there are any arguments, if no, use current working directory
        // if there is an agument, check if its a valid path.
        // if not valid, print an error message and exit with non-zero status code
        // if valid, list the files and directories in the given path
        if (cmdArgs.length == 0) {
            // no arguments, use current working directory
            String currentDir = System.getProperty("user.dir");
            listFilesAndDirectories(currentDir);
        } else {
            // Sort files before directories, lexicographically within each group.
            java.util.List<java.io.File> files = new java.util.ArrayList<>();
            java.util.List<java.io.File> directories = new java.util.ArrayList<>();
            for (String path : cmdArgs) {
                java.io.File file = new java.io.File(path);
                if (!file.exists()) {
                    System.err.println("ls: " + path + ": No such file or directory");
                } else if (file.isDirectory()) {
                    directories.add(file);
                } else {
                    files.add(file);
                }
            }
            java.util.Comparator<java.io.File> byPath = java.util.Comparator.comparing(java.io.File::getPath);
            files.sort(byPath);
            directories.sort(byPath);

            for (java.io.File file : files) {
                System.out.println(file.getPath());
            }
            if (!files.isEmpty() && !directories.isEmpty()) {
                System.out.println();
            }
            for (int index = 0; index < directories.size(); index++) {
                java.io.File directory = directories.get(index);
                if (cmdArgs.length > 1) {
                    System.out.println(directory.getPath() + ":");
                }
                listFilesAndDirectories(directory.getPath());
                if (cmdArgs.length > 1 && index < directories.size() - 1) {
                    System.out.println(); // blank line after directory listing
                }
            }
        }

    }

}
