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
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Utils {
  static final String CHARACTERS_TO_PRESERVE = "$\"\\\n";
  private Utils() {
    throw new IllegalStateException("Utility class");
  }

  static public String processInputCommand(Set<String> commands) {
    try {
      Logger jlineLogger = Logger.getLogger("org.jline");
      jlineLogger.setLevel(Level.OFF);

      Terminal terminal = TerminalBuilder.builder().system(true).build();
      Completer completer = new StringsCompleter(commands);
      DefaultParser parser = new DefaultParser();
      parser.setEscapeChars("".toCharArray());
      LineReader lineReader = LineReaderBuilder.builder().terminal(terminal).completer(completer).parser(parser).build();
      return lineReader.readLine("$ ");
    } catch (IOException e) {
      e.printStackTrace();
      return "";
    }
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
