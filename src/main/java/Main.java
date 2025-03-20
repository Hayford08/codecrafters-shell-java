import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;

public class Main {
    static Set<String> commands = Set.of("exit", "echo", "type");
    static final String CHARACTERS_TO_PRESERVE = "$\"\\";
    
    static ArrayList<String> tokenizeInputString(String input) {
        ArrayList<String> output = new ArrayList<>();
        int n = input.length();
        int index = 0;
        while (index < n) {
            boolean spaceFound = false;
            while (index < n && input.charAt(index) == ' ') {
                spaceFound = true;
                index++;
            }
            if (index >= n) {
                break;
            }
            if (spaceFound) {
                output.add(" ");
            }
            if (input.charAt(index) == '\'' || input.charAt(index) == '\"') {
                char quote = input.charAt(index);
                index++;
                StringBuilder sb = new StringBuilder();
                while (index < n && input.charAt(index) != quote) {
                    if (quote == '\"' && input.charAt(index) == '\\' && index + 1 < n && CHARACTERS_TO_PRESERVE.indexOf(input.charAt(index + 1)) != -1) {
                        index++;
                        sb.append(input.charAt(index));
                        index++;
                        continue;
                    }
                    sb.append(input.charAt(index));
                    index++;
                }
                if (index == n) {
                    output.add(quote + sb.toString());
                }
                output.add(sb.toString());
                index++;
            }
            else {
                StringBuilder sb = new StringBuilder();
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
                output.add(sb.toString());
            }
        }
        return output;
    }
    
    
    public static void main(String[] args) throws Exception {
        // Uncomment this block to pass the first stage

        HashMap<String, String> exeToDirectory = new HashMap<>();
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
                        exeToDirectory.put(file.getName(), dir);
                    }
                }
            }
        }

        while (true) {
            System.out.print("$ ");
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine().trim();

            ArrayList<String> tokens = tokenizeInputString(input);
            if (tokens.size() == 0) {
                System.out.println(input + ": command not found");
                continue;
            }
            if (tokens.get(0).equals("exit")) {
                scanner.close();
                try {
                    int index = input.indexOf(tokens.get(0)) + tokens.get(0).length();
                    System.exit(Integer.parseInt(input.substring(index).trim()));
                }
                catch (Exception e) {
                    System.exit(1);
                }
            }
            else if (tokens.get(0).equals("echo")) {
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
            else if (tokens.get(0).equals("type")) {
                int index = input.indexOf(tokens.get(0)) + tokens.get(0).length();
                String cmd = input.substring(index).trim();
                if (commands.contains(cmd)) {
                    System.out.println(cmd + " is a shell builtin");
                }
                else {
                    String dir = exeToDirectory.get(cmd);
                    if (dir != null) {
                        System.out.println(cmd + " is " + dir + "/" + cmd);
                    }
                    else {
                        System.out.println(cmd + ": not found");
                    }
                }
            }
            else if (tokens.get(0).equals("cat")) {
                if (tokens.size() == 1) {
                    System.out.print("cat: missing operand\n");
                    continue;
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < tokens.size(); i++) {
                    if (tokens.get(i).equals(" ")) {
                        continue;
                    }
                    Path path = Paths.get(tokens.get(i));
                    if (!Files.exists(path)) {
                        sb.append(tokens.get(i));
                    }
                    else {
                        Files.lines(path).forEach(sb::append);
                    }
                }
                System.out.println(sb.toString());
            }
            else {
                String dir = exeToDirectory.get(tokens.get(0));
                if (dir != null) {
                    ArrayList<String> cmds = new ArrayList<>();
                    cmds.add(tokens.get(0));
                    for (int i = 1; i < tokens.size(); i++) {
                        if (tokens.get(i).equals(" ")) {
                            continue;
                        }
                        cmds.add(tokens.get(i));
                    }
                    Process process = Runtime.getRuntime().exec(cmds.toArray(new String[0]));
                    process.getInputStream().transferTo(System.out);
                }
                else {
                    System.out.print(input + ": command not found\n");
                }
            }
        }
    }
}
