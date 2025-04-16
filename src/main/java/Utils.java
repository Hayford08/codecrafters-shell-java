import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Utils {
  static final String CHARACTERS_TO_PRESERVE = "$\"\\\n";
  static final int TAB = 9, ENTER = 10, RING = 7, BACKSPACE = 127;
  private Utils() {
    throw new IllegalStateException("Utility class");
  }

  private static void enableRawMode() {
    String[] cmd = {"/bin/sh", "-c", "stty -icanon min 1 -echo < /dev/tty"};
    try {
      Process process = Runtime.getRuntime().exec(cmd);
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        System.exit(1);
      }
    } catch (Exception e) {
      System.exit(1);
    }
  }

  private static void disableRawMode() {
    String [] cmd = {"/bin/sh", "-c", "stty icanon echo < /dev/tty"};
    try {
      Process process = Runtime.getRuntime().exec(cmd);
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        System.exit(1);
      }
    } catch (Exception e) {
      System.exit(1);
    }
  }

  private static String getLongCommonPrefix(List<String> words) {
    if (words.isEmpty()) {
      return "";
    }
    String prefix = words.get(0);
    for (int i = 1; i < words.size(); i++) {
      String curr = words.get(i);
      int p1 = 0, p2 = 0;
      while (p1 < prefix.length() && p2 < curr.length() && prefix.charAt(p1) == curr.charAt(p2)) {
        p1++;
        p2++;
      }
      prefix = prefix.substring(0, p1);
    }
    return prefix;
  }

  public static String processInputCommand(Trie commandTrie) {
    StringBuilder sb = new StringBuilder();
    enableRawMode();
    int tabCount = 0;
    List<String> matchedCommands = new ArrayList<>();
    System.out.print("$ ");
    try {
      while (true) {
        int key = System.in.read();
        if (key == -1) {
          break;
        }
        if (key == TAB) {
          if (tabCount == 0) {
            List<String> commands = commandTrie.getWordsWithPrefix(sb.toString());
            if (commands.isEmpty()) {
              System.out.print((char) RING);
              continue;
            }
            if (commands.size() == 1) {
              int len = sb.length();
              System.out.print(commands.get(0).substring(len) + " ");
              System.out.flush();
              sb.append(commands.get(0).substring(len)).append(" ");
            } else {
              tabCount++;
              String prefix = getLongCommonPrefix(commands);
              if (prefix.length() > sb.length()) {
                System.out.print(prefix.substring(sb.length()));
                System.out.flush();
                sb.append(prefix.substring(sb.length()));
              }
              matchedCommands = commands;
              System.out.print((char) RING);
            }
          }
          else {
            System.out.println();
            System.out.println(String.join("  ", matchedCommands));
            System.out.print("$ " + sb);
            tabCount = 0;
            matchedCommands.clear();
          }
        }
        else {
          tabCount = 0;
          if (key == ENTER) {
            System.out.println();
            break;
          }
          if (key == BACKSPACE) {
            if (sb.length() > 0) {
              sb.deleteCharAt(sb.length() - 1);
              System.out.print("\b \b");
            }
          } else {
            sb.append((char) key);
            System.out.print((char) key);
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    disableRawMode();
    return sb.toString().trim();
  }
  public enum OutputType {
    REDIRECT_STDOUT,
    REDIRECT_STDERR,
    APPEND_STDOUT,
    APPEND_STDERR
  }

  static public void writeOrAppendToFile(String output, PrintStream out, OutputType outputType) {
    if (outputType == OutputType.APPEND_STDERR || outputType == OutputType.APPEND_STDOUT) {
      out.append(output).append(System.lineSeparator());
      return;
    }
    out.println(output);
  }

  static public void writeToOutput(String output, OutputType outputType, PrintStream out, OutputType redirectType) {
    output = output.trim();
    if (output.isEmpty()) {
      return;
    }
    if (outputType == OutputType.REDIRECT_STDOUT && (redirectType == OutputType.APPEND_STDOUT || redirectType == OutputType.REDIRECT_STDOUT)) {
      writeOrAppendToFile(output, out, redirectType);
      return;
    }
    if (outputType == OutputType.REDIRECT_STDERR && (redirectType == OutputType.APPEND_STDERR || redirectType == OutputType.REDIRECT_STDERR)) {
      writeOrAppendToFile(output, out, redirectType);
      return;
    }
    System.out.println(output);
  }

  static public OutputType getRedirectType(String redirect) {
    if (redirect.equals(">") || redirect.equals("1>")) {
      return OutputType.REDIRECT_STDOUT;
    }
    if (redirect.equals(">>") || redirect.equals("1>>")) {
      return OutputType.APPEND_STDOUT;
    }
    if (redirect.equals("2>")) {
      return OutputType.REDIRECT_STDERR;
    }
    return OutputType.APPEND_STDERR;
  }

  static Integer findRedirectIndex(List<String> tokens) {
    for (int n = tokens.size(), i = 0; i < n; i++) {
      Pattern pattern = Pattern.compile("^[1-2]?>$|^[1-2]?>>$");
      if (pattern.matcher(tokens.get(i)).matches()) {
        return i;
      }
    }
    return null;
  }
  
  static List<String> tokenizeInputString(String input) {
    ArrayList<String> tokens = new ArrayList<>();
    int n = input.length();
    int index = 0;
    while (index < n) {
      if (Character.isWhitespace(input.charAt(index))) {
        while (index < n && Character.isWhitespace(input.charAt(index))) {
          index++;
        }
        tokens.add(" ");
      }
      else if (input.charAt(index) == '\'' || input.charAt(index) == '\"') {
        Pair<String, Integer> tokenResult = readQuotedToken(input, index);
        tokens.add(tokenResult.getFirst());
        index = tokenResult.getSecond();
      }
      else {
        Pair<String, Integer> tokenResult = readUnQuotedToken(input, index);
        tokens.add(tokenResult.getFirst());
        index = tokenResult.getSecond();
      }
    }
    return tokens;
  }

  static Pair<String, Integer> readQuotedToken(String input, int index) {
    char quote = input.charAt(index);
    index++;
    StringBuilder sb = new StringBuilder();
    int n = input.length();
    while (index < n && input.charAt(index) != quote) {
      if (quote == '\"' && input.charAt(index) == '\\' && index + 1 < input.length() && CHARACTERS_TO_PRESERVE.indexOf(input.charAt(index + 1)) != -1) {
        index++;
        sb.append(input.charAt(index));
        index++;
        continue;
      }
      sb.append(input.charAt(index));
      index++;
    }
    if (index >= n) {
      sb = new StringBuilder().append(quote).append(sb);
    }
    return new Pair<>(sb.toString(), index + 1);
  }

  static Pair<String, Integer> readUnQuotedToken(String input, int index) {
    StringBuilder sb = new StringBuilder();
    int n = input.length();
    while (index < n && input.charAt(index) != ' ') {
      if (input.charAt(index) == '\\' && index + 1 < n) {
        index++;
        sb.append(input.charAt(index));
        index++;
        continue;
      } 
      sb.append(input.charAt(index));
      index++;
    }
    return new Pair<>(sb.toString(), index);
  }

  static String getProcessOutput(Process process, Integer exitCode) throws IOException {
    BufferedReader reader;
    if (exitCode == 0) {
      reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    }
    else {
        reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
    }
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
      sb.append(line).append(System.lineSeparator());
    }
    return sb.toString().trim();
  }
}
