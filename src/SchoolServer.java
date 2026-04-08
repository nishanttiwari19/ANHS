import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.sql.*;
import java.util.Properties;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class SchoolServer {

    private static final int DEFAULT_PORT = 8080;
    private static final String PORT_ENV = System.getenv("PORT");
    private static final int PORT = (PORT_ENV != null) ? Integer.parseInt(PORT_ENV) : DEFAULT_PORT;
    private static final String STATIC_DIR = "src/main/resources/static";
    private static final String GALLERY_DIR = "src/main/resources/static/img/gallery";
    private static final String FACULTY_DIR = "src/main/resources/static/img/faculty";
    private static final String DOWNLOADS_DIR = "src/main/resources/static/downloads";
    private static final String BOOKS_DIR = "src/main/resources/static/books";
    
    // Database configuration - Using environment variables for Cloud Deployment
    private static final String DB_URL = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://127.0.0.1:3306/adarsh_school_db?useSSL=false&allowPublicKeyRetrieval=true");
    private static final String DB_USER = System.getenv().getOrDefault("DB_USER", "root");
    private static final String DB_PASS = System.getenv().getOrDefault("DB_PASS", "admin123");

    public static void main(String[] args) throws IOException {
        testDatabase();
        new File(GALLERY_DIR).mkdirs();
        new File(FACULTY_DIR).mkdirs();
        new File(DOWNLOADS_DIR).mkdirs();
        new File(BOOKS_DIR).mkdirs();
        
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
        
        // Static File Handler
        server.createContext("/", new StaticHandler());
        
        // API Handlers
        server.createContext("/api/notifications", new ApiHandler());
        server.createContext("/api/applications", new ApplicationHandler());
        server.createContext("/api/careers", new CareerHandler());
        server.createContext("/api/gallery", new GalleryHandler());
        server.createContext("/api/principal-message", new PrincipalHandler());
        server.createContext("/api/testimonials", new TestimonialHandler());
        server.createContext("/api/vision-mission", new VisionHandler());
        server.createContext("/api/faculty", new FacultyHandler());
        server.createContext("/api/students", new StudentHandler());
        server.createContext("/api/results", new ResultHandler());
        server.createContext("/api/settings", new SettingsHandler());
        server.createContext("/api/library", new LibraryHandler());
        server.createContext("/api/enquiries", new EnquiryHandler());
        server.createContext("/api/login", new AuthHandler());
        
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        System.out.println(">>> School Website Server started at http://localhost:" + PORT);
        server.start();
    }

    private static void testDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = getConnection()) {
                System.out.println(">>> Connected to MySQL Successfully!");
                // Auto-create students table if missing
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("CREATE TABLE IF NOT EXISTS students (" +
                        "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                        "name VARCHAR(255) NOT NULL, " +
                        "father_name VARCHAR(255), " +
                        "student_class VARCHAR(50), " +
                        "roll_no VARCHAR(50), " +
                        "dob DATE, " +
                        "blood_group VARCHAR(10), " +
                        "photo VARCHAR(255), " +
                        "address TEXT, " +
                        "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
                    
                stmt.execute("CREATE TABLE IF NOT EXISTS results (" +
                        "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                        "roll_no VARCHAR(50) UNIQUE NOT NULL, " +
                        "student_name VARCHAR(255) NOT NULL, " +
                        "student_class VARCHAR(50), " +
                        "subjects_json JSON, " +
                        "total_marks INT, " +
                        "obtained_marks INT, " +
                        "percentage DECIMAL(5,2), " +
                        "grade VARCHAR(10), " +
                        "status VARCHAR(20), " +
                        "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

                stmt.execute("CREATE TABLE IF NOT EXISTS school_settings (" +
                        "id INT PRIMARY KEY DEFAULT 1, email VARCHAR(255), phone VARCHAR(100), address TEXT, maps_link TEXT)");
                stmt.execute("INSERT IGNORE INTO school_settings (id, email, phone, address, maps_link) " +
                        "VALUES (1, 'contact@anhs.com', '+91 8989824557', 'Purwa No 4 313, Gangeo, Rewa, MP 486111', 'https://www.google.com/maps/search/?api=1&query=PHGW%2BCM6,+Purwa+No+4+313,+Madhya+Pradesh+486111')");

                stmt.execute("CREATE TABLE IF NOT EXISTS principal_profile (" +
                        "id INT PRIMARY KEY DEFAULT 1, name VARCHAR(255), quote TEXT, message TEXT, photo VARCHAR(255))");
                
                // Add new columns for Seal and Signature if they don't exist
                try { stmt.execute("ALTER TABLE principal_profile ADD COLUMN seal_photo VARCHAR(255)"); } catch(Exception e){}
                try { stmt.execute("ALTER TABLE principal_profile ADD COLUMN signature_photo VARCHAR(255)"); } catch(Exception e){}
                
                stmt.execute("INSERT IGNORE INTO principal_profile (id, name, quote, message, photo) VALUES (1, 'Principal Name', 'Quote...', 'Msg...', 'default_principal.jpg')");
                
                // Add books table if missing (auto-create)
                stmt.execute("CREATE TABLE IF NOT EXISTS books (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                    "title VARCHAR(255) NOT NULL, " +
                    "author VARCHAR(255), " +
                    "pdf_path VARCHAR(255), " +
                    "upload_date DATETIME DEFAULT CURRENT_TIMESTAMP)");
                
                // Add enquiries table if missing
                stmt.execute("CREATE TABLE IF NOT EXISTS enquiries (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                    "name VARCHAR(255) NOT NULL, " +
                    "email VARCHAR(255), " +
                    "mobile VARCHAR(15), " +
                    "subject VARCHAR(255), " +
                    "message TEXT, " +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");

                // Add admin_users and sessions table
                stmt.execute("CREATE TABLE IF NOT EXISTS admin_users (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "username VARCHAR(100) UNIQUE NOT NULL, " +
                    "password_hash VARCHAR(255) NOT NULL, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
                
                stmt.execute("CREATE TABLE IF NOT EXISTS sessions (" +
                    "token VARCHAR(100) PRIMARY KEY, " +
                    "username VARCHAR(100) NOT NULL, " +
                    "expiry DATETIME NOT NULL)");

                // Seed initial admin if missing
                String seedUser = "Nishant";
                String seedPass = "Nishant@2005";
                String seedHash = hash(seedPass);
                
                // Force update password for the primary admin
                stmt.execute("INSERT INTO admin_users (username, password_hash) VALUES ('" + seedUser + "', '" + seedHash + "') " +
                             "ON DUPLICATE KEY UPDATE password_hash='" + seedHash + "'");
                
                // Ensure the hash is correct by running an explicit update if insert didn't do it
                try (PreparedStatement upstmt = conn.prepareStatement("UPDATE admin_users SET password_hash = ? WHERE username = ?")) {
                    upstmt.setString(1, seedHash);
                    upstmt.setString(2, seedUser);
                    upstmt.executeUpdate();
                }

                // Cleanup old admin if it exists
                stmt.execute("DELETE FROM admin_users WHERE username='admin'");

                // MASTER DIAGNOSTIC: List all users in DB
                System.out.println("---------- [DATABASE AUTH STATE] ----------");
                try (ResultSet rsUsers = stmt.executeQuery("SELECT username, password_hash FROM admin_users")) {
                    while (rsUsers.next()) {
                        System.out.println("USER: [" + rsUsers.getString("username") + "] HASH: [" + rsUsers.getString("password_hash") + "]");
                    }
                }
                System.out.println("-------------------------------------------");
                }
            }
        } catch (SQLException se) {
            System.err.println("!!! MySQL Error Code: " + se.getErrorCode());
            System.err.println("!!! MySQL Message: " + se.getMessage());
            if (se.getErrorCode() == 1049) {
                System.err.println("!!! ERROR: Database 'adarsh_school_db' does not exist. Please run database_setup.sql first.");
            } else if (se.getErrorCode() == 1045) {
                System.err.println("!!! ERROR: Wrong Username or Password.");
            }
        } catch (Exception e) {
            System.err.println("!!! Error: " + e.getMessage());
        }
    }

    // --- Authentication Helpers ---
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
        } catch (NoSuchAlgorithmException e) { e.printStackTrace(); return null; }
    }

    private static boolean isAdminSession(HttpExchange exchange) {
        String token = exchange.getRequestHeaders().getFirst("X-Session-Token");
        if (token == null) return false;
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM sessions WHERE token = ? AND expiry > NOW()")) {
            pstmt.setString(1, token);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Extend session by 2 hours
                    try (PreparedStatement extend = conn.prepareStatement("UPDATE sessions SET expiry = DATE_ADD(NOW(), INTERVAL 2 HOUR) WHERE token = ?")) {
                        extend.setString(1, token);
                        extend.executeUpdate();
                    }
                    return true;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    static class AuthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                InputStream is = exchange.getRequestBody();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[16384];
                while ((nRead = is.read(data, 0, data.length)) != -1) buffer.write(data, 0, nRead);
                String body = buffer.toString("UTF-8");

                String user = extract(body, "username").trim();
                String pass = extract(body, "password");
                String hashedPass = hash(pass);

                System.out.println(">>> [AUTH DEBUG] Body: " + body);
                System.out.println(">>> [AUTH DEBUG] User: '" + user + "'");
                System.out.println(">>> [AUTH DEBUG] Hash: " + hashedPass);

                try (Connection conn = getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM admin_users WHERE LOWER(username) = LOWER(?) AND password_hash = ?")) {
                    pstmt.setString(1, user);
                    pstmt.setString(2, hashedPass);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            System.out.println(">>> [AUTH SUCCESS] Logged in as: " + rs.getString("username"));
                            String token = UUID.randomUUID().toString();
                            try (PreparedStatement sessionPstmt = conn.prepareStatement("INSERT INTO sessions (token, username, expiry) VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 2 HOUR))")) {
                                sessionPstmt.setString(1, token);
                                sessionPstmt.setString(2, rs.getString("username"));
                                sessionPstmt.executeUpdate();
                            }
                            String json = "{\"success\":true, \"token\":\"" + token + "\"}";
                            byte[] bytes = json.getBytes();
                            exchange.sendResponseHeaders(200, bytes.length);
                            exchange.getResponseBody().write(bytes);
                        } else {
                            System.out.println(">>> [AUTH FAILED] No match found in database for user '" + user + "' with that hash.");
                            String json = "{\"success\":false, \"message\":\"Invalid credentials\"}";
                            byte[] bytes = json.getBytes();
                            exchange.sendResponseHeaders(401, bytes.length);
                            exchange.getResponseBody().write(bytes);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(500, -1);
                }
            }
            exchange.getResponseBody().close();
        }
        
        private String extract(String json, String key) {
            try {
                String search = "\"" + key + "\"";
                int startPos = json.indexOf(search);
                if (startPos == -1) return "";
                
                int colonPos = json.indexOf(":", startPos);
                if (colonPos == -1) return "";
                
                int quoteStart = json.indexOf("\"", colonPos);
                if (quoteStart == -1) return "";
                
                int quoteEnd = json.indexOf("\"", quoteStart + 1);
                if (quoteEnd == -1) return "";
                
                return json.substring(quoteStart + 1, quoteEnd);
            } catch (Exception e) {
                return "";
            }
        }
    }

    private static Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        } catch (SQLException e) {
            if (e.getErrorCode() == 1045) { // Access Denied
                // Try fallback to empty password
                return DriverManager.getConnection(DB_URL, DB_USER, "");
            }
            throw e;
        }
    }

    static class FacultyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if ((method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("DELETE")) && !isAdminSession(exchange)) {
                exchange.sendResponseHeaders(401, -1); exchange.close(); return;
            }
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if (method.equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            if (method.equalsIgnoreCase("GET")) {
                // ... Existing GET logic ...
                List<String> list = new ArrayList<>();
                try (Connection conn = getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT * FROM faculty")) {
                    while (rs.next()) {
                        list.add(String.format("{\"id\":\"%s\", \"name\":\"%s\", \"photo\":\"%s\"}", 
                            rs.getString("id"), rs.getString("name"), rs.getString("photo")));
                    }
                } catch (Exception e) { e.printStackTrace(); }
                String json = "[" + String.join(",", list) + "]";
                byte[] bytes = json.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            } 
            else if (method.equalsIgnoreCase("POST")) {
                if (!isAdminSession(exchange)) { exchange.sendResponseHeaders(401, -1); exchange.close(); return; }
                InputStream is = exchange.getRequestBody();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[16384];
                while ((nRead = is.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                String body = buffer.toString("UTF-8");

                try {
                    String name = extract(body, "name");
                    String base64Data = extract(body, "photo");
                    if (base64Data.contains(",")) base64Data = base64Data.split(",")[1];
                    
                    String fileName = System.currentTimeMillis() + "_faculty.jpg";
                    byte[] imageData = java.util.Base64.getDecoder().decode(base64Data.trim());
                    Files.write(Paths.get(FACULTY_DIR, fileName), imageData);
                    
                    try (Connection conn = getConnection();
                         PreparedStatement pstmt = conn.prepareStatement("INSERT INTO faculty (name, photo) VALUES (?, ?)")) {
                        pstmt.setString(1, name);
                        pstmt.setString(2, fileName);
                        pstmt.executeUpdate();
                    }
                    exchange.sendResponseHeaders(201, 0);
                } catch (Exception e) { e.printStackTrace(); exchange.sendResponseHeaders(400, -1); }
            }
            else if (method.equalsIgnoreCase("DELETE")) {
                String path = exchange.getRequestURI().getPath();
                String id = path.substring(path.lastIndexOf('/') + 1);
                try (Connection conn = getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("DELETE FROM faculty WHERE id = ?")) {
                    pstmt.setString(1, id);
                    pstmt.executeUpdate();
                } catch (Exception e) { e.printStackTrace(); }
                exchange.sendResponseHeaders(204, -1);
            }
            exchange.getResponseBody().close();
        }

        private String extract(String json, String key) {
            String pattern = "\"" + key + "\":\"";
            int start = json.indexOf(pattern);
            if (start == -1) return "";
            start += pattern.length();
            int end = json.indexOf("\"", start);
            if (end == -1) return "";
            return json.substring(start, end);
        }
    }

    static class VisionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if (method.equalsIgnoreCase("POST") && !isAdminSession(exchange)) {
                exchange.sendResponseHeaders(401, -1); exchange.close(); return;
            }
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if (method.equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            if (method.equalsIgnoreCase("GET")) {
                String content = "";
                try (Connection conn = getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT content FROM vision_mission LIMIT 1")) {
                    if (rs.next()) content = rs.getString("content");
                } catch (Exception e) { e.printStackTrace(); }
                
                String json = "{\"content\":\"" + content.replace("\"", "\\\"").replace("\n", "\\n") + "\"}";
                byte[] bytes = json.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            } 
            else if (method.equalsIgnoreCase("POST")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
                String body = reader.lines().collect(Collectors.joining());
                
                String pattern = "\"content\":\"";
                int start = body.indexOf(pattern);
                if (start != -1) {
                    start += pattern.length();
                    int end = body.lastIndexOf("\"");
                    if (end > start) {
                        String msg = body.substring(start, end);
                        try (Connection conn = getConnection();
                             PreparedStatement pstmt = conn.prepareStatement("UPDATE vision_mission SET content = ? LIMIT 1")) {
                            pstmt.setString(1, msg);
                            pstmt.executeUpdate();
                        } catch (Exception e) { e.printStackTrace(); }
                        exchange.sendResponseHeaders(200, 0);
                    } else { exchange.sendResponseHeaders(400, -1); }
                } else { exchange.sendResponseHeaders(400, -1); }
            }
            exchange.getResponseBody().close();
        }
    }

    static class TestimonialHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if ((method.equalsIgnoreCase("PUT") || method.equalsIgnoreCase("DELETE")) && !isAdminSession(exchange)) {
                exchange.sendResponseHeaders(401, -1); exchange.close(); return;
            }
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if (method.equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            if (method.equalsIgnoreCase("GET")) {
                List<String> list = new ArrayList<>();
                try (Connection conn = getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT * FROM testimonials")) {
                    while (rs.next()) {
                        list.add(String.format("{\"id\":\"%s\", \"name\":\"%s\", \"relation\":\"%s\", \"message\":\"%s\"}", 
                            rs.getString("id"), rs.getString("name"), rs.getString("relation"), 
                            rs.getString("message").replace("\"", "\\\"").replace("\n", "\\n")));
                    }
                } catch (Exception e) { e.printStackTrace(); }
                String json = "[" + String.join(",", list) + "]";
                byte[] bytes = json.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            } 
            else if (method.equalsIgnoreCase("POST")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
                String body = reader.lines().collect(Collectors.joining());
                
                try {
                    String name = extract(body, "name");
                    String relation = extract(body, "relation");
                    String message = extract(body, "message");
                    
                    try (Connection conn = getConnection();
                         PreparedStatement pstmt = conn.prepareStatement("INSERT INTO testimonials (name, relation, message) VALUES (?, ?, ?)")) {
                        pstmt.setString(1, name);
                        pstmt.setString(2, relation);
                        pstmt.setString(3, message);
                        pstmt.executeUpdate();
                    }
                    exchange.sendResponseHeaders(201, 0);
                } catch (Exception e) { e.printStackTrace(); exchange.sendResponseHeaders(400, -1); }
            }
            else if (method.equalsIgnoreCase("DELETE")) {
                String path = exchange.getRequestURI().getPath();
                String id = path.substring(path.lastIndexOf('/') + 1);
                try (Connection conn = getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("DELETE FROM testimonials WHERE id = ?")) {
                    pstmt.setString(1, id);
                    pstmt.executeUpdate();
                } catch (Exception e) { e.printStackTrace(); }
                exchange.sendResponseHeaders(204, -1);
            }
            exchange.getResponseBody().close();
        }
        private String extract(String json, String key) {
            String pattern = "\"" + key + "\":\"";
            int start = json.indexOf(pattern);
            if (start == -1) return "";
            start += pattern.length();
            int end = json.indexOf("\"", start);
            if (end == -1) return "";
            return json.substring(start, end);
        }
    }

    static class SettingsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if (method.equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            if (method.equalsIgnoreCase("GET")) {
                try (Connection conn = getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT * FROM school_settings WHERE id = 1")) {
                    if (rs.next()) {
                        String json = String.format("{\"email\":\"%s\", \"phone\":\"%s\", \"address\":\"%s\", \"maps_link\":\"%s\"}",
                                rs.getString("email"), rs.getString("phone"), 
                                rs.getString("address").replace("\n", "\\n"),
                                rs.getString("maps_link"));
                        byte[] bytes = json.getBytes();
                        exchange.sendResponseHeaders(200, bytes.length);
                        exchange.getResponseBody().write(bytes);
                    } else {
                        exchange.sendResponseHeaders(404, -1);
                    }
                } catch (Exception e) { e.printStackTrace(); exchange.sendResponseHeaders(500, -1); }
            } else if (method.equalsIgnoreCase("POST")) {
                if (!isAdminSession(exchange)) { exchange.sendResponseHeaders(401, -1); exchange.close(); return; }
                InputStream is = exchange.getRequestBody();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[1024];
                while ((nRead = is.read(data, 0, data.length)) != -1) buffer.write(data, 0, nRead);
                String body = buffer.toString("UTF-8");

                try {
                    String email = extract(body, "email");
                    String phone = extract(body, "phone");
                    String address = extract(body, "address");
                    String mapsLink = extract(body, "maps_link");

                    try (Connection conn = getConnection();
                         PreparedStatement pstmt = conn.prepareStatement("UPDATE school_settings SET email=?, phone=?, address=?, maps_link=? WHERE id=1")) {
                        pstmt.setString(1, email);
                        pstmt.setString(2, phone);
                        pstmt.setString(3, address);
                        pstmt.setString(4, mapsLink);
                        pstmt.executeUpdate();
                    }
                    exchange.sendResponseHeaders(200, 0);
                } catch (Exception e) { e.printStackTrace(); exchange.sendResponseHeaders(400, -1); }
            }
            exchange.getResponseBody().close();
        }

        private String extract(String json, String key) {
            String pattern = "\"" + key + "\":\"";
            int start = json.indexOf(pattern);
            if (start == -1) return "";
            start += pattern.length();
            int end = json.indexOf("\"", start);
            if (end == -1) return "";
            return json.substring(start, end).replace("\\n", "\n").replace("\\\"", "\"");
        }
    }

    static class PrincipalHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if (method.equalsIgnoreCase("POST") && !isAdminSession(exchange)) {
                exchange.sendResponseHeaders(401, -1); exchange.close(); return;
            }
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if (method.equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            if (method.equalsIgnoreCase("GET")) {
                String json = "{}";
                try (Connection conn = getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT * FROM principal_profile WHERE id = 1")) {
                    if (rs.next()) {
                        json = String.format("{\"name\":\"%s\", \"quote\":\"%s\", \"message\":\"%s\", \"photo\":\"%s\", \"seal\":\"%s\", \"signature\":\"%s\"}",
                            rs.getString("name"), 
                            rs.getString("quote").replace("\"", "\\\"").replace("\n", "\\n"),
                            rs.getString("message").replace("\"", "\\\"").replace("\n", "\\n"),
                            rs.getString("photo"),
                            rs.getString("seal_photo") == null ? "" : rs.getString("seal_photo"),
                            rs.getString("signature_photo") == null ? "" : rs.getString("signature_photo"));
                    }
                } catch (Exception e) { e.printStackTrace(); }
                byte[] bytes = json.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            } 
            else if (method.equalsIgnoreCase("POST")) {
                InputStream is = exchange.getRequestBody();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[16384];
                while ((nRead = is.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                String body = buffer.toString("UTF-8");

                try {
                    String name = extract(body, "name");
                    String quote = extract(body, "quote");
                    String message = extract(body, "message");
                    String photoBase64 = extract(body, "photo");
                    String sealBase64 = extract(body, "seal");
                    String signBase64 = extract(body, "signature");

                    String photoFile = "";
                    String sealFile = "";
                    String signFile = "";

                    // Fetch current values first
                    try (Connection conn = getConnection();
                         PreparedStatement pstmt = conn.prepareStatement("SELECT photo, seal_photo, signature_photo FROM principal_profile WHERE id = 1")) {
                        ResultSet rs = pstmt.executeQuery();
                        if (rs.next()) {
                            photoFile = rs.getString("photo");
                            sealFile = rs.getString("seal_photo");
                            signFile = rs.getString("signature_photo");
                        }
                    }

                    if (photoBase64.startsWith("data:image")) {
                        if (photoBase64.contains(",")) photoBase64 = photoBase64.split(",")[1];
                        photoFile = "principal_" + System.currentTimeMillis() + ".jpg";
                        Files.write(Paths.get(FACULTY_DIR, photoFile), java.util.Base64.getDecoder().decode(photoBase64.trim()));
                    }
                    if (sealBase64.startsWith("data:image")) {
                        if (sealBase64.contains(",")) sealBase64 = sealBase64.split(",")[1];
                        sealFile = "seal_" + System.currentTimeMillis() + ".png";
                        Files.write(Paths.get(FACULTY_DIR, sealFile), java.util.Base64.getDecoder().decode(sealBase64.trim()));
                    }
                    if (signBase64.startsWith("data:image")) {
                        if (signBase64.contains(",")) signBase64 = signBase64.split(",")[1];
                        signFile = "sign_" + System.currentTimeMillis() + ".png";
                        Files.write(Paths.get(FACULTY_DIR, signFile), java.util.Base64.getDecoder().decode(signBase64.trim()));
                    }
                    
                    try (Connection conn = getConnection();
                         PreparedStatement pstmt = conn.prepareStatement("UPDATE principal_profile SET name=?, quote=?, message=?, photo=?, seal_photo=?, signature_photo=? WHERE id=1")) {
                        pstmt.setString(1, name);
                        pstmt.setString(2, quote);
                        pstmt.setString(3, message);
                        pstmt.setString(4, photoFile);
                        pstmt.setString(5, sealFile);
                        pstmt.setString(6, signFile);
                        pstmt.executeUpdate();
                    }
                    exchange.sendResponseHeaders(200, 0);
                } catch (Exception e) { e.printStackTrace(); exchange.sendResponseHeaders(400, -1); }
            }
            exchange.getResponseBody().close();
        }

        private String extract(String json, String key) {
            String pattern = "\"" + key + "\":\"";
            int start = json.indexOf(pattern);
            if (start == -1) return "";
            start += pattern.length();
            int end = json.indexOf("\"", start);
            if (end == -1) return "";
            return json.substring(start, end);
        }
    }

    static class CareerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if (method.equalsIgnoreCase("GET") && !isAdminSession(exchange)) {
                exchange.sendResponseHeaders(401, -1); exchange.close(); return;
            }
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if (method.equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            if (method.equalsIgnoreCase("POST")) {
                BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), "utf-8"));
                String body = br.lines().collect(Collectors.joining());
                
                try {
                    String name = extract(body, "name");
                    String profile = extract(body, "profile");
                    String mobile = extract(body, "mobile");
                    
                    try (Connection conn = getConnection();
                         PreparedStatement pstmt = conn.prepareStatement("INSERT INTO careers (name, profile, mobile, data_json) VALUES (?, ?, ?, ?)")) {
                        pstmt.setString(1, name);
                        pstmt.setString(2, profile);
                        pstmt.setString(3, mobile);
                        pstmt.setString(4, body);
                        pstmt.executeUpdate();
                    }
                    String response = "{\"status\":\"success\"}";
                    exchange.sendResponseHeaders(201, response.length());
                    exchange.getResponseBody().write(response.getBytes());
                } catch (Exception e) { e.printStackTrace(); exchange.sendResponseHeaders(400, -1); }
            } 
            else if (method.equalsIgnoreCase("GET")) {
                List<String> list = new ArrayList<>();
                try (Connection conn = getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT data_json FROM careers ORDER BY timestamp DESC")) {
                    while (rs.next()) {
                        list.add(rs.getString("data_json"));
                    }
                } catch (Exception e) { e.printStackTrace(); }
                String json = "[" + String.join(",", list) + "]";
                byte[] bytes = json.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
            exchange.getResponseBody().close();
        }
        private String extract(String json, String key) {
            String pattern = "\"" + key + "\":\"";
            int start = json.indexOf(pattern);
            if (start == -1) return "";
            start += pattern.length();
            int end = json.indexOf("\"", start);
            if (end == -1) return "";
            return json.substring(start, end);
        }
    }


    static class LibraryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if ((method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("DELETE")) && !isAdminSession(exchange)) {
                exchange.sendResponseHeaders(401, -1); exchange.close(); return;
            }
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if (method.equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            if (method.equalsIgnoreCase("GET")) {
                List<String> list = new ArrayList<>();
                try (Connection conn = getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT * FROM books ORDER BY upload_date DESC")) {
                    while (rs.next()) {
                        list.add(String.format("{\"id\":%d, \"title\":\"%s\", \"author\":\"%s\", \"pdf_path\":\"%s\", \"date\":\"%s\"}",
                            rs.getLong("id"), rs.getString("title"), rs.getString("author"), 
                            rs.getString("pdf_path"), rs.getString("upload_date")));
                    }
                } catch (Exception e) { e.printStackTrace(); }
                String json = "[" + String.join(",", list) + "]";
                byte[] bytes = json.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            } 
            else if (method.equalsIgnoreCase("POST")) {
                InputStream is = exchange.getRequestBody();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[16384];
                while ((nRead = is.read(data, 0, data.length)) != -1) buffer.write(data, 0, nRead);
                String body = buffer.toString("UTF-8");

                try {
                    String title = extract(body, "title");
                    String author = extract(body, "author");
                    String base64Data = extract(body, "data");
                    if (base64Data.contains(",")) base64Data = base64Data.split(",")[1];
                    
                    String fileName = System.currentTimeMillis() + "_book.pdf";
                    byte[] pdfData = java.util.Base64.getDecoder().decode(base64Data.trim());
                    Files.write(Paths.get(BOOKS_DIR, fileName), pdfData);
                    
                    try (Connection conn = getConnection();
                         PreparedStatement pstmt = conn.prepareStatement("INSERT INTO books (title, author, pdf_path) VALUES (?, ?, ?)")) {
                        pstmt.setString(1, title);
                        pstmt.setString(2, author);
                        pstmt.setString(3, fileName);
                        pstmt.executeUpdate();
                    }
                    exchange.sendResponseHeaders(201, 0);
                } catch (Exception e) { e.printStackTrace(); exchange.sendResponseHeaders(400, -1); }
            }
            else if (method.equalsIgnoreCase("DELETE")) {
                String path = exchange.getRequestURI().getPath();
                String id = path.substring(path.lastIndexOf('/') + 1);
                try (Connection conn = getConnection()) {
                    // Get file name first to delete from disk
                    String fileName = "";
                    try (PreparedStatement pstmt = conn.prepareStatement("SELECT pdf_path FROM books WHERE id = ?")) {
                        pstmt.setString(1, id);
                        ResultSet rs = pstmt.executeQuery();
                        if (rs.next()) fileName = rs.getString("pdf_path");
                    }
                    
                    if (!fileName.isEmpty()) {
                        File file = new File(BOOKS_DIR, fileName);
                        if (file.exists()) file.delete();
                    }

                    try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM books WHERE id = ?")) {
                        pstmt.setString(1, id);
                        pstmt.executeUpdate();
                    }
                } catch (Exception e) { e.printStackTrace(); }
                exchange.sendResponseHeaders(204, -1);
            }
            exchange.getResponseBody().close();
        }

        private String extract(String json, String key) {
            String pattern = "\"" + key + "\":\"";
            int start = json.indexOf(pattern);
            if (start == -1) return "";
            start += pattern.length();
            int end = json.indexOf("\"", start);
            if (end == -1) return "";
            return json.substring(start, end);
        }
    }

    static class EnquiryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if ((method.equalsIgnoreCase("GET") || method.equalsIgnoreCase("DELETE")) && !isAdminSession(exchange)) {
                exchange.sendResponseHeaders(401, -1); exchange.close(); return;
            }
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if (method.equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            if (method.equalsIgnoreCase("POST")) {
                InputStream is = exchange.getRequestBody();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[16384];
                while ((nRead = is.read(data, 0, data.length)) != -1) buffer.write(data, 0, nRead);
                String body = buffer.toString("UTF-8");

                try {
                    String name = extract(body, "name");
                    String email = extract(body, "email");
                    String mobile = extract(body, "mobile");
                    String subject = extract(body, "subject");
                    String message = extract(body, "message");

                    try (Connection conn = getConnection();
                         PreparedStatement pstmt = conn.prepareStatement("INSERT INTO enquiries (name, email, mobile, subject, message) VALUES (?, ?, ?, ?, ?)")) {
                        pstmt.setString(1, name);
                        pstmt.setString(2, email);
                        pstmt.setString(3, mobile);
                        pstmt.setString(4, subject);
                        pstmt.setString(5, message);
                        pstmt.executeUpdate();
                    }
                    exchange.sendResponseHeaders(201, 0);
                } catch (Exception e) { e.printStackTrace(); exchange.sendResponseHeaders(400, -1); }
            } 
            else if (method.equalsIgnoreCase("GET")) {
                if (!isAdminSession(exchange)) { exchange.sendResponseHeaders(401, -1); exchange.close(); return; }
                List<String> list = new ArrayList<>();
                try (Connection conn = getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT * FROM enquiries ORDER BY timestamp DESC")) {
                    while (rs.next()) {
                        list.add(String.format("{\"id\":%d, \"name\":\"%s\", \"email\":\"%s\", \"mobile\":\"%s\", \"subject\":\"%s\", \"message\":\"%s\", \"timestamp\":\"%s\"}",
                            rs.getLong("id"), rs.getString("name"), rs.getString("email"), 
                            rs.getString("mobile"), rs.getString("subject"), 
                            rs.getString("message").replace("\"", "\\\"").replace("\n", " "), rs.getString("timestamp")));
                    }
                } catch (Exception e) { e.printStackTrace(); }
                String json = "[" + String.join(",", list) + "]";
                byte[] bytes = json.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
            else if (method.equalsIgnoreCase("DELETE")) {
                String path = exchange.getRequestURI().getPath();
                String id = path.substring(path.lastIndexOf('/') + 1);
                try (Connection conn = getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("DELETE FROM enquiries WHERE id = ?")) {
                    pstmt.setString(1, id);
                    pstmt.executeUpdate();
                } catch (Exception e) { e.printStackTrace(); }
                exchange.sendResponseHeaders(204, -1);
            }
            exchange.getResponseBody().close();
        }

        private String extract(String json, String key) {
            String pattern = "\"" + key + "\":\"";
            int start = json.indexOf(pattern);
            if (start == -1) return "";
            start += pattern.length();
            int end = json.indexOf("\"", start);
            if (end == -1) return "";
            return json.substring(start, end);
        }
    }

    static class ApplicationHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if (method.equalsIgnoreCase("GET") && !isAdminSession(exchange)) {
                exchange.sendResponseHeaders(401, -1); exchange.close(); return;
            }
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if (method.equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            if (method.equalsIgnoreCase("POST")) {
                BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), "utf-8"));
                String body = br.lines().collect(Collectors.joining());
                
                try {
                    String studentName = extract(body, "name");
                    String studentClass = extract(body, "class");
                    String mobile = extract(body, "mobile");
                    
                    try (Connection conn = getConnection();
                         PreparedStatement pstmt = conn.prepareStatement("INSERT INTO applications (student_name, student_class, mobile, data_json) VALUES (?, ?, ?, ?)")) {
                        pstmt.setString(1, studentName);
                        pstmt.setString(2, studentClass);
                        pstmt.setString(3, mobile);
                        pstmt.setString(4, body);
                        pstmt.executeUpdate();
                    }
                    String response = "{\"status\":\"success\"}";
                    exchange.sendResponseHeaders(201, response.length());
                    exchange.getResponseBody().write(response.getBytes());
                } catch (Exception e) { e.printStackTrace(); exchange.sendResponseHeaders(400, -1); }
            } 
            else if (method.equalsIgnoreCase("GET")) {
                List<String> list = new ArrayList<>();
                try (Connection conn = getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT data_json FROM applications ORDER BY timestamp DESC")) {
                    while (rs.next()) {
                        list.add(rs.getString("data_json"));
                    }
                } catch (Exception e) { e.printStackTrace(); }
                String json = "[" + String.join(",", list) + "]";
                byte[] bytes = json.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
            exchange.getResponseBody().close();
        }
        private String extract(String json, String key) {
            String pattern = "\"" + key + "\":\"";
            int start = json.indexOf(pattern);
            if (start == -1) return "";
            start += pattern.length();
            int end = json.indexOf("\"", start);
            if (end == -1) return "";
            return json.substring(start, end);
        }
    }

    // --- Handlers ---
    static class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";

            File file = new File(STATIC_DIR + path);
            if (!file.exists() || file.isDirectory()) {
                String response = "404 Not Found";
                exchange.sendResponseHeaders(404, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.getResponseBody().close();
                return;
            }

            String contentType = getContentType(path);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, file.length());
            Files.copy(file.toPath(), exchange.getResponseBody());
            exchange.getResponseBody().close();
        }

        private String getContentType(String path) {
            if (path.endsWith(".html")) return "text/html";
            if (path.endsWith(".css")) return "text/css";
            if (path.endsWith(".js")) return "application/javascript";
            if (path.endsWith(".png")) return "image/png";
            if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
            if (path.endsWith(".pdf")) return "application/pdf";
            return "text/plain";
        }
    }

    static class GalleryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if ((method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("DELETE")) && !isAdminSession(exchange)) {
                exchange.sendResponseHeaders(401, -1); exchange.close(); return;
            }
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if (method.equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            if (method.equalsIgnoreCase("GET")) {
                File dir = new File(GALLERY_DIR);
                String[] files = dir.list();
                String json = "[]";
                if (files != null) {
                    json = "[" + java.util.Arrays.stream(files)
                            .filter(f -> f.toLowerCase().endsWith(".jpg") || f.toLowerCase().endsWith(".png") || f.toLowerCase().endsWith(".jpeg"))
                            .map(f -> "\"" + f + "\"")
                            .collect(Collectors.joining(",")) + "]";
                }
                byte[] bytes = json.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            } 
            else if (method.equalsIgnoreCase("POST")) {
                InputStream is = exchange.getRequestBody();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[16384];
                while ((nRead = is.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                String body = buffer.toString("UTF-8");
                
                // More robust Base64 Extraction
                try {
                    String fileName = extractValue(body, "name");
                    String base64Data = extractValue(body, "data");
                    
                    if (base64Data.contains(",")) base64Data = base64Data.split(",")[1];
                    
                    byte[] imageData = java.util.Base64.getDecoder().decode(base64Data.trim());
                    Files.write(Paths.get(GALLERY_DIR, fileName), imageData);
                    
                    String resp = "{\"status\":\"uploaded\"}";
                    exchange.sendResponseHeaders(201, resp.length());
                    exchange.getResponseBody().write(resp.getBytes());
                    System.out.println(">>> Photo Uploaded: " + fileName);
                } catch (Exception e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(400, -1);
                }
            }
            else if (method.equalsIgnoreCase("DELETE")) {
                String path = exchange.getRequestURI().getPath();
                String fileName = path.substring(path.lastIndexOf('/') + 1);
                File file = new File(GALLERY_DIR, fileName);
                if (file.exists() && file.delete()) {
                    exchange.sendResponseHeaders(204, -1);
                } else {
                    exchange.sendResponseHeaders(404, -1);
                }
            }
            exchange.getResponseBody().close();
        }

        private String extractValue(String json, String key) {
            String pattern = "\"" + key + "\":\"";
            int start = json.indexOf(pattern);
            if (start == -1) return "";
            start += pattern.length();
            int end = json.indexOf("\"", start);
            if (end == -1) return "";
            return json.substring(start, end);
        }
    }


    static class ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if (!method.equalsIgnoreCase("GET") && !method.equalsIgnoreCase("OPTIONS") && !isAdminSession(exchange)) {
                exchange.sendResponseHeaders(401, -1); exchange.close(); return;
            }
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if (method.equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            if (method.equalsIgnoreCase("GET")) {
                List<String> list = new ArrayList<>();
                try (Connection conn = getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT * FROM notifications ORDER BY date DESC")) {
                    while (rs.next()) {
                        list.add(String.format("{\"id\":%d, \"title\":\"%s\", \"content\":\"%s\", \"category\":\"%s\", \"date\":\"%s\"}",
                            rs.getLong("id"), rs.getString("title"), rs.getString("content"), 
                            rs.getString("category"), rs.getString("date")));
                    }
                } catch (Exception e) { e.printStackTrace(); }
                String json = "[" + String.join(",", list) + "]";
                byte[] bytes = json.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            } 
            else if (method.equalsIgnoreCase("POST")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
                String body = reader.lines().collect(Collectors.joining());
                try {
                    String title = extract(body, "title");
                    String content = extract(body, "content");
                    String category = extract(body, "category");
                    
                    try (Connection conn = getConnection();
                         PreparedStatement pstmt = conn.prepareStatement("INSERT INTO notifications (title, content, category) VALUES (?, ?, ?)")) {
                        pstmt.setString(1, title);
                        pstmt.setString(2, content);
                        pstmt.setString(3, category);
                        pstmt.executeUpdate();
                    }
                    exchange.sendResponseHeaders(201, 0);
                } catch (Exception e) { e.printStackTrace(); exchange.sendResponseHeaders(400, -1); }
            }
            else if (method.equalsIgnoreCase("DELETE")) {
                String path = exchange.getRequestURI().getPath();
                String idStr = path.substring(path.lastIndexOf('/') + 1);
                try (Connection conn = getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("DELETE FROM notifications WHERE id = ?")) {
                    pstmt.setLong(1, Long.parseLong(idStr));
                    pstmt.executeUpdate();
                    exchange.sendResponseHeaders(204, -1);
                } catch (Exception e) {
                    exchange.sendResponseHeaders(400, -1);
                }
            }
            exchange.getResponseBody().close();
        }

        private String extract(String json, String key) {
            String pattern = "\"" + key + "\":\"";
            int start = json.indexOf(pattern);
            if (start == -1) return "";
            start += pattern.length();
            int end = json.indexOf("\"", start);
            if (end == -1) return "";
            return json.substring(start, end);
        }
    }

    static class Notification {
        long id; String title; String content; String category; String date;
        Notification(long id, String title, String content, String category) {
            this.id = id; this.title = title; this.content = content; this.category = category;
            this.date = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        }
        String toJson() {
            return String.format("{\"id\":%d, \"title\":\"%s\", \"content\":\"%s\", \"category\":\"%s\", \"date\":\"%s\"}",
                    id, escape(title), escape(content), escape(category), date);
        }
        private String escape(String s) { return s.replace("\"", "\\\"").replace("\n", "\\n"); }
    }

    static class StudentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if ((method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("DELETE")) && !isAdminSession(exchange)) {
                exchange.sendResponseHeaders(401, -1); exchange.close(); return;
            }
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if (method.equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            if (method.equalsIgnoreCase("GET")) {
                String query = exchange.getRequestURI().getQuery();
                String rollParam = "";
                if (query != null && query.contains("roll=")) {
                    rollParam = query.substring(query.indexOf("roll=") + 5);
                    if (rollParam.contains("&")) rollParam = rollParam.substring(0, rollParam.indexOf("&"));
                }

                List<String> list = new ArrayList<>();
                if (rollParam.isEmpty() && !isAdminSession(exchange)) { exchange.sendResponseHeaders(401, -1); exchange.close(); return; }
                String sql = rollParam.isEmpty() ? "SELECT * FROM students ORDER BY created_at DESC" : "SELECT * FROM students WHERE roll_no = ?";
                
                try (Connection conn = getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    
                    if (!rollParam.isEmpty()) pstmt.setString(1, rollParam);
                    
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            list.add(String.format("{\"id\":%d, \"name\":\"%s\", \"father_name\":\"%s\", \"student_class\":\"%s\", \"roll_no\":\"%s\", \"dob\":\"%s\", \"blood_group\":\"%s\", \"photo\":\"%s\", \"address\":\"%s\"}",
                                rs.getLong("id"), rs.getString("name"), rs.getString("father_name"), 
                                rs.getString("student_class"), rs.getString("roll_no"), 
                                rs.getString("dob"), rs.getString("blood_group"), 
                                rs.getString("photo"), rs.getString("address").replace("\n", "\\n")));
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
                
                String json = !rollParam.isEmpty() && list.isEmpty() ? "{}" : (rollParam.isEmpty() ? "[" + String.join(",", list) + "]" : list.get(0));
                byte[] bytes = json.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            } 
            else if (method.equalsIgnoreCase("POST")) {
                InputStream is = exchange.getRequestBody();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[16384];
                while ((nRead = is.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                String body = buffer.toString("UTF-8");

                try {
                    String name = extract(body, "name");
                    String father = extract(body, "father_name");
                    String sClass = extract(body, "student_class");
                    String roll = extract(body, "roll_no");
                    String dob = extract(body, "dob");
                    String blood = extract(body, "blood_group");
                    String address = extract(body, "address");
                    String photoBase64 = extract(body, "photo");
                    
                    String fileName = "student_default.jpg";
                    if (photoBase64.startsWith("data:image")) {
                        if (photoBase64.contains(",")) photoBase64 = photoBase64.split(",")[1];
                        fileName = "student_" + System.currentTimeMillis() + ".jpg";
                        byte[] imageData = java.util.Base64.getDecoder().decode(photoBase64.trim());
                        Files.write(Paths.get(FACULTY_DIR, fileName), imageData);
                    }
                    
                    try (Connection conn = getConnection();
                         PreparedStatement pstmt = conn.prepareStatement("INSERT INTO students (name, father_name, student_class, roll_no, dob, blood_group, photo, address) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                        pstmt.setString(1, name);
                        pstmt.setString(2, father);
                        pstmt.setString(3, sClass);
                        pstmt.setString(4, roll);
                        pstmt.setString(5, dob);
                        pstmt.setString(6, blood);
                        pstmt.setString(7, fileName);
                        pstmt.setString(8, address);
                        pstmt.executeUpdate();
                    }
                    exchange.sendResponseHeaders(201, 0);
                } catch (Exception e) { e.printStackTrace(); exchange.sendResponseHeaders(400, -1); }
            }
            else if (method.equalsIgnoreCase("DELETE")) {
                String path = exchange.getRequestURI().getPath();
                String idStr = path.substring(path.lastIndexOf('/') + 1);
                try (Connection conn = getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("DELETE FROM students WHERE id = ?")) {
                    pstmt.setLong(1, Long.parseLong(idStr));
                    pstmt.executeUpdate();
                    exchange.sendResponseHeaders(204, -1);
                } catch (Exception e) {
                    exchange.sendResponseHeaders(400, -1);
                }
            }
            exchange.getResponseBody().close();
        }

        private String extract(String json, String key) {
            String pattern = "\"" + key + "\":\"";
            int start = json.indexOf(pattern);
            if (start == -1) return "";
            start += pattern.length();
            int end = json.indexOf("\"", start);
            if (end == -1) return "";
            return json.substring(start, end);
        }
    }

    static class ResultHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if ((method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("DELETE")) && !isAdminSession(exchange)) {
                exchange.sendResponseHeaders(401, -1); exchange.close(); return;
            }
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if (method.equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            if (method.equalsIgnoreCase("GET")) {
                String query = exchange.getRequestURI().getQuery();
                String roll = query != null && query.contains("roll=") ? query.split("roll=")[1].split("&")[0] : null;
                
                List<String> list = new ArrayList<>();
                String sql = roll != null ? "SELECT * FROM results WHERE roll_no = ?" : "SELECT * FROM results ORDER BY created_at DESC";
                
                try (Connection conn = getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    if (roll != null) pstmt.setString(1, roll);
                    ResultSet rs = pstmt.executeQuery();
                    while (rs.next()) {
                        list.add(String.format("{\"id\":%d, \"roll_no\":\"%s\", \"student_name\":\"%s\", \"student_class\":\"%s\", \"subjects\":\"%s\", \"total\":%d, \"obtained\":%d, \"percentage\":%.2f, \"grade\":\"%s\", \"status\":\"%s\"}",
                            rs.getLong("id"), rs.getString("roll_no"), rs.getString("student_name"), 
                            rs.getString("student_class"), rs.getString("subjects_json").replace("\"", "\\\""), 
                            rs.getInt("total_marks"), rs.getInt("obtained_marks"), 
                            rs.getDouble("percentage"), rs.getString("grade"), rs.getString("status")));
                    }
                } catch (Exception e) { e.printStackTrace(); }

                String json = roll != null && list.size() > 0 ? list.get(0) : "[" + String.join(",", list) + "]";
                byte[] bytes = json.getBytes();
                exchange.sendResponseHeaders(list.size() > 0 || roll == null ? 200 : 404, bytes.length);
                exchange.getResponseBody().write(bytes);
            } 
            else if (method.equalsIgnoreCase("POST")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), "UTF-8");
                try {
                    String roll = extract(body, "roll_no");
                    String name = extract(body, "student_name");
                    String sClass = extract(body, "student_class");
                    
                    // Smarter subjects extraction
                    String subjects = "{}";
                    int subIndex = body.indexOf("\"subjects\":");
                    if (subIndex != -1) {
                        int start = body.indexOf("\"", subIndex + 11);
                        if (start != -1) {
                            int end = body.lastIndexOf("\"");
                            // Backwards search for the closing quote of the subjects string
                            if (end > start) subjects = body.substring(start + 1, end).replace("\\\"", "\"");
                        } else {
                            // Fallback for non-stringified object
                            subjects = body.substring(subIndex + 11, body.lastIndexOf("}")).trim();
                        }
                    }
                    
                    int total = Integer.parseInt(extract(body, "total_marks"));
                    int obtained = Integer.parseInt(extract(body, "obtained_marks"));
                    double percentage = (double)obtained / total * 100;
                    String status = percentage >= 33 ? "PASS" : "FAIL";
                    String grade = percentage >= 90 ? "A+" : percentage >= 75 ? "A" : percentage >= 60 ? "B" : percentage >= 45 ? "C" : "D";

                    try (Connection conn = getConnection();
                         PreparedStatement pstmt = conn.prepareStatement("INSERT INTO results (roll_no, student_name, student_class, subjects_json, total_marks, obtained_marks, percentage, grade, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE student_name=?, student_class=?, subjects_json=?, total_marks=?, obtained_marks=?, percentage=?, grade=?, status=?")) {
                        pstmt.setString(1, roll);
                        pstmt.setString(2, name);
                        pstmt.setString(3, sClass);
                        pstmt.setString(4, subjects);
                        pstmt.setInt(5, total);
                        pstmt.setInt(6, obtained);
                        pstmt.setDouble(7, percentage);
                        pstmt.setString(8, grade);
                        pstmt.setString(9, status);
                        // Update fields
                        pstmt.setString(10, name);
                        pstmt.setString(11, sClass);
                        pstmt.setString(12, subjects);
                        pstmt.setInt(13, total);
                        pstmt.setInt(14, obtained);
                        pstmt.setDouble(15, percentage);
                        pstmt.setString(16, grade);
                        pstmt.setString(17, status);
                        pstmt.executeUpdate();
                    }
                    exchange.sendResponseHeaders(201, 0);
                } catch (Exception e) { e.printStackTrace(); exchange.sendResponseHeaders(400, -1); }
            }
            else if (method.equalsIgnoreCase("DELETE")) {
                String path = exchange.getRequestURI().getPath();
                String idStr = path.substring(path.lastIndexOf('/') + 1);
                try (Connection conn = getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("DELETE FROM results WHERE id = ?")) {
                    pstmt.setLong(1, Long.parseLong(idStr));
                    pstmt.executeUpdate();
                    exchange.sendResponseHeaders(204, -1);
                } catch (Exception e) { exchange.sendResponseHeaders(400, -1); }
            }
            exchange.getResponseBody().close();
        }

        private String extract(String json, String key) {
            String quoteKey = "\"" + key + "\":";
            int keyIndex = json.indexOf(quoteKey);
            if (keyIndex == -1) return "";
            
            int start = keyIndex + quoteKey.length();
            // Skip whitespace
            while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
            
            if (start < json.length() && json.charAt(start) == '\"') {
                // String value
                int end = json.indexOf("\"", start + 1);
                if (end == -1) return "";
                return json.substring(start + 1, end);
            } else {
                // Number or boolean value
                int end = start;
                while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-')) end++;
                if (end == start) {
                    // Maybe it's stopped by a comma or brace
                    int comma = json.indexOf(",", start);
                    int brace = json.indexOf("}", start);
                    if (comma == -1) end = brace;
                    else if (brace == -1) end = comma;
                    else end = Math.min(comma, brace);
                }
                if (end == -1) return "";
                return json.substring(start, end).trim();
            }
        }
    }
}
