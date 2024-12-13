import java.sql.*;
import java.time.LocalDateTime;

public class DatabaseUtils {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/passmanager";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    public static boolean userNameExists(Connection connection, String userName) {
        String sql = "SELECT COUNT(*) FROM userinformation WHERE userName = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, userName);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println("Error Checking Username: " + e.getMessage());
        }
        return false;
    }

    public static boolean validateUserName(Connection connection, String userName) {
        return userNameExists(connection, userName);
    }

    public static void checkInfo(Connection connection, String userName) {
        String sql = "SELECT * FROM userinformation WHERE userName = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, userName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println(userName + "'s Information:");
                    System.out.println("User ID: " + rs.getInt("userId"));
                    System.out.println("User Name: " + rs.getString("userName"));
                    System.out.println("Password: " + rs.getString("password"));
                    System.out.println("Last Updated: " + rs.getTimestamp("updateTime"));
                } else {
                    System.out.println("No information found for user: " + userName);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error Fetching Information: " + e.getMessage());
        }
    }

    public static void updateField(Connection connection, String oldUserName, String newUserName, String newPassword) {
        try {
            // Fetch the current information for history tracking
            String fetchSql = "SELECT userName, password FROM userinformation WHERE userName = ?";
            try (PreparedStatement fetchStmt = connection.prepareStatement(fetchSql)) {
                fetchStmt.setString(1, oldUserName);
                ResultSet rs = fetchStmt.executeQuery();

                if (rs.next()) {
                    String oldPassword = rs.getString("password");
                    String currentUserName = rs.getString("userName");

                    // Determine the new values
                    String updatedUserName = (newUserName != null) ? newUserName : currentUserName;
                    String updatedPassword = (newPassword != null) ? newPassword : oldPassword;

                    // Update the database
                    String updateSql = "UPDATE userinformation SET userName = ?, password = ?, updateTime = ? WHERE userName = ?";
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                        updateStmt.setString(1, updatedUserName);
                        updateStmt.setString(2, updatedPassword);
                        updateStmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                        updateStmt.setString(4, oldUserName);

                        int rowsAffected = updateStmt.executeUpdate();
                        if (rowsAffected > 0) {
                            System.out.println("Information updated successfully!");

                            // Save update history
                            saveUpdateHistory(connection, currentUserName, updatedUserName, oldPassword, updatedPassword);
                        } else {
                            System.out.println("Failed to update information. Please try again.");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error updating information: " + e.getMessage());
        }
    }

    private static void saveUpdateHistory(Connection connection, String oldName, String newName, String oldPassword, String newPassword) {
        try {
            String insertHistorySql = "INSERT INTO updatedinformation (oldName, newName, oldPassword, newPassword, updateTime) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement historyStmt = connection.prepareStatement(insertHistorySql)) {
                historyStmt.setString(1, oldName);
                historyStmt.setString(2, (newName != null && !newName.equals(oldName)) ? newName : "UNCHANGED");
                historyStmt.setString(3, oldPassword);
                historyStmt.setString(4, (newPassword != null && !newPassword.equals(oldPassword)) ? newPassword : "UNCHANGED");
                historyStmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));

                historyStmt.executeUpdate();
                System.out.println("Update history saved successfully.");
            }
        } catch (SQLException e) {
            System.out.println("Error saving update history: " + e.getMessage());
        }
    }


    public static boolean deleteAccount(Connection connection, String userName) {
        String sql = "DELETE FROM userinformation WHERE userName = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, userName);
            if (stmt.executeUpdate() > 0) {
                System.out.println("Account Deleted Successfully.");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Error Deleting Account: " + e.getMessage());
        }
        return false;
    }
}
