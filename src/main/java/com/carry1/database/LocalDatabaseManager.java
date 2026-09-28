package com.carry1.database;

import com.carry1.models.AuthFlowState;
import com.carry1.models.Message;
import com.carry1.models.Order;
import com.carry1.models.OrderStatus;
import com.carry1.models.Role;
import com.carry1.models.User;
import com.carry1.models.Transaction;
import com.carry1.models.Complaint;
import com.carry1.models.ComplaintMessage;
import java.sql.Statement;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalDatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:carry1_local.db";

    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (id TEXT PRIMARY KEY, name TEXT NOT NULL, phone TEXT NOT NULL, password TEXT NOT NULL, role TEXT NOT NULL, balance REAL NOT NULL, account_status TEXT DEFAULT 'ACTIVE', rating REAL DEFAULT 5.0, email TEXT)");
            try { stmt.execute("ALTER TABLE users ADD COLUMN email TEXT"); } catch (java.sql.SQLException e) { if (!e.getMessage().contains("duplicate column name")) e.printStackTrace(); }
            try { stmt.execute("ALTER TABLE users ADD COLUMN rating REAL DEFAULT 5.0"); } catch (SQLException e) {}
            stmt.execute("CREATE TABLE IF NOT EXISTS orders (" +
                    "order_id TEXT PRIMARY KEY, customer_id TEXT NOT NULL, traveler_id TEXT, " +
                    "item_type TEXT, item_name TEXT NOT NULL, weight REAL, " +
                    "sender_name TEXT, sender_phone TEXT, pickup_loc TEXT NOT NULL, pickup_info TEXT, " +
                    "receiver_name TEXT, receiver_phone TEXT, dropoff_loc TEXT NOT NULL, dropoff_info TEXT, " +
                    "distance_km REAL, reward REAL NOT NULL, status TEXT NOT NULL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, order_id TEXT, sender_id TEXT, receiver_id TEXT, amount REAL, type TEXT, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");
            stmt.execute("CREATE TABLE IF NOT EXISTS messages (id INTEGER PRIMARY KEY AUTOINCREMENT, order_id TEXT NOT NULL, sender_id TEXT NOT NULL, receiver_id TEXT NOT NULL, message_text TEXT NOT NULL, timestamp INTEGER NOT NULL, is_read INTEGER DEFAULT 0)");
            stmt.execute("CREATE TABLE IF NOT EXISTS complaints (id INTEGER PRIMARY KEY AUTOINCREMENT, order_id TEXT, creator_id TEXT, against_id TEXT, status TEXT DEFAULT 'OPEN')");
            stmt.execute("CREATE TABLE IF NOT EXISTS complaint_messages (id INTEGER PRIMARY KEY AUTOINCREMENT, complaint_id INTEGER, sender_id TEXT, message_text TEXT, timestamp INTEGER)");
            stmt.execute("CREATE TABLE IF NOT EXISTS ratings (id INTEGER PRIMARY KEY AUTOINCREMENT, order_id TEXT NOT NULL, traveler_id TEXT NOT NULL, customer_id TEXT NOT NULL, rating_value INTEGER NOT NULL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS traveler_profiles (user_id TEXT PRIMARY KEY, roll TEXT NOT NULL, department TEXT NOT NULL, hall TEXT NOT NULL, graduation_year INTEGER NOT NULL)");

            ResultSet rs = stmt.executeQuery("SELECT id FROM users WHERE role = 'ADMIN'");
            if (!rs.next()) {
                stmt.execute("INSERT INTO users (id, name, phone, password, role, balance, account_status, email) VALUES ('ADMIN-adm', 'System Admin', 'admin@carry.com', 'adm', 'ADMIN', 0.0, 'ACTIVE', 'admin@carry.com')");
            }
            stmt.execute("UPDATE users SET phone = 'admin@carry.com', password = 'adm' WHERE role = 'ADMIN'");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    
    public static boolean registerUser(User user, String phone, String password, String email) {
        String insertSql = "INSERT INTO users (id, name, phone, password, role, balance, account_status, email) VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            pstmt.setString(1, user.getId()); pstmt.setString(2, user.getName()); pstmt.setString(3, phone); pstmt.setString(4, password); pstmt.setString(5, user.getRole().name()); pstmt.setDouble(6, user.getBalance()); pstmt.setString(7, email);
            pstmt.executeUpdate(); return true;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static boolean deleteUser(String userId) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static User authenticateUser(String phone, String password, Role role) {
        String sql = "SELECT * FROM users WHERE phone = ? AND password = ? AND role = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, phone); pstmt.setString(2, password); pstmt.setString(3, role.name());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                User u = new User(rs.getString("id"), rs.getString("name"), Role.valueOf(rs.getString("role")), rs.getDouble("balance"), "auth_token_" + rs.getString("id"), rs.getString("account_status"));
                u.setRating(rs.getDouble("rating"));
                return u;
            }
        } catch (SQLException e) {} return null;
    }

    public static boolean verifyUserPassword(String userId, String password) {
        String sql = "SELECT id FROM users WHERE id = ? AND password = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId); pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery(); return rs.next();
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static void saveSession(User user) { AuthFlowState.currentUser = user; }
    public static User getCurrentUser() { return AuthFlowState.currentUser; }

    public static void refreshCurrentUser() {
        if (AuthFlowState.currentUser == null) return;
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("SELECT balance, account_status, rating FROM users WHERE id = ?")) {
            pstmt.setString(1, AuthFlowState.currentUser.getId()); ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                AuthFlowState.currentUser = new User(AuthFlowState.currentUser.getId(), AuthFlowState.currentUser.getName(), AuthFlowState.currentUser.getRole(), rs.getDouble("balance"), AuthFlowState.currentUser.getAuthToken(), rs.getString("account_status"));
                AuthFlowState.currentUser.setRating(rs.getDouble("rating"));
            }
        } catch (SQLException e) {}
    }

    public static boolean addFundsToUser(String userId, double amount) {
        String sql = "UPDATE users SET balance = balance + ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, amount); pstmt.setString(2, userId); return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static boolean withdrawFundsFromUser(String userId, double amount) {
        String sql = "UPDATE users SET balance = balance - ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, amount); pstmt.setString(2, userId); return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static String getUserPhoneById(String userId) {
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("SELECT phone FROM users WHERE id = ?")) {
            pstmt.setString(1, userId); ResultSet rs = pstmt.executeQuery(); if (rs.next()) return rs.getString("phone");
        } catch (SQLException e) {} return "Unknown";
    }

    public static String getUserNameById(String userId) {
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("SELECT name FROM users WHERE id = ?")) {
            pstmt.setString(1, userId); ResultSet rs = pstmt.executeQuery(); if (rs.next()) return rs.getString("name");
        } catch (SQLException e) {} return "Unknown";
    }

    public static void clearSession() { AuthFlowState.currentUser = null; }

    
    public static boolean adjustUserBalance(String userId, double newBalance) {
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("UPDATE users SET balance = ? WHERE id = ?")) {
            pstmt.setDouble(1, newBalance); pstmt.setString(2, userId); return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static boolean setUserStatus(String userId, String status) {
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("UPDATE users SET account_status = ? WHERE id = ?")) {
            pstmt.setString(1, status); pstmt.setString(2, userId); return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE role != 'ADMIN'")) {
            while(rs.next()) {
                User u = new User(rs.getString("id"), rs.getString("name"), Role.valueOf(rs.getString("role")), rs.getDouble("balance"), "", rs.getString("account_status"));
                u.setRating(rs.getDouble("rating"));
                list.add(u);
            }
        } catch (SQLException e) {} return list;
    }

    public static List<Order> getAllOrders() {
        List<Order> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM orders ORDER BY status ASC")) {
            while(rs.next()) list.add(extractOrderFromResultSet(rs));
        } catch (SQLException e) {} return list;
    }

    public static Map<String, Double> getGlobalLedgerStats() {
        Map<String, Double> stats = new HashMap<>();
        try (Connection conn = DriverManager.getConnection(DB_URL); Statement stmt = conn.createStatement()) {
            ResultSet rsP = stmt.executeQuery("SELECT COUNT(*) AS c FROM orders WHERE status = 'PENDING' OR status = 'PICKED_UP'");
            stats.put("activeOrders", (double) rsP.getInt("c"));
            ResultSet rsD = stmt.executeQuery("SELECT COUNT(*) AS c FROM orders WHERE status = 'DELIVERED'");
            stats.put("deliveredOrders", (double) rsD.getInt("c"));
            ResultSet rsB = stmt.executeQuery("SELECT SUM(balance) AS s FROM users");
            stats.put("totalSystemMoney", rsB.getDouble("s"));
            ResultSet rsA = stmt.executeQuery("SELECT balance FROM users WHERE role = 'ADMIN'");
            stats.put("adminProfit", rsA.next() ? rsA.getDouble("balance") : 0.0);
        } catch (SQLException e) {} return stats;
    }

    
    public static boolean createOrderWithEscrow(Order order, double totalDeduction) {
        String sqlOrder = "INSERT INTO orders (order_id, customer_id, traveler_id, item_type, item_name, weight, sender_name, sender_phone, pickup_loc, pickup_info, receiver_name, receiver_phone, dropoff_loc, dropoff_info, distance_km, reward, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlDeduct = "UPDATE users SET balance = balance - ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);
            try (PreparedStatement psO = conn.prepareStatement(sqlOrder); PreparedStatement psD = conn.prepareStatement(sqlDeduct)) {
                psO.setString(1, order.getOrderId()); psO.setString(2, order.getCustomerId()); psO.setString(3, order.getTravelerId());
                psO.setString(4, order.getItemType()); psO.setString(5, order.getItemName()); psO.setDouble(6, order.getWeight());
                psO.setString(7, order.getSenderName()); psO.setString(8, order.getSenderPhone()); psO.setString(9, order.getPickupLocation()); psO.setString(10, order.getPickupInfo());
                psO.setString(11, order.getReceiverName()); psO.setString(12, order.getReceiverPhone()); psO.setString(13, order.getDropoffLocation()); psO.setString(14, order.getDropoffInfo());
                psO.setDouble(15, order.getDistanceKm()); psO.setDouble(16, order.getRewardAmount()); psO.setString(17, order.getStatus().name());

                psD.setDouble(1, totalDeduction); psD.setString(2, order.getCustomerId());

                if(psD.executeUpdate() > 0 && psO.executeUpdate() > 0) { conn.commit(); return true; }
                else { conn.rollback(); return false; }
            } catch (SQLException e) { conn.rollback(); return false; }
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static boolean cancelOrderAndRefundEscrow(String orderId, String customerId, double reward) {
        double totalRefund = reward * 1.05;
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);
            try (PreparedStatement psU = conn.prepareStatement("UPDATE orders SET status = 'CANCELLED' WHERE order_id = ? AND status = 'PENDING'");
                 PreparedStatement psR = conn.prepareStatement("UPDATE users SET balance = balance + ? WHERE id = ?")) {
                psU.setString(1, orderId); psR.setDouble(1, totalRefund); psR.setString(2, customerId);
                if (psU.executeUpdate() > 0 && psR.executeUpdate() > 0) { conn.commit(); return true; }
                else { conn.rollback(); return false; }
            } catch (SQLException e) { conn.rollback(); return false; }
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static boolean completeDeliveryWithCommission(String orderId, String travelerId, double reward) {
        double travelerEarning = Math.round((reward * 0.95) * 100.0) / 100.0;
        double adminCommission = Math.round((reward * 0.10) * 100.0) / 100.0;
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);
            try (PreparedStatement psGet = conn.prepareStatement("SELECT customer_id FROM orders WHERE order_id = ?");
                 PreparedStatement psT = conn.prepareStatement("UPDATE users SET balance = balance + ? WHERE id = ?");
                 PreparedStatement psA = conn.prepareStatement("UPDATE users SET balance = balance + ? WHERE role = 'ADMIN'");
                 PreparedStatement psO = conn.prepareStatement("UPDATE orders SET status = 'DELIVERED' WHERE order_id = ?");
                 PreparedStatement psTx = conn.prepareStatement("INSERT INTO transactions (order_id, sender_id, receiver_id, amount, type) VALUES (?, ?, ?, ?, ?)")) {
                 
                psGet.setString(1, orderId);
                ResultSet rs = psGet.executeQuery();
                String customerId = rs.next() ? rs.getString("customer_id") : "UNKNOWN";

                psT.setDouble(1, travelerEarning); psT.setString(2, travelerId);
                psA.setDouble(1, adminCommission);
                psO.setString(1, orderId);

                if (psT.executeUpdate() > 0 && psA.executeUpdate() > 0 && psO.executeUpdate() > 0) { 
                    psTx.setString(1, orderId); psTx.setString(2, customerId); psTx.setString(3, travelerId); psTx.setDouble(4, travelerEarning); psTx.setString(5, "DELIVERY_FEE");
                    psTx.executeUpdate();
                    psTx.setString(1, orderId); psTx.setString(2, customerId); psTx.setString(3, "SYSTEM"); psTx.setDouble(4, adminCommission); psTx.setString(5, "COMMISSION");
                    psTx.executeUpdate();
                    conn.commit(); return true; 
                }
                else { conn.rollback(); return false; }
            } catch (SQLException e) { conn.rollback(); return false; }
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static boolean reportDispute(String orderId) {
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("UPDATE orders SET status = 'DISPUTED' WHERE order_id = ?")) {
            pstmt.setString(1, orderId); return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static boolean forceRefundAdmin(String orderId, String customerId, double reward) {
        double totalRefund = reward * 1.05;
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);
            try (PreparedStatement psO = conn.prepareStatement("UPDATE orders SET status = 'CANCELLED' WHERE order_id = ?");
                 PreparedStatement psC = conn.prepareStatement("UPDATE users SET balance = balance + ? WHERE id = ?")) {
                psO.setString(1, orderId); psC.setDouble(1, totalRefund); psC.setString(2, customerId);
                if (psO.executeUpdate() > 0 && psC.executeUpdate() > 0) { conn.commit(); return true; }
                else { conn.rollback(); return false; }
            } catch (SQLException e) { conn.rollback(); return false; }
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static boolean forceOrderPending(String orderId) {
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("UPDATE orders SET status = 'PENDING', traveler_id = NULL WHERE order_id = ?")) {
            pstmt.setString(1, orderId); return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    private static Order extractOrderFromResultSet(ResultSet rs) throws SQLException {
        return new Order(rs.getString("order_id"), rs.getString("customer_id"), rs.getString("traveler_id"), rs.getString("item_type"), rs.getString("item_name"), rs.getDouble("weight"), rs.getString("sender_name"), rs.getString("sender_phone"), rs.getString("pickup_loc"), rs.getString("pickup_info"), rs.getString("receiver_name"), rs.getString("receiver_phone"), rs.getString("dropoff_loc"), rs.getString("dropoff_info"), rs.getDouble("distance_km"), rs.getDouble("reward"), OrderStatus.valueOf(rs.getString("status")));
    }

    public static Order getOrderById(String orderId) {
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM orders WHERE order_id = ?")) {
            pstmt.setString(1, orderId); ResultSet rs = pstmt.executeQuery(); if (rs.next()) return extractOrderFromResultSet(rs);
        } catch (SQLException e) {} return null;
    }

    
    private static int getOrderStatusPriority(OrderStatus status) {
        if (status == OrderStatus.AWAITING_CONFIRMATION || status == OrderStatus.PENDING || status == OrderStatus.PICKED_UP || status == OrderStatus.DISPUTED) return 1;
        if (status == OrderStatus.DELIVERED || status == OrderStatus.CANCELLED) return 2;
        return 3;
    }

    private static void sortOrders(List<Order> list) {
        list.sort((o1, o2) -> {
            int p1 = getOrderStatusPriority(o1.getStatus());
            int p2 = getOrderStatusPriority(o2.getStatus());
            if (p1 != p2) return Integer.compare(p1, p2);
            return o2.getOrderId().compareTo(o1.getOrderId());
        });
    }

    public static List<Order> getOrdersByCustomerId(String customerId) {
        List<Order> orderList = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM orders WHERE customer_id = ? ORDER BY status ASC")) {
            pstmt.setString(1, customerId); ResultSet rs = pstmt.executeQuery(); while (rs.next()) orderList.add(extractOrderFromResultSet(rs));
        } catch (SQLException e) {} sortOrders(orderList); return orderList;
    }

    public static List<Order> getPendingOrders() {
        List<Order> orderList = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM orders WHERE status = 'PENDING'")) {
            ResultSet rs = pstmt.executeQuery(); while (rs.next()) orderList.add(extractOrderFromResultSet(rs));
        } catch (SQLException e) {} return orderList;
    }

    public static List<Order> getOrdersByTravelerId(String travelerId) {
        List<Order> orderList = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM orders WHERE traveler_id = ? ORDER BY status DESC")) {
            pstmt.setString(1, travelerId); ResultSet rs = pstmt.executeQuery(); while (rs.next()) orderList.add(extractOrderFromResultSet(rs));
        } catch (SQLException e) {} sortOrders(orderList); return orderList;
    }

    public static boolean acceptOrder(String orderId, String travelerId) {
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("UPDATE orders SET traveler_id = ?, status = 'PICKED_UP' WHERE order_id = ? AND status = 'PENDING'")) {
            pstmt.setString(1, travelerId); pstmt.setString(2, orderId); return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static boolean updateOrderStatus(String orderId, OrderStatus newStatus) {
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("UPDATE orders SET status = ? WHERE order_id = ?")) {
            pstmt.setString(1, newStatus.name()); pstmt.setString(2, orderId); return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    
    public static boolean updateTravelerRating(String travelerId, double newStars) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement psGet = conn.prepareStatement("SELECT rating FROM users WHERE id = ?");
             PreparedStatement psUpd = conn.prepareStatement("UPDATE users SET rating = ? WHERE id = ?")) {
            psGet.setString(1, travelerId);
            ResultSet rs = psGet.executeQuery();
            if (rs.next()) {
                double currentRating = rs.getDouble("rating");
                double updatedRating = (currentRating == 5.0) ? newStars : (currentRating + newStars) / 2.0;
                psUpd.setDouble(1, Math.round(updatedRating * 10.0) / 10.0);
                psUpd.setString(2, travelerId);
                return psUpd.executeUpdate() > 0;
            }
        } catch (SQLException e) {}
        return false;
    }

    public static boolean submitTravelerRating(String orderId, String travelerId, String customerId, int ratingValue) {
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("INSERT INTO ratings (order_id, traveler_id, customer_id, rating_value) VALUES (?, ?, ?, ?)")) {
            pstmt.setString(1, orderId); pstmt.setString(2, travelerId); pstmt.setString(3, customerId); pstmt.setInt(4, ratingValue); return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static boolean sendTipTransaction(String customerId, String travelerId, double tipAmount) {
        String sqlCust = "UPDATE users SET balance = balance - ? WHERE id = ?";
        String sqlTrav = "UPDATE users SET balance = balance + ? WHERE id = ?";
        String sqlTx = "INSERT INTO transactions (order_id, sender_id, receiver_id, amount, type) VALUES ('N/A', ?, ?, ?, 'TIP')";
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);
            try (PreparedStatement psC = conn.prepareStatement(sqlCust); 
                 PreparedStatement psT = conn.prepareStatement(sqlTrav);
                 PreparedStatement psTx = conn.prepareStatement(sqlTx)) {
                psC.setDouble(1, tipAmount); psC.setString(2, customerId); int c = psC.executeUpdate();
                psT.setDouble(1, tipAmount); psT.setString(2, travelerId); int t = psT.executeUpdate();
                if (c > 0 && t > 0) { 
                    psTx.setString(1, customerId); psTx.setString(2, travelerId); psTx.setDouble(3, tipAmount); psTx.executeUpdate();
                    conn.commit(); return true; 
                } else { conn.rollback(); return false; }
            } catch (SQLException e) { conn.rollback(); return false; }
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static boolean sendMessage(String orderId, String senderId, String receiverId, String text) {
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("INSERT INTO messages (order_id, sender_id, receiver_id, message_text, timestamp, is_read) VALUES (?, ?, ?, ?, ?, 0)")) {
            pstmt.setString(1, orderId); pstmt.setString(2, senderId); pstmt.setString(3, receiverId); pstmt.setString(4, text); pstmt.setLong(5, System.currentTimeMillis()); return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static boolean hasUnreadMessages(String receiverId) {
        return getUnreadMessageCount(receiverId) > 0;
    }

    public static int getUnreadMessageCount(String receiverId) {
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(m.id) AS count FROM messages m JOIN orders o ON m.order_id = o.order_id WHERE m.receiver_id = ? AND m.is_read = 0 AND o.status IN ('PICKED_UP', 'AWAITING_CONFIRMATION')")) {
            pstmt.setString(1, receiverId); ResultSet rs = pstmt.executeQuery(); if (rs.next()) return rs.getInt("count");
        } catch (SQLException e) {} return 0;
    }

    public static List<Message> getOrderMessages(String orderId) {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM messages WHERE order_id = ? ORDER BY timestamp ASC")) {
            pstmt.setString(1, orderId); ResultSet rs = pstmt.executeQuery();
            while (rs.next()) messages.add(new Message(rs.getInt("id"), rs.getString("order_id"), rs.getString("sender_id"), rs.getString("receiver_id"), rs.getString("message_text"), rs.getLong("timestamp"), rs.getInt("is_read") == 1));
        } catch (SQLException e) {} return messages;
    }

    public static List<Message> getActiveInboxMessages(String currentUserId) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT m.* FROM messages m JOIN orders o ON m.order_id = o.order_id WHERE (m.receiver_id = ? OR m.sender_id = ?) AND o.status IN ('PICKED_UP', 'AWAITING_CONFIRMATION') ORDER BY m.timestamp ASC";
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, currentUserId); pstmt.setString(2, currentUserId); ResultSet rs = pstmt.executeQuery();
            while (rs.next()) messages.add(new Message(rs.getInt("id"), rs.getString("order_id"), rs.getString("sender_id"), rs.getString("receiver_id"), rs.getString("message_text"), rs.getLong("timestamp"), rs.getInt("is_read") == 1));
        } catch (SQLException e) {} return messages;
    }

    public static void markMessagesAsReadForOrder(String orderId, String receiverId) {
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("UPDATE messages SET is_read = 1 WHERE order_id = ? AND receiver_id = ?")) {
            pstmt.setString(1, orderId); pstmt.setString(2, receiverId); pstmt.executeUpdate();
        } catch (SQLException e) {}
    }


    public static boolean checkUserExists(String phone, Role role) {
        String sql = "SELECT id FROM users WHERE phone = ? AND role = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, phone);
            pstmt.setString(2, role.name());
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static boolean updateUserPassword(String phone, Role role, String newPassword) {
        String sql = "UPDATE users SET password = ? WHERE phone = ? AND role = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newPassword);
            pstmt.setString(2, phone);
            pstmt.setString(3, role.name());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static boolean saveTravelerProfile(String userId, String roll, String department, String hall, int graduationYear) {
        String sql = "INSERT OR REPLACE INTO traveler_profiles (user_id, roll, department, hall, graduation_year) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId); pstmt.setString(2, roll); pstmt.setString(3, department); pstmt.setString(4, hall); pstmt.setInt(5, graduationYear);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static Complaint createComplaint(String orderId, String creatorId, String againstId) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO complaints(order_id, creator_id, against_id) VALUES(?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, orderId);
            pstmt.setString(2, creatorId);
            pstmt.setString(3, againstId);
            pstmt.executeUpdate();
            reportDispute(orderId);
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return new Complaint(rs.getInt(1), orderId, creatorId, againstId, "OPEN");
                }
            }
        } catch (SQLException e) { }
        return null;
    }

    public static List<Complaint> getOpenComplaints() {
        List<Complaint> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM complaints WHERE status = 'OPEN'")) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new Complaint(rs.getInt("id"), rs.getString("order_id"), rs.getString("creator_id"), rs.getString("against_id"), rs.getString("status")));
            }
        } catch (SQLException e) { }
        return list;
    }

    public static List<Complaint> getOpenComplaintsForUser(String userId) {
        List<Complaint> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM complaints WHERE status = 'OPEN' AND (creator_id = ? OR against_id = ?)")) {
            pstmt.setString(1, userId);
            pstmt.setString(2, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new Complaint(rs.getInt("id"), rs.getString("order_id"), rs.getString("creator_id"), rs.getString("against_id"), rs.getString("status")));
            }
        } catch (SQLException e) { }
        return list;
    }

    public static boolean sendComplaintMessage(int complaintId, String senderId, String messageText) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO complaint_messages(complaint_id, sender_id, message_text, timestamp) VALUES(?, ?, ?, ?)")) {
            pstmt.setInt(1, complaintId);
            pstmt.setString(2, senderId);
            pstmt.setString(3, messageText);
            pstmt.setLong(4, System.currentTimeMillis());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static List<ComplaintMessage> getComplaintMessages(int complaintId) {
        List<ComplaintMessage> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM complaint_messages WHERE complaint_id = ? ORDER BY timestamp ASC")) {
            pstmt.setInt(1, complaintId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new ComplaintMessage(rs.getInt("id"), rs.getInt("complaint_id"), rs.getString("sender_id"), rs.getString("message_text"), rs.getLong("timestamp")));
            }
        } catch (SQLException e) { }
        return list;
    }

    public static boolean resolveComplaint(int complaintId) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("UPDATE complaints SET status = 'CLOSED' WHERE id = ?")) {
            pstmt.setInt(1, complaintId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static boolean reverseTransaction(int transactionId) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);
            try (PreparedStatement psGet = conn.prepareStatement("SELECT * FROM transactions WHERE id = ?")) {
                psGet.setInt(1, transactionId);
                ResultSet rs = psGet.executeQuery();
                if (!rs.next()) return false;
                String senderId = rs.getString("sender_id");
                String receiverId = rs.getString("receiver_id");
                double amount = rs.getDouble("amount");
                String orderId = rs.getString("order_id");
                String type = rs.getString("type");

                if ("REVERSAL".equals(type)) { conn.rollback(); return false; }

                double rxBalance = 0;
                if ("SYSTEM".equals(receiverId)) {
                    try (PreparedStatement psA = conn.prepareStatement("SELECT balance FROM users WHERE role = 'ADMIN'")) {
                        ResultSet rsA = psA.executeQuery();
                        if (rsA.next()) rxBalance = rsA.getDouble("balance");
                    }
                } else {
                    try (PreparedStatement psU = conn.prepareStatement("SELECT balance FROM users WHERE id = ?")) {
                        psU.setString(1, receiverId);
                        ResultSet rsU = psU.executeQuery();
                        if (rsU.next()) rxBalance = rsU.getDouble("balance");
                    }
                }

                if (rxBalance < amount) {
                    conn.rollback();
                    return false;
                }

                if ("SYSTEM".equals(receiverId)) {
                    try (PreparedStatement psA = conn.prepareStatement("UPDATE users SET balance = balance - ? WHERE role = 'ADMIN'")) {
                        psA.setDouble(1, amount); psA.executeUpdate();
                    }
                } else {
                    try (PreparedStatement psU = conn.prepareStatement("UPDATE users SET balance = balance - ? WHERE id = ?")) {
                        psU.setDouble(1, amount); psU.setString(2, receiverId); psU.executeUpdate();
                    }
                }

                if ("SYSTEM".equals(senderId)) {
                    try (PreparedStatement psA = conn.prepareStatement("UPDATE users SET balance = balance + ? WHERE role = 'ADMIN'")) {
                        psA.setDouble(1, amount); psA.executeUpdate();
                    }
                } else {
                    try (PreparedStatement psU = conn.prepareStatement("UPDATE users SET balance = balance + ? WHERE id = ?")) {
                        psU.setDouble(1, amount); psU.setString(2, senderId); psU.executeUpdate();
                    }
                }

                try (PreparedStatement psTx = conn.prepareStatement("INSERT INTO transactions (order_id, sender_id, receiver_id, amount, type) VALUES (?, ?, ?, ?, ?)")) {
                    psTx.setString(1, orderId); psTx.setString(2, receiverId); psTx.setString(3, senderId); psTx.setDouble(4, amount); psTx.setString(5, "REVERSAL");
                    psTx.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (Exception e) { conn.rollback(); return false; }
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static List<Transaction> getAllTransactions() {
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL); 
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM transactions ORDER BY timestamp DESC")) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new Transaction(rs.getInt("id"), rs.getString("order_id"), rs.getString("sender_id"), rs.getString("receiver_id"), rs.getDouble("amount"), rs.getString("type"), rs.getString("timestamp")));
            }
        } catch (SQLException e) {}
        return list;
    }
}
