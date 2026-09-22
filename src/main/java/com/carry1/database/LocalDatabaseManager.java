package com.carry1.database;

import com.carry1.models.AuthFlowState;
import com.carry1.models.Message;
import com.carry1.models.Order;
import com.carry1.models.OrderStatus;
import com.carry1.models.Role;
import com.carry1.models.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalDatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:carry1_local.db";

    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (id TEXT PRIMARY KEY, name TEXT NOT NULL, phone TEXT NOT NULL, password TEXT NOT NULL, role TEXT NOT NULL, balance REAL NOT NULL, account_status TEXT DEFAULT 'ACTIVE')");
            stmt.execute("CREATE TABLE IF NOT EXISTS orders (" +
                    "order_id TEXT PRIMARY KEY, customer_id TEXT NOT NULL, traveler_id TEXT, " +
                    "item_type TEXT, item_name TEXT NOT NULL, weight REAL, " +
                    "sender_name TEXT, sender_phone TEXT, pickup_loc TEXT NOT NULL, pickup_info TEXT, " +
                    "receiver_name TEXT, receiver_phone TEXT, dropoff_loc TEXT NOT NULL, dropoff_info TEXT, " +
                    "distance_km REAL, reward REAL NOT NULL, status TEXT NOT NULL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS messages (id INTEGER PRIMARY KEY AUTOINCREMENT, order_id TEXT NOT NULL, sender_id TEXT NOT NULL, receiver_id TEXT NOT NULL, message_text TEXT NOT NULL, timestamp INTEGER NOT NULL, is_read INTEGER DEFAULT 0)");
            stmt.execute("CREATE TABLE IF NOT EXISTS ratings (id INTEGER PRIMARY KEY AUTOINCREMENT, order_id TEXT NOT NULL, traveler_id TEXT NOT NULL, customer_id TEXT NOT NULL, rating_value INTEGER NOT NULL)");

            ResultSet rs = stmt.executeQuery("SELECT id FROM users WHERE role = 'ADMIN'");
            if (!rs.next()) {
                stmt.execute("INSERT INTO users (id, name, phone, password, role, balance, account_status) VALUES ('ADMIN-adm', 'System Admin', 'adm', 'adm123', 'ADMIN', 0.0, 'ACTIVE')");
            }
        } catch (SQLException e) { System.exit(1); }
    }

    // --- USER RELATED METHODS ---
    public static boolean registerUser(User user, String phone, String password) {
        String insertSql = "INSERT INTO users (id, name, phone, password, role, balance, account_status) VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE')";
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            pstmt.setString(1, user.getId()); pstmt.setString(2, user.getName()); pstmt.setString(3, phone); pstmt.setString(4, password); pstmt.setString(5, user.getRole().name()); pstmt.setDouble(6, user.getBalance());
            pstmt.executeUpdate(); return true;
        } catch (SQLException e) { return false; }
    }

    public static User authenticateUser(String phone, String password, Role role) {
        String sql = "SELECT * FROM users WHERE phone = ? AND password = ? AND role = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, phone); pstmt.setString(2, password); pstmt.setString(3, role.name());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return new User(rs.getString("id"), rs.getString("name"), Role.valueOf(rs.getString("role")), rs.getDouble("balance"), "auth_token_" + rs.getString("id"), rs.getString("account_status"));
        } catch (SQLException e) {} return null;
    }

    public static boolean verifyUserPassword(String userId, String password) {
        String sql = "SELECT id FROM users WHERE id = ? AND password = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId); pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery(); return rs.next();
        } catch (SQLException e) { return false; }
    }

    public static void saveSession(User user) { AuthFlowState.currentUser = user; }
    public static User getCurrentUser() { return AuthFlowState.currentUser; }

    public static void refreshCurrentUser() {
        if (AuthFlowState.currentUser == null) return;
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("SELECT balance, account_status FROM users WHERE id = ?")) {
            pstmt.setString(1, AuthFlowState.currentUser.getId()); ResultSet rs = pstmt.executeQuery();
            if (rs.next()) AuthFlowState.currentUser = new User(AuthFlowState.currentUser.getId(), AuthFlowState.currentUser.getName(), AuthFlowState.currentUser.getRole(), rs.getDouble("balance"), AuthFlowState.currentUser.getAuthToken(), rs.getString("account_status"));
        } catch (SQLException e) {}
    }

    // NEW ADDED MISSING METHODS FOR MOCK PAYMENT/WITHDRAWAL
    public static boolean addFundsToUser(String userId, double amount) {
        String sql = "UPDATE users SET balance = balance + ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, amount); pstmt.setString(2, userId); return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public static boolean withdrawFundsFromUser(String userId, double amount) {
        String sql = "UPDATE users SET balance = balance - ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, amount); pstmt.setString(2, userId); return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
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

    // --- ADMIN WALLET CONTROL & MODERATION ---
    public static boolean adjustUserBalance(String userId, double newBalance) {
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("UPDATE users SET balance = ? WHERE id = ?")) {
            pstmt.setDouble(1, newBalance); pstmt.setString(2, userId); return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public static boolean setUserStatus(String userId, String status) {
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("UPDATE users SET account_status = ? WHERE id = ?")) {
            pstmt.setString(1, status); pstmt.setString(2, userId); return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public static List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE role != 'ADMIN'")) {
            while(rs.next()) list.add(new User(rs.getString("id"), rs.getString("name"), Role.valueOf(rs.getString("role")), rs.getDouble("balance"), "", rs.getString("account_status")));
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

    // --- ESCROW & ORDER LOGIC ---
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
        } catch (SQLException e) { return false; }
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
        } catch (SQLException e) { return false; }
    }

    public static boolean completeDeliveryWithCommission(String orderId, String travelerId, double reward) {
        double travelerEarning = reward * 0.95;
        double adminCommission = reward * 0.10;

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);
            try (PreparedStatement psT = conn.prepareStatement("UPDATE users SET balance = balance + ? WHERE id = ?");
                 PreparedStatement psA = conn.prepareStatement("UPDATE users SET balance = balance + ? WHERE role = 'ADMIN'");
                 PreparedStatement psO = conn.prepareStatement("UPDATE orders SET status = 'DELIVERED' WHERE order_id = ?")) {
                psT.setDouble(1, travelerEarning); psT.setString(2, travelerId);
                psA.setDouble(1, adminCommission);
                psO.setString(1, orderId);

                if (psT.executeUpdate() > 0 && psA.executeUpdate() > 0 && psO.executeUpdate() > 0) { conn.commit(); return true; }
                else { conn.rollback(); return false; }
            } catch (SQLException e) { conn.rollback(); return false; }
        } catch (SQLException e) { return false; }
    }

    // --- DISPUTE & ADMIN OVERRIDE ---
    public static boolean reportDispute(String orderId) {
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("UPDATE orders SET status = 'DISPUTED' WHERE order_id = ?")) {
            pstmt.setString(1, orderId); return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
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
        } catch (SQLException e) { return false; }
    }

    public static boolean forceOrderPending(String orderId) {
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("UPDATE orders SET status = 'PENDING', traveler_id = NULL WHERE order_id = ?")) {
            pstmt.setString(1, orderId); return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    // --- GENERAL ORDER EXTRACTION & FETCHING ---
    private static Order extractOrderFromResultSet(ResultSet rs) throws SQLException {
        return new Order(rs.getString("order_id"), rs.getString("customer_id"), rs.getString("traveler_id"), rs.getString("item_type"), rs.getString("item_name"), rs.getDouble("weight"), rs.getString("sender_name"), rs.getString("sender_phone"), rs.getString("pickup_loc"), rs.getString("pickup_info"), rs.getString("receiver_name"), rs.getString("receiver_phone"), rs.getString("dropoff_loc"), rs.getString("dropoff_info"), rs.getDouble("distance_km"), rs.getDouble("reward"), OrderStatus.valueOf(rs.getString("status")));
    }

    public static Order getOrderById(String orderId) {
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM orders WHERE order_id = ?")) {
            pstmt.setString(1, orderId); ResultSet rs = pstmt.executeQuery(); if (rs.next()) return extractOrderFromResultSet(rs);
        } catch (SQLException e) {} return null;
    }

    public static List<Order> getOrdersByCustomerId(String customerId) {
        List<Order> orderList = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM orders WHERE customer_id = ? ORDER BY status ASC")) {
            pstmt.setString(1, customerId); ResultSet rs = pstmt.executeQuery(); while (rs.next()) orderList.add(extractOrderFromResultSet(rs));
        } catch (SQLException e) {} return orderList;
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
        } catch (SQLException e) {} return orderList;
    }

    public static boolean acceptOrder(String orderId, String travelerId) {
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("UPDATE orders SET traveler_id = ?, status = 'PICKED_UP' WHERE order_id = ? AND status = 'PENDING'")) {
            pstmt.setString(1, travelerId); pstmt.setString(2, orderId); return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public static boolean updateOrderStatus(String orderId, OrderStatus newStatus) {
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("UPDATE orders SET status = ? WHERE order_id = ?")) {
            pstmt.setString(1, newStatus.name()); pstmt.setString(2, orderId); return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    // --- RATINGS AND TIPS ---
    public static boolean submitTravelerRating(String orderId, String travelerId, String customerId, int ratingValue) {
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("INSERT INTO ratings (order_id, traveler_id, customer_id, rating_value) VALUES (?, ?, ?, ?)")) {
            pstmt.setString(1, orderId); pstmt.setString(2, travelerId); pstmt.setString(3, customerId); pstmt.setInt(4, ratingValue); return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public static boolean sendTipTransaction(String customerId, String travelerId, double tipAmount) {
        String sqlCust = "UPDATE users SET balance = balance - ? WHERE id = ?";
        String sqlTrav = "UPDATE users SET balance = balance + ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);
            try (PreparedStatement psC = conn.prepareStatement(sqlCust); PreparedStatement psT = conn.prepareStatement(sqlTrav)) {
                psC.setDouble(1, tipAmount); psC.setString(2, customerId); int c = psC.executeUpdate();
                psT.setDouble(1, tipAmount); psT.setString(2, travelerId); int t = psT.executeUpdate();
                if (c > 0 && t > 0) { conn.commit(); return true; } else { conn.rollback(); return false; }
            } catch (SQLException e) { conn.rollback(); return false; }
        } catch (SQLException e) { return false; }
    }

    // --- MESSAGING SYSTEM ---
    public static boolean sendMessage(String orderId, String senderId, String receiverId, String text) {
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("INSERT INTO messages (order_id, sender_id, receiver_id, message_text, timestamp, is_read) VALUES (?, ?, ?, ?, ?, 0)")) {
            pstmt.setString(1, orderId); pstmt.setString(2, senderId); pstmt.setString(3, receiverId); pstmt.setString(4, text); pstmt.setLong(5, System.currentTimeMillis()); return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
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
}