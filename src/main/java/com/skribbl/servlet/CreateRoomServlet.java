package com.skribbl.servlet;

import com.skribbl.dao.PlayerDao;
import com.skribbl.dao.RoomDao;
import com.skribbl.model.Room;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

@WebServlet("/CreateRoomServlet")
public class CreateRoomServlet extends HttpServlet {

    private final RoomDao roomDao = new RoomDao();
    private final PlayerDao playerDao = new PlayerDao();
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json");
        String playerName = req.getParameter("playerName");
        PrintWriter out = resp.getWriter();

        if (playerName == null || playerName.trim().isEmpty()) {
            out.write("{\"success\":false,\"message\":\"Player name is required\"}");
            return;
        }

        try {
            String roomCode = generateUniqueRoomCode();
            int roomId = roomDao.createRoom(roomCode);
            int playerId = playerDao.addPlayer(playerName.trim(), roomId);

            out.write("{\"success\":true,"
                    + "\"roomId\":" + roomId + ","
                    + "\"roomCode\":\"" + roomCode + "\","
                    + "\"playerId\":" + playerId + ","
                    + "\"playerName\":\"" + escape(playerName.trim()) + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            out.write("{\"success\":false,\"message\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    private String generateUniqueRoomCode() throws Exception {
        Random rnd = new Random();
        String code;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                sb.append(CHARS.charAt(rnd.nextInt(CHARS.length())));
            }
            code = sb.toString();
        } while (roomDao.getRoomByCode(code) != null);
        return code;
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}
