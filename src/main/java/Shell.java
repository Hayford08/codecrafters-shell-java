import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Shell {
  private final Set<String> BUILTIN_COMMANDS = Set.of("exit", "echo", "type");
  private final Map<String, String> executableToPath = new HashMap<>();
  private final Trie commandTrie = new Trie();
  
  public Shell() {
    // init
    String systemPath = System.getenv("PATH");
    for (String cmd : BUILTIN_COMMANDS) {
      commandTrie.insert(cmd);
    }
    if (systemPath != null) {
      for (String dir : systemPath.split(File.pathSeparator)) {
        File directory = new File(dir);
        if (!directory.isDirectory()) {
          continue;
        }
        File[] files = directory.listFiles();
        if (files == null) {
          continue;
        }
        for (File file : files) {
          if (file.isFile() && file.canExecute()) {
            commandTrie.insert(file.getName());
            executableToPath.put(file.getName(), dir);
          }
        }
      }
    }
  }

  public void run() {
    while (true) {
      String input = Utils.processInputCommand(commandTrie);
      List<String> tokens = Utils.tokenizeInputString(input);
      if (tokens.isEmpty()) {
        continue;
      }
      try {
        runShellCommand(tokens, input);
      }
      catch (Exception e) {
        System.out.println("An error occurred: " + e.getMessage());
        break;
      }
    }
  }

  private void runShellCommand(List<String> tokens, String input) throws Exception {
    Integer redirectIndex = Utils.findRedirectIndex(tokens);
    if (redirectIndex != null && redirectIndex < tokens.size() - 2) {
      Utils.OutputType redirectType = Utils.getRedirectType(tokens.get(redirectIndex));

      String fileName = tokens.get(redirectIndex + 2);
      Path path = Paths.get(fileName);
      Files.createDirectories(path.getParent());
      PrintStream out = new PrintStream(new FileOutputStream(fileName, true));

      List<String> numTokens = tokens.subList(0, redirectIndex);
      handleCommand(numTokens, String.join("", numTokens), out, redirectType);
      return;
    }
    handleCommand(tokens, input, System.out, Utils.OutputType.REDIRECT_STDOUT);
  }

  private void handleCommand(List<String> tokens, String input, PrintStream out, Utils.OutputType outputType) {
    String command = tokens.get(0);
    if (command.equals("exit")) {
      handleExit(input);
      return;
    }
    if (command.equals("echo")) {
      handleEcho(tokens, out, outputType);
      return;
    }
    if (command.equals("type")) {
      handleType(input, out, outputType);
      return;
    }
    if (command.equals("cat")) {
      handleCat(tokens, out, outputType);
      return;
    }
    handleExternalCommand(tokens, out, outputType);
  }

  private void handleExit(String input) {
    try {
      int index = input.indexOf("exit") + "exit".length();
      System.exit(Integer.parseInt(input.substring(index).trim()));      
    }
    catch (Exception e) {
      System.exit(1);
    }
  }

  private void handleEcho(List<String> tokens, PrintStream out, Utils.OutputType outputType) {
    if (tokens.size() == 1) {
      return;
    } 
    StringBuilder sb = new StringBuilder();
    for (int i = 2; i < tokens.size(); i++) {
      sb.append(tokens.get(i));
    }
    Utils.writeToOutput(sb.toString().trim(), Utils.OutputType.REDIRECT_STDOUT, out, outputType);
  }

  private void handleType(String input, PrintStream out, Utils.OutputType outputType) {
    int index = input.indexOf("type") + "type".length();
    String executable = input.substring(index).trim();
    if (BUILTIN_COMMANDS.contains(executable)) {
      Utils.writeToOutput(executable + " is a shell builtin", Utils.OutputType.REDIRECT_STDOUT, out, outputType);
      return;
    }

    String systemPath = System.getenv("PATH");
    if (systemPath != null) {
      for (String dir : systemPath.split(":")) {
        Path path = Paths.get(dir, executable);
        if (Files.isExecutable(path)) {
          Utils.writeToOutput(executable + " is " + path, Utils.OutputType.REDIRECT_STDOUT, out, outputType);
          return;
        }
      }
    }
    Utils.writeToOutput(executable + ": not found", Utils.OutputType.REDIRECT_STDERR, out, outputType);
  }

  private void handleCat(List<String> tokens, PrintStream out, Utils.OutputType outputType) {
    if (tokens.size() == 1) {
      Utils.writeToOutput("cat: missing operand", Utils.OutputType.REDIRECT_STDERR, out, outputType);
      return;
    }
    StringBuilder sb = new StringBuilder();
    try {
      for (int i = 1; i < tokens.size(); i++) {
        if (tokens.get(i).equals(" ")) {
          continue;
        }
        Path path = Paths.get(tokens.get(i));
        if (Files.exists(path)) {
          sb.append(Files.readString(path));
        }
        else {
          String output = sb.toString().trim();
          if (!output.isEmpty()) {
            Utils.writeToOutput(output, Utils.OutputType.REDIRECT_STDOUT, out, outputType);
          }
          Utils.writeToOutput("cat: " + tokens.get(i) + ": No such file or directory", Utils.OutputType.REDIRECT_STDERR, out, outputType);
          return;
        }
      }
      Utils.writeToOutput(sb.toString().trim(), Utils.OutputType.REDIRECT_STDOUT, out, outputType);
    }
    catch (IOException e) {
      Utils.writeToOutput(e.getMessage(), Utils.OutputType.REDIRECT_STDERR, out, outputType);
    }
  }

  private void handleExternalCommand(List<String> tokens, PrintStream out, Utils.OutputType outputType) {
    String path = executableToPath.get(tokens.get(0));
    if (path == null) {
      out.println(tokens.get(0) + ": not found");
      return;
    }
    ArrayList<String> commands = new ArrayList<>();
    commands.add(tokens.get(0));
    for (int i = 1; i < tokens.size(); i++) {
      if (tokens.get(i).equals(" ")) {
        continue;
      }
      commands.add(tokens.get(i));
    }
    try {
      Process process = Runtime.getRuntime().exec(commands.toArray(new String[0]));
      int exitCode = process.waitFor();
      Utils.writeToOutput(Utils.getProcessOutput(process, exitCode), exitCode == 0 ? Utils.OutputType.REDIRECT_STDOUT : Utils.OutputType.REDIRECT_STDERR, out, outputType);
    }
    catch (Exception e) {
      Utils.writeToOutput(e.getMessage(), Utils.OutputType.REDIRECT_STDERR, out, outputType);
    }
  }
}
