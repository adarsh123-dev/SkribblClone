package com.skribbl.servlet;

import com.skribbl.connection.MyJdbcConnection;
import com.skribbl.dao.PlayerDao;
import com.skribbl.dao.RoomDao;
import com.skribbl.model.Player;
import com.skribbl.model.Room;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.List;
import java.util.Random;

@WebServlet("/GameServlet")
public class GameServlet extends HttpServlet {

    private final RoomDao roomDao = new RoomDao();
    private final PlayerDao playerDao = new PlayerDao();

    private static final String[] WORDS = {
            "apple", "banana", "guitar", "elephant", "mountain", "laptop", "rainbow",
            "pizza", "rocket", "butterfly", "castle", "bicycle", "dolphin", "umbrella",
            "volcano", "sandwich", "penguin", "dragon", "robot", "flower", "camera",
            "spider", "skateboard", "waterfall", "cactus", "lighthouse", "pirate",
            "snowman", "jellyfish", "helicopter"
    };

    // ---------------------------------------------------------------
    // GET: poll for current game state (room info, players, new strokes, new messages)
    // ---------------------------------------------------------------
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();

        try {
            int roomId = Integer.parseInt(req.getParameter("roomId"));
            int playerId = Integer.parseInt(req.getParameter("playerId"));
            int lastStrokeId = parseIntOrDefault(req.getParameter("lastStrokeId"), 0);
            int lastMessageId = parseIntOrDefault(req.getParameter("lastMessageId"), 0);

            Room room = roomDao.getRoomById(roomId);
            if (room == null) {
                out.write("{\"success\":false,\"message\":\"Room not found\"}");
                return;
            }
            Player me = playerDao.getPlayerById(playerId);
            List<Player> players = playerDao.getPlayersByRoom(roomId);

            boolean revealWord = me != null && me.isDrawer();
            boolean revealFull = "ROUND_END".equals(room.getStatus()) || (me != null && me.isHasGuessed());

            StringBuilder json = new StringBuilder();
            json.append("{\"success\":true,");
            json.append("\"roomStatus\":\"").append(room.getStatus()).append("\",");
            json.append("\"roundNumber\":").append(room.getRoundNumber()).append(",");
            json.append("\"currentDrawerId\":").append(room.getCurrentDrawerId()).append(",");
            json.append("\"isDrawer\":").append(me != null && me.isDrawer()).append(",");
            json.append("\"hasGuessed\":").append(me != null && me.isHasGuessed()).append(",");

            if (room.getCurrentWord() == null) {
                json.append("\"wordDisplay\":\"\",");
            } else if (revealWord || revealFull) {
                json.append("\"wordDisplay\":\"").append(room.getCurrentWord()).append("\",");
            } else {
                json.append("\"wordDisplay\":\"").append(blankFor(room.getCurrentWord())).append("\",");
            }

            // players
            json.append("\"players\":[");
            for (int i = 0; i < players.size(); i++) {
                Player p = players.get(i);
                if (i > 0) json.append(",");
                json.append("{\"id\":").append(p.getId())
                        .append(",\"name\":\"").append(escape(p.getName())).append("\"")
                        .append(",\"score\":").append(p.getScore())
                        .append(",\"isDrawer\":").append(p.isDrawer())
                        .append(",\"hasGuessed\":").append(p.isHasGuessed())
                        .append("}");
            }
            json.append("],");

            // strokes since lastStrokeId
            json.append("\"strokes\":[");
            appendStrokes(json, roomId, lastStrokeId);
            json.append("],");

            // messages since lastMessageId
            json.append("\"messages\":[");
            appendMessages(json, roomId, lastMessageId);
            json.append("]");

            json.append("}");
            out.write(json.toString());
        } catch (Exception e) {
            e.printStackTrace();
            out.write("{\"success\":false,\"message\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    // ---------------------------------------------------------------
    // POST: action = start | draw | clear | nextRound
    // ---------------------------------------------------------------
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        String action = req.getParameter("action");

        try {
            int roomId = Integer.parseInt(req.getParameter("roomId"));

            switch (action == null ? "" : action) {
                case "start":
                    startGame(roomId);
                    out.write("{\"success\":true}");
                    break;

                case "draw":
                    double x1 = Double.parseDouble(req.getParameter("x1"));
                    double y1 = Double.parseDouble(req.getParameter("y1"));
                    double x2 = Double.parseDouble(req.getParameter("x2"));
                    double y2 = Double.parseDouble(req.getParameter("y2"));
                    String color = req.getParameter("color");
                    int lineWidth = parseIntOrDefault(req.getParameter("lineWidth"), 3);
                    saveStroke(roomId, x1, y1, x2, y2, color, lineWidth, false);
                    out.write("{\"success\":true}");
                    break;

                case "clear":
                    saveStroke(roomId, 0, 0, 0, 0, "#000000", 0, true);
                    out.write("{\"success\":true}");
                    break;

                case "nextRound":
                    nextRound(roomId);
                    out.write("{\"success\":true}");
                    break;

                default:
                    out.write("{\"success\":false,\"message\":\"Unknown action\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.write("{\"success\":false,\"message\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    // ---------------- helpers ----------------

    private void startGame(int roomId) throws SQLException {
        List<Player> players = playerDao.getPlayersByRoom(roomId);
        if (players.isEmpty()) return;
        int drawerId = players.get(0).getId();
        String word = randomWord();
        playerDao.setDrawer(roomId, drawerId);
        playerDao.resetHasGuessedForRoom(roomId);
        roomDao.updateWordAndDrawer(roomId, word, drawerId);
        clearStrokesForRoom(roomId);
    }

    private void nextRound(int roomId) throws SQLException {
        List<Player> players = playerDao.getPlayersByRoom(roomId);
        if (players.isEmpty()) return;
        Room room = roomDao.getRoomById(roomId);

        int currentDrawerId = room.getCurrentDrawerId();
        int nextIndex = 0;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getId() == currentDrawerId) {
                nextIndex = (i + 1) % players.size();
                break;
            }
        }
        int nextDrawerId = players.get(nextIndex).getId();
        String word = randomWord();

        playerDao.setDrawer(roomId, nextDrawerId);
        playerDao.resetHasGuessedForRoom(roomId);
        roomDao.updateWordAndDrawer(roomId, word, nextDrawerId);
        roomDao.incrementRound(roomId);
        clearStrokesForRoom(roomId);
    }

    private String randomWord() {
        return WORDS[new Random().nextInt(WORDS.length)];
    }

    private String blankFor(String word) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            sb.append(word.charAt(i) == ' ' ? "  " : "_ ");
        }
        return sb.toString().trim();
    }

    private void saveStroke(int roomId, double x1, double y1, double x2, double y2,
                             String color, int lineWidth, boolean isClear) throws SQLException {
        String sql = "INSERT INTO strokes (room_id, x1, y1, x2, y2, color, line_width, is_clear) "
                + "VALUES (?,?,?,?,?,?,?,?)";
        try (Connection con = MyJdbcConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setDouble(2, x1);
            ps.setDouble(3, y1);
            ps.setDouble(4, x2);
            ps.setDouble(5, y2);
            ps.setString(6, color == null ? "#000000" : color);
            ps.setInt(7, lineWidth);
            ps.setBoolean(8, isClear);
            ps.executeUpdate();
        }
    }

    private void clearStrokesForRoom(int roomId) throws SQLException {
        String sql = "DELETE FROM strokes WHERE room_id = ?";
        try (Connection con = MyJdbcConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.executeUpdate();
        }
    }

    private void appendStrokes(StringBuilder json, int roomId, int lastStrokeId) throws SQLException {
        String sql = "SELECT * FROM strokes WHERE room_id = ? AND id > ? ORDER BY id ASC";
        try (Connection con = MyJdbcConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setInt(2, lastStrokeId);
            try (ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(",");
                    first = false;
                    json.append("{\"id\":").append(rs.getInt("id"))
                            .append(",\"x1\":").append(rs.getDouble("x1"))
                            .append(",\"y1\":").append(rs.getDouble("y1"))
                            .append(",\"x2\":").append(rs.getDouble("x2"))
                            .append(",\"y2\":").append(rs.getDouble("y2"))
                            .append(",\"color\":\"").append(rs.getString("color")).append("\"")
                            .append(",\"lineWidth\":").append(rs.getInt("line_width"))
                            .append(",\"isClear\":").append(rs.getBoolean("is_clear"))
                            .append("}");
                }
            }
        }
    }

    private void appendMessages(StringBuilder json, int roomId, int lastMessageId) throws SQLException {
        String sql = "SELECT * FROM messages WHERE room_id = ? AND id > ? ORDER BY id ASC";
        try (Connection con = MyJdbcConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setInt(2, lastMessageId);
            try (ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(",");
                    first = false;
                    json.append("{\"id\":").append(rs.getInt("id"))
                            .append(",\"playerName\":\"").append(escape(rs.getString("player_name"))).append("\"")
                            .append(",\"message\":\"").append(escape(rs.getString("message"))).append("\"")
                            .append(",\"isCorrect\":").append(rs.getBoolean("is_correct_guess"))
                            .append("}");
                }
            }
        }
    }

    private int parseIntOrDefault(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
