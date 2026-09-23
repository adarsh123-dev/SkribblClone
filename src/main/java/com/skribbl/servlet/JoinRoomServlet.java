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

@WebServlet("/JoinRoomServlet")
public class JoinRoomServlet extends HttpServlet {

    private final RoomDao roomDao = new RoomDao();
    private final PlayerDao playerDao = new PlayerDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json");
        String playerName = req.getParameter("playerName");
        String roomCode = req.getParameter("roomCode");
        PrintWriter out = resp.getWriter();

        if (playerName == null || playerName.trim().isEmpty()
                || roomCode == null || roomCode.trim().isEmpty()) {
            out.write("{\"success\":false,\"message\":\"Player name and room code are required\"}");
            return;
        }

        try {
            Room room = roomDao.getRoomByCode(roomCode.trim().toUpperCase());
            if (room == null) {
                out.write("{\"success\":false,\"message\":\"Room not found\"}");
                return;
            }

            int playerId = playerDao.addPlayer(playerName.trim(), room.getId());

            out.write("{\"success\":true,"
                    + "\"roomId\":" + room.getId() + ","
                    + "\"roomCode\":\"" + room.getRoomCode() + "\","
                    + "\"playerId\":" + playerId + ","
                    + "\"playerName\":\"" + escape(playerName.trim()) + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            out.write("{\"success\":false,\"message\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}
