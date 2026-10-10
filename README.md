# A Unix-ish Shell — Part B

This builds on Part A: **copy your working `commandBin` implementation and your `findCommandClass`
implementation over from your Part A project before you start** (see the assignment document's
"SetUp" section for exactly how) — everything else in this project is either unchanged
infrastructure or new for Part B.

You will expand your simple shell into one that reads commands, runs them (each as a real,
separate OS process, connected with real pipes — the same way your actual terminal works), and
prints their output.

For the full generated API reference (every class/method signature and doc comment), open
`doc/apidocs/index.html` in a browser (regenerate it anytime with `mvn javadoc:aggregate`).

## Getting started

> Already set up from Part A? Skip to "SetUp" in the assignment document — this section is the same
JDK/Maven guidance as Part A, here in case you're on a new machine or something's stopped working.

**1. JDK 21 or newer.** Check what you have: `java -version`. If that's below 21, install one:

- macOS: `brew install openjdk@21` ([Homebrew](https://brew.sh)), or download from
  [Adoptium](https://adoptium.net/temurin/releases/).
- Windows: install the Temurin 21 `.msi` from [Adoptium](https://adoptium.net/temurin/releases/)
  — check "Set JAVA_HOME" in the installer.
- Linux: `sudo apt install openjdk-21-jdk` (Debian/Ubuntu) or your distro's equivalent.

If `java -version` still shows an old version after installing, you likely have more than one JDK
on your machine — check `JAVA_HOME` and your shell's `PATH` ordering.

**2. Maven.** Check with `mvn -version` (its output also shows which Java it's using — make sure
that's 21+, too). If missing: `brew install maven` (macOS), `sudo apt install maven` (Linux), or
see the [official install guide](https://maven.apache.org/install.html) (Windows included).

**3. Sanity-check the build.** From the project root directory, run `mvn clean` then `mvn compile` — see "Did
I successfully set it up?" in the assignment document for what the output should look like.

## New ideas this assignment uses

Part A's shell loaded and invoked each command's class directly, in the same process — that's why
`Shell.executeCommand()` was already given to you there. Part B instead runs each command as a
real, separate OS process, so `jobs`/`kill` control real processes. That's one more idea most of you haven't used in Java before, on top of the three from
Part A (quick recap below).

- **Finding your own class on disk.** `Shell.class.getProtectionDomain().getCodeSource().getLocation()`
  returns a `URL`; converting it to a `Path` goes through a
  [`URI`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/net/URI.html)
  (`Paths.get(url.toURI())`) — `URL` has no direct conversion.
- **Loading a class you only know the name of.** `shell` has no compile-time dependency on
  `commandBin` (check `shell/pom.xml`), so you can't `import Wc`. A
  [`URLClassLoader`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/net/URLClassLoader.html)
  pointed at `commandBin`'s compiled classes loads it by name, at runtime, instead.
- **Calling a method you only know the name of.** Once loaded,
  [reflection](https://docs.oracle.com/javase/tutorial/reflect/) — `class.getMethod("main",
  String[].class)` then `.invoke(...)` — calls it. `ShellCommand.start()` does the same trick in
  miniature (on a constructor).
- **[new in Part B] Forking and piping real processes.** Java has no `fork()`/`exec()`; a
  [`ProcessBuilder`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ProcessBuilder.html)
  starts a whole new `java` process instead.
  [`ProcessBuilder.startPipeline`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ProcessBuilder.html#startPipeline(java.util.List))
  wires several processes' stdin/stdout together as real OS pipes, and a
  [`Process`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Process.html)
  handle's `isAlive()`/`waitFor()`/`destroy()` are genuine OS operations — that's what makes
  `jobs`/`kill` control real processes instead of flags on a Java object.

## Project structure

Two modules:

- **`commandBin`** — one class per command (`Cat`, `Grep`, `Ls`, `Pwd`, `Wc`, `Head`, `Tail`,
  `Sort`, `Uniq`, `Sleep`). Each one extends `ShellCommand` (given to you, don't modify it — read
  it first, it's short). **`Pwd` and `Sleep` are given to you, fully implemented, as worked
  examples** — start by reading them. You implement `runCommand()` for `Ls`, `Cat`, `Grep`, `Wc`
  (carried forward from Part A) and `Head`, `Tail`, `Sort`, `Uniq` (new for Part B).
- **`shell`** — `Shell.java`, the REPL and process-orchestration logic. `shell` does **not**
  depend on `commandBin` (check `shell/pom.xml`) — that's deliberate; see "New ideas" above for
  why. `getCurrentClassPath`, `getPath`, `executeCommand`, and `classNameToCommandName` are given;
  everything else — the `Pipeline` class, the `cd`/`jobs`/`kill` builtins, and
  `runRepl`/`processInput`/`tryExecuteBuiltin`/`buildForkedShell` — is what you are going to implement in Part B.

Every method you need to implement currently looks like this:

```java
// TODO: implement <method>
throw new UnsupportedOperationException("TODO: implement <method>");
```

Replace the `throw` with your implementation. The Javadoc directly above each method explains what
it needs to do; read it before you start.

## Commands

| Command | Reads piped input? | Produces piped output? | Description |
|---|---|---|---|
| `pwd` | no | yes | Prints the shell's current working directory. |
| `ls [<path>...]` | no | yes | Lists files/directories. With no args, lists the current directory. |
| `cd <dir>` | no | no | Changes the shell's current working directory. |
| `cat <file>...` | no | yes | Prints the contents of one or more files. |
| `grep <pattern> [<file>...]` | yes (if no files given) | yes | Prints lines matching a regex pattern. |
| `wc [<file>...]` | yes (if no files given) | yes | Prints line, word, and byte counts. |
| `head [-n <count>] [<file>...]` | yes (if no files given) | yes | Prints the first `count` lines (default 10) of each file. |
| `tail [-n <count>] [<file>...]` | yes (if no files given) | yes | Prints the last `count` lines (default 10) of each file. |
| `sort [-r] [<file>...]` | yes (if no files given) | yes | Prints all input lines sorted lexicographically (descending with `-r`). |
| `uniq [-c] [<file>]` | yes (if no file given) | yes | Collapses adjacent duplicate lines; `-c` prefixes each with its count. Takes at most one file. |
| `sleep <seconds>` | no | no | Blocks for the given number of seconds, then exits. |
| `<cmd> \| <cmd>` | — | — | Pipes the output of one command into the input of the next. |
| `> <file>` | yes | no (writes to a file instead) | Redirects the preceding command's output to a file. |
| `<cmd> &` | — | — | Runs a command in the background instead of waiting for it to finish. |
| `jobs` | no | no | Lists background jobs and their status (`Running` / `Done` / `Terminated`). |
| `kill <id>...` | no | no | Terminates the given background job(s) by their `jobs` id. |
| `exit` | no | no | Terminates the shell. |

Notes:
- `cd`, `kill`, and `jobs` are **builtins**: they run inside the main shell process itself (not
  as a separate forked process) because they need to change the shell's own state. See
  `Shell.tryExecuteBuiltin`.
- A pipeline of subcommands (`cat file.txt | grep foo | wc`) chains real OS processes together,
  each one's output becoming the next one's input — see `ProcessBuilder.startPipeline` above.

## Error handling

Every command should report errors to `System.err` and keep running rather than crashing. Look at
the `*Test.java` files for the exact expected error message format for each case (nonexistent
file, file that's actually a directory, invalid command, etc.) — the tests are the source of
truth here, not this table.

## Building and testing

Install Maven ([instructions](https://maven.apache.org/install.html)) and run it from the command
line — running tests through your IDE may not exercise the Maven plugins this project relies on.
Three commands come up constantly; here's what each one actually does.

**`mvn compile`** — compiles the main source only, no tests. A stub that just throws
`UnsupportedOperationException` is still valid Java, so this succeeds even before you've
implemented anything — it only confirms your JDK/Maven/project setup is correct. This is exactly
what the assignment document's "Did I successfully set it up?" check uses, and it's a good first
thing to run any time something feels broken: if `compile` fails, it's a setup or syntax problem;
if it succeeds but tests fail, that's just unfinished work.

**`mvn clean`** — deletes both modules' `target/` build output, including the copy of
`test-resources/` that gets made fresh for each module before testing. You rarely run this alone;
it's normally just the first half of `mvn clean test`, there so a previous run's stale compiled
classes or copied test files can't quietly affect the current one.

**`mvn clean test`** — cleans, compiles, *and* runs the full test suite. This is what you'll run
constantly while implementing. Expect test *failures* for anything you haven't implemented yet
(each throws `UnsupportedOperationException`) — that's normal. A *compile error*, on the other
hand, means your code doesn't build at all, and is always worth fixing first.

**The one thing that trips people up:** this project is a two-module Maven build (`commandBin`
then `shell`), and by default Maven **stops at the first module that fails** rather than
continuing on to the next one. So if `commandBin`'s tests are still failing — say, because you
haven't finished `head`/`tail`/`sort`/`uniq` yet — running `mvn clean test` from the project root
will never even attempt `shell`'s tests, and you'll see no `shell` output at all. That's not a
bug, and it doesn't mean anything is wrong with `shell` — the build just never got there. To test
`shell` on its own regardless of `commandBin`'s current state, target it directly and pass `-am`
("also make") so `commandBin` still gets *built* (just not necessarily fully passing) first:

```
mvn clean test -pl :shell -am
```

The same works the other way — `mvn clean test -pl :commandBin` runs just `commandBin`'s tests.
Still, run the whole project together (`mvn clean test`, no `-pl`) before you consider yourself
done: `shell`'s tests dynamically load `commandBin`'s *compiled* classes at runtime, so a broken
`commandBin` build can cause confusing failures in `shell`'s tests that have nothing to do with
your `Shell.java` code.

**Do not modify the `*Test.java` files** — they're the specification for this assignment. If your
code doesn't match what a test expects, the test is right and your code needs to change.
