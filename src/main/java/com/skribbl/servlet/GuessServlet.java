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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@WebServlet("/GuessServlet")
public class GuessServlet extends HttpServlet {

    private final RoomDao roomDao = new RoomDao();
    private final PlayerDao playerDao = new PlayerDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();

        try {
            int roomId = Integer.parseInt(req.getParameter("roomId"));
            int playerId = Integer.parseInt(req.getParameter("playerId"));
            String guess = req.getParameter("guess");

            if (guess == null || guess.trim().isEmpty()) {
                out.write("{\"success\":false,\"message\":\"Empty guess\"}");
                return;
            }
            guess = guess.trim();

            Room room = roomDao.getRoomById(roomId);
            Player player = playerDao.getPlayerById(playerId);

            if (room == null || player == null) {
                out.write("{\"success\":false,\"message\":\"Invalid room or player\"}");
                return;
            }

            // Drawer cannot guess their own word
            if (player.isDrawer()) {
                out.write("{\"success\":false,\"message\":\"You are the drawer\"}");
                return;
            }

            // Already guessed correctly this round - ignore further guesses
            if (player.isHasGuessed()) {
                saveMessage(roomId, player.getName(), guess, false);
                out.write("{\"success\":true,\"correct\":false}");
                return;
            }

            boolean correct = room.getCurrentWord() != null
                    && guess.equalsIgnoreCase(room.getCurrentWord());

            if (correct) {
                // Don't leak the word to other players via chat log
                saveMessage(roomId, player.getName(), player.getName() + " guessed the word!", true);
                playerDao.setHasGuessed(playerId, true);
                playerDao.addScore(playerId, 100);
                playerDao.addScore(room.getCurrentDrawerId(), 25); // reward the drawer too

                if (playerDao.haveAllGuessed(roomId)) {
                    roomDao.updateStatus(roomId, "ROUND_END");
                }
            } else {
                saveMessage(roomId, player.getName(), guess, false);
            }

            out.write("{\"success\":true,\"correct\":" + correct + "}");
        } catch (Exception e) {
            e.printStackTrace();
            out.write("{\"success\":false,\"message\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    private void saveMessage(int roomId, String playerName, String message, boolean correct) throws SQLException {
        String sql = "INSERT INTO messages (room_id, player_name, message, is_correct_guess) VALUES (?,?,?,?)";
        try (Connection con = MyJdbcConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setString(2, playerName);
            ps.setString(3, message);
            ps.setBoolean(4, correct);
            ps.executeUpdate();
        }
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}
