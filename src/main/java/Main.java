import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        // Uncomment this block to pass the first stage

        while (true) {
            System.out.print("$ ");
            try (Scanner scanner = new Scanner(System.in)) {
                String input = scanner.nextLine();

                if (input.equals("")) {
                    break;
                }
    
                System.out.print(input + ": command not found\n");
            }
        }
    }
}
