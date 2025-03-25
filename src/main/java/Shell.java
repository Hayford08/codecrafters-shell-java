import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class Shell {
  private static final Set<String> COMMANDS = Set.of("exit", "echo", "type");
  private final Map<String, String> executableToPath = new HashMap<>();
  private final Scanner scanner = new Scanner(System.in);
  
  public Shell() {
    // init 
    String systemPath = System.getenv("PATH");
    if (systemPath != null) {
      for (String dir : systemPath.split(":")) {
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
            executableToPath.put(file.getName(), dir);
          }
        }
      }
    }
  }

  public void run() {
    while (true) {
      System.out.print("$ ");
      String input = scanner.nextLine().trim();
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
      String fileName = tokens.get(redirectIndex + 2);
      Path path = Paths.get(fileName);
      Files.createDirectories(path.getParent());
      PrintStream out = new PrintStream(new FileOutputStream(fileName, false));

      List<String> numTokens = tokens.subList(0, redirectIndex);
      handleCommand(numTokens, String.join("", numTokens), out);
      return;
    }
    handleCommand(tokens, input, System.out);
  }

  private void handleCommand(List<String> tokens, String input, PrintStream out) throws Exception {
    String command = tokens.get(0);
    if (command.equals("exit")) {
      handleExit(input);
      return;
    }
    if (command.equals("echo")) {
      handleEcho(tokens, out);
      return;
    }
    if (command.equals("type")) {
      handleType(tokens, input, out);
      return;
    }
    if (command.equals("cat")) {
      handleCat(tokens, out);
      return;
    }
    handleExternalCommand(tokens, out);
  }

  private void handleExit(String input) {
    scanner.close();
    try {
      int index = input.indexOf("exit") + "exit".length();
      System.exit(Integer.parseInt(input.substring(index).trim()));      
    }
    catch (Exception e) {
      System.exit(1);
    }
  }

  private void handleEcho(List<String> tokens, PrintStream out) {
    if (tokens.size() == 1) {
      out.println();
      return;
    } 
    StringBuilder sb = new StringBuilder();
    for (int i = 2; i < tokens.size(); i++) {
      sb.append(tokens.get(i));
    }
    out.println(sb);
  }

  private void handleType(List<String> tokens, String input, PrintStream out) {
    int index = input.indexOf("type") + "type".length();
    String executable = input.substring(index).trim();
    if (COMMANDS.contains(executable)) {
      out.println(executable + " is a shell builtin");
      return;
    }

    String systemPath = System.getenv("PATH");
    if (systemPath != null) {
      for (String dir : systemPath.split(":")) {
        Path path = Paths.get(dir, executable);
        if (Files.isExecutable(path)) {
          out.println(executable + " is " + path.toString());
          return;
        }
      }
    }

    out.println(executable + ": not found");
  }

  private void handleCat(List<String> tokens, PrintStream out) throws Exception {
    if (tokens.size() == 1) {
      out.println("cat: missing operand");
      return;
    }
    StringBuilder sb = new StringBuilder();
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
          out.println(output);
        }
        System.out.println("cat: " + tokens.get(i) + ": No such file or directory");
        return;
      }
    }
    out.println(sb.toString().trim());
  }

  private void handleExternalCommand(List<String> tokens, PrintStream out) throws IOException {
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
    Process process = Runtime.getRuntime().exec(commands.toArray(new String[0]));
    out.println(Utils.getProcessOutput(process));
  }
}
