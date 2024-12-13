import java.sql.*;
import java.time.LocalDateTime;
import java.util.Scanner;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private static Connection connection;

    public static void main(String[] args) {
        try {
            connection = DatabaseUtils.getConnection();
            mainMenu();
        } catch (SQLException e) {
            System.out.println("Database Connection Error: " + e.getMessage());
        }
    }

    private static void mainMenu() {
        while (true) {
            System.out.println("Password Manager 2.5.0");
            System.out.println("-----------------------");
            System.out.println("1. Sign Up");
            System.out.println("2. Sign In (Username)");
            System.out.println("0. Exit Program");
            System.out.print("Choose An Option: ");

            int choice = getUserChoice();

            switch (choice) {
                case 1 -> signUp();
                case 2 -> signIn();
                case 0 -> exit();
                default -> System.out.println("Invalid Option! Please try again.");
            }
        }
    }

    private static void signUp() {
        String userName;
        while (true) {
            System.out.print("Enter a Username: ");
            userName = scanner.nextLine();

            if (!InputValidator.isValid(userName, "Username cannot be empty!")
                    || DatabaseUtils.userNameExists(connection, userName)) {
                System.out.println("Username is already taken or invalid. Try another.");
            } else {
                break;
            }
        }

        System.out.print("Enter Password: ");
        String password = scanner.nextLine();

        try {
            String sql = "INSERT INTO userinformation (userName, password, updateTime) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, userName);
                stmt.setString(2, password);
                stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                stmt.executeUpdate();
            }
            System.out.println("Sign Up Complete!");
        } catch (SQLException e) {
            System.out.println("Error During Sign-Up: " + e.getMessage());
        }
    }

    private static void signIn() {
        System.out.print("Enter Your Username: ");
        String userName = scanner.nextLine();

        if (DatabaseUtils.validateUserName(connection, userName)) {
            System.out.println("Login Successful! Welcome, " + userName);
            userMenu(userName);
        } else {
            System.out.println("Login Failed! Username not found!");
        }
    }

    private static void userMenu(String userName) {
        while (true) {
            System.out.println("\nUser Menu");
            System.out.println("1. Check Information");
            System.out.println("2. Update Information");
            System.out.println("3. Delete Account");
            System.out.println("4. Download Information");
            System.out.println("0. Logout");
            System.out.print("Choose Option: ");
            int choice = getUserChoice();
            System.out.println(" ");

            switch (choice) {
                case 1 -> DatabaseUtils.checkInfo(connection, userName);
                case 2 -> updateInfo(userName);
                case 3 -> {
                    if (DatabaseUtils.deleteAccount(connection, userName)) return;
                }
                case 4 -> downloadInformation(userName);
                case 0 -> {
                    return;
                }
                default -> System.out.println("Invalid Option!");
            }
        }
    }


    private static void updateInfo(String userName) {
        while (true) {
            System.out.println("\nUpdate Information");
            System.out.println("1. Update Username");
            System.out.println("2. Update Password");
            System.out.println("0. Back to User Menu");
            System.out.print("Choose an Option: ");

            int choice = getUserChoice();

            switch (choice) {
                case 1 -> {
                    System.out.print("Enter New Username: ");
                    String newUserName = scanner.nextLine();

                    if (!InputValidator.isValid(newUserName, "Username cannot be empty!")
                            || DatabaseUtils.userNameExists(connection, newUserName)) {
                        System.out.println("Invalid or existing username. Try again.");
                    } else {
                        DatabaseUtils.updateField(connection, userName, newUserName, null); // Update only username
                        return;
                    }
                }
                case 2 -> {
                    System.out.print("Enter New Password: ");
                    String newPassword = scanner.nextLine();

                    if (InputValidator.isValid(newPassword, "Password cannot be empty!")) {
                        DatabaseUtils.updateField(connection, userName, null, newPassword); // Update only password
                        return;
                    }
                }
                case 0 -> {
                    return; // Return to user menu
                }
                default -> System.out.println("Invalid Option! Please try again.");
            }
        }
    }


    private static void downloadInformation(String userName) {
        String sql = "SELECT * FROM userinformation WHERE userName = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, userName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String userId = String.valueOf(rs.getInt("userId"));
                    String password = rs.getString("password");
                    String updateTime = String.valueOf(rs.getTimestamp("updateTime"));

                    // Create the content to write to the file
                    String userInfo = """
                        User Information:
                        -----------------
                        User ID: %s
                        Username: %s
                        Password: %s
                        Last Updated: %s
                        """.formatted(userId, userName, password, updateTime);

                    // Define the file name (use the username for a custom file name)
                    String fileName = userName + "_info.txt";

                    // Write the content to the file
                    try (java.io.FileWriter writer = new java.io.FileWriter(fileName)) {
                        writer.write(userInfo);

                        // Notify the user where the file is saved
                        String absolutePath = new java.io.File(fileName).getAbsolutePath();
                        System.out.println("User information saved successfully!");
                        System.out.println("File saved at: " + absolutePath);
                    } catch (java.io.IOException e) {
                        System.out.println("Error writing to file: " + e.getMessage());
                    }
                } else {
                    System.out.println("No information found for user: " + userName);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error Fetching Information: " + e.getMessage());
        }
    }



    private static void exit() {
        System.out.println("Thanks for using Password Manager!");
        System.exit(0);
    }

    private static int getUserChoice() {
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a number.");
            return -1;
        }
    }
}
