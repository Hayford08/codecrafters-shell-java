import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        // Uncomment this block to pass the first stage

        while (true) {
            System.out.print("$ ");
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine();
            String firstFour = input.substring(0, 4);
            if (firstFour.equals("exit")) {
                try {
                    System.exit(Integer.parseInt(firstFour.substring(5)));
                }
                catch (Exception e) {
                    System.exit(1);
                }
            }
            else if (firstFour.equals("echo")) {
                System.out.println(input.substring(5));
            }
            else {
                System.out.print(input + ": command not found\n");
            }
        }
    }
}
