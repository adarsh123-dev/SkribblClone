package com.skribbl.dao;

import com.skribbl.connection.MyJdbcConnection;
import com.skribbl.model.Room;

import java.sql.*;

public class RoomDao {

    public int createRoom(String roomCode) throws SQLException {
        String sql = "INSERT INTO rooms (room_code, status) VALUES (?, 'WAITING')";
        try (Connection con = MyJdbcConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, roomCode);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public Room getRoomByCode(String roomCode) throws SQLException {
        String sql = "SELECT * FROM rooms WHERE room_code = ?";
        try (Connection con = MyJdbcConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, roomCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public Room getRoomById(int id) throws SQLException {
        String sql = "SELECT * FROM rooms WHERE id = ?";
        try (Connection con = MyJdbcConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public void updateStatus(int roomId, String status) throws SQLException {
        String sql = "UPDATE rooms SET status = ? WHERE id = ?";
        try (Connection con = MyJdbcConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, roomId);
            ps.executeUpdate();
        }
    }

    public void updateWordAndDrawer(int roomId, String word, int drawerId) throws SQLException {
        String sql = "UPDATE rooms SET current_word = ?, current_drawer_id = ?, status = 'PLAYING' WHERE id = ?";
        try (Connection con = MyJdbcConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, word);
            ps.setInt(2, drawerId);
            ps.setInt(3, roomId);
            ps.executeUpdate();
        }
    }

    public void incrementRound(int roomId) throws SQLException {
        String sql = "UPDATE rooms SET round_number = round_number + 1 WHERE id = ?";
        try (Connection con = MyJdbcConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.executeUpdate();
        }
    }

    private Room mapRow(ResultSet rs) throws SQLException {
        return new Room(
                rs.getInt("id"),
                rs.getString("room_code"),
                rs.getString("current_word"),
                rs.getInt("current_drawer_id"),
                rs.getInt("round_number"),
                rs.getString("status")
        );
    }
}
