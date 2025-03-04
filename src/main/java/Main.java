import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        // Uncomment this block to pass the first stage

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
            else {
                System.out.print(input + ": command not found\n");
            }
        }
    }
}
