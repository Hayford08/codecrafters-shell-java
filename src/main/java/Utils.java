import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Utils {
  static final String CHARACTERS_TO_PRESERVE = "$\"\\";
  private Utils() {
    throw new IllegalStateException("Utility class");
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
      sb.append(quote);
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

  static Integer findRedirectIndex(List<String> tokens) {
    for (int n = tokens.size(), i = 0; i < n; i++) {
      Pattern pattern = Pattern.compile("^[1-9]?>$");
      if (pattern.matcher(tokens.get(i)).matches()) {
        return i;
      }
    }
    return null;
  }

  static String getProcessOutput(Process process) throws IOException {
    StringBuilder sb = new StringBuilder();
    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    String line;
    while ((line = reader.readLine()) != null) {
      sb.append(line).append(System.lineSeparator());
    }
    return sb.toString().trim();
  }
}
