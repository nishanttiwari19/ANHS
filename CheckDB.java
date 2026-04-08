import java.sql.*;
import java.security.MessageDigest;

public class CheckDB {
    private static final String DB_URL = "jdbc:mysql://127.0.0.1:3306/adarsh_school_db?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "admin123";

    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            System.out.println("Connected to database.");
            
            System.out.println("\n--- admin_users ---");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM admin_users");
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id") + ", User: [" + rs.getString("username") + "], Hash: [" + rs.getString("password_hash") + "]");
            }

            System.out.println("\n--- sessions ---");
            rs = stmt.executeQuery("SELECT * FROM sessions");
            while (rs.next()) {
                System.out.println("Token: " + rs.getString("token") + ", User: " + rs.getString("username") + ", Expiry: " + rs.getTimestamp("expiry"));
            }
            
            System.out.println("\n--- Testing Nishant@2005 Hash ---");
            System.out.println("Expected: " + hash("Nishant@2005"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String hash(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) { return null; }
    }
}
