import java.io.File;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;


public class Main {
    static Set<String> commands = Set.of("exit", "echo", "type");    
    public static void main(String[] args) throws Exception {
        // Uncomment this block to pass the first stage

        HashMap<String, String> exeToDirectory = new HashMap<>();
        String path = System.getenv("PATH");
        if (path != null) {
            for (String dir : path.split(":")) {
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
                try {
                    System.exit(Integer.parseInt(cmds[1]));
                }
                catch (Exception e) {
                    System.exit(1);
                }
            }
            else if (cmds[0].equals("echo")) {
                System.out.println(cmds[1]);
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
            else {
                System.out.print(input + ": command not found\n");
            }
        }
    }
}
