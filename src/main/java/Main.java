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
            String input = scanner.nextLine();
            
            String[] cmds = input.split(" ", 2);
            if (cmds.length == 0) {
                System.out.print(input + ": command not found\n");
                continue;
            }
            if (cmds[0].equals("exit")) {
                scanner.close();
                try {
                    System.exit(Integer.parseInt(cmds[1]));
                }
                catch (Exception e) {
                    System.exit(1);
                }
            }
            else if (cmds[0].equals("echo")) {
                if (cmds.length == 1) {
                    System.out.println();
                }
                else {
                    for (String s : tokenizeInputString(cmds[1])) {
                        System.out.print(s);
                    }
                    System.out.println();
                }
            }
            else if (cmds[0].equals("type")) {
                if (commands.contains(cmds[1])) {
                    System.out.println(cmds[1] + " is a shell builtin");
                }
                else {
                    String dir = exeToDirectory.get(cmds[1]);
                    if (dir != null) {
                        System.out.println(cmds[1] + " is " + dir + "/" + cmds[1]);
                    }
                    else {
                        System.out.println(cmds[1] + ": not found");
                    }
                }
            }
            else if (cmds[0].equals("cat")) {
                if (cmds.length == 1) {
                    System.out.print("cat: missing operand\n");
                    continue;
                }
                StringBuilder sb = new StringBuilder();
                for (String token : tokenizeInputString(cmds[1])) {
                    if (token.equals(" ")) {
                        continue;
                    }
                    Path path = Paths.get(token);
                    if (!Files.exists(path)) {
                        sb.append(token);
                    }
                    else {
                        Files.lines(path).forEach(sb::append);
                    }
                }
                System.out.println(sb.toString());
            }
            else {
                String dir = exeToDirectory.get(cmds[0]);
                if (dir != null) {
                    cmds = input.split(" ");
                    Process process = Runtime.getRuntime().exec(cmds);
                    process.getInputStream().transferTo(System.out);
                }
                else {
                    System.out.print(input + ": command not found\n");
                }
            }
        }
    }
}
