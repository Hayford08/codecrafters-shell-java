import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        // Uncomment this block to pass the first stage

        while (true) {
            System.out.print("$ ");
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine();
            if (input.substring(0, 4).equals("exit")) {
                try {
                    int exitCode = Integer.parseInt(input.substring(5));
                    System.exit(exitCode);
                }
                catch (Exception e) {
                    System.exit(1);
                }
            }
            System.out.print(input + ": command not found\n");
        }
    }
}
