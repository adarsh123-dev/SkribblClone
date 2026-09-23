package com.skribbl.dao;

import com.skribbl.connection.MyJdbcConnection;
import com.skribbl.model.Player;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlayerDao {

    public int addPlayer(String name, int roomId) throws SQLException {
        String sql = "INSERT INTO players (name, room_id) VALUES (?, ?)";
        try (Connection con = MyJdbcConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setInt(2, roomId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public Player getPlayerById(int id) throws SQLException {
        String sql = "SELECT * FROM players WHERE id = ?";
        try (Connection con = MyJdbcConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public List<Player> getPlayersByRoom(int roomId) throws SQLException {
        List<Player> list = new ArrayList<>();
        String sql = "SELECT * FROM players WHERE room_id = ? ORDER BY id ASC";
        try (Connection con = MyJdbcConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public void addScore(int playerId, int pointsToAdd) throws SQLException {
        String sql = "UPDATE players SET score = score + ? WHERE id = ?";
        try (Connection con = MyJdbcConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, pointsToAdd);
            ps.setInt(2, playerId);
            ps.executeUpdate();
        }
    }

    public void setHasGuessed(int playerId, boolean value) throws SQLException {
        String sql = "UPDATE players SET has_guessed = ? WHERE id = ?";
        try (Connection con = MyJdbcConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, value);
            ps.setInt(2, playerId);
            ps.executeUpdate();
        }
    }

    public void resetHasGuessedForRoom(int roomId) throws SQLException {
        String sql = "UPDATE players SET has_guessed = FALSE WHERE room_id = ?";
        try (Connection con = MyJdbcConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.executeUpdate();
        }
    }

    public void setDrawer(int roomId, int drawerPlayerId) throws SQLException {
        String clearAll = "UPDATE players SET is_drawer = FALSE WHERE room_id = ?";
        String setOne = "UPDATE players SET is_drawer = TRUE WHERE id = ?";
        try (Connection con = MyJdbcConnection.getConnection()) {
            try (PreparedStatement ps1 = con.prepareStatement(clearAll)) {
                ps1.setInt(1, roomId);
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = con.prepareStatement(setOne)) {
                ps2.setInt(1, drawerPlayerId);
                ps2.executeUpdate();
            }
        }
    }

    /** Returns true if every non-drawer player in the room has guessed correctly. */
    public boolean haveAllGuessed(int roomId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM players WHERE room_id = ? AND is_drawer = FALSE AND has_guessed = FALSE";
        try (Connection con = MyJdbcConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) == 0;
            }
        }
        return false;
    }

    private Player mapRow(ResultSet rs) throws SQLException {
        return new Player(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getInt("room_id"),
                rs.getInt("score"),
                rs.getBoolean("is_drawer"),
                rs.getBoolean("has_guessed")
        );
    }
}
