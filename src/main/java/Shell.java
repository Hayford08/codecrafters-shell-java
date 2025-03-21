import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
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

  public void run() throws Exception {
    while (true) {
      System.out.print("$ ");
      String input = scanner.nextLine().trim();
      ArrayList<String> tokens = Utils.tokenizeInputString(input);
      if (tokens.isEmpty()) {
        continue;
      }
      handleCommand(tokens, input);
    }
  }

  private void handleCommand(ArrayList<String> tokens, String input) throws IOException {
    String command = tokens.get(0);
    if (command.equals("exit")) {
      handleExit(input);
    }
    else if (command.equals("echo")) {
      handleEcho(tokens);
    }
    else if (command.equals("type")) {
      handleType(tokens, input);
    }
    else if (command.equals("cat")) {
      handleCat(tokens);
    }
    else {
      handleExternalCommand(tokens);
    }
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

  private void handleEcho(ArrayList<String> tokens) {
    if (tokens.size() == 1) {
      System.out.println();
    } 
    else {
      for (int i = 2; i < tokens.size(); i++) {
        System.out.print(tokens.get(i));
      }
      System.out.println();
    }
  }

  private void handleType(ArrayList<String> tokens, String input) {
    int index = input.indexOf("type") + "type".length();
    String executable = input.substring(index).trim();
    if (COMMANDS.contains(executable)) {
      System.out.println(executable + " is a shell builtin");
    }
    else {
      String path = executableToPath.get(executable);
      if (path == null) {
        System.out.println(executable + ": command not found");
      }
      else {
        System.out.println(executable + " is " + path + "/" + executable);
      }
    }
  }

  private void handleCat(ArrayList<String> tokens) throws IOException {
    if (tokens.size() == 1) {
      System.out.println("cat: missing operand");
    }
    else {
      StringBuilder sb = new StringBuilder();
      for (int i = 1; i < tokens.size(); i++) {
        if (tokens.get(i).equals(" ")) {
          continue;
        }
        Path path = Paths.get(tokens.get(i));
        if (Files.exists(path)) {
          Files.lines(path).forEach(sb::append);
        }
        else {
          sb.append(tokens.get(i));
        }
      }
    }
  }

  private void handleExternalCommand(ArrayList<String> tokens) throws IOException {
    String path = executableToPath.get(tokens.get(0));
    if (path == null) {
      System.out.println(tokens.get(0) + ": command not found");
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
    process.getInputStream().transferTo(System.out);
  }
}
