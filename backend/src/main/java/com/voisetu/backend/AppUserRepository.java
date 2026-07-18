package com.voisetu.backend;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AppUserRepository {
    private final JdbcTemplate jdbcTemplate;

    public AppUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<AppUser> rowMapper = (rs, rowNum) -> new AppUser(
        rs.getLong("id"),
        rs.getString("email"),
        rs.getString("password_hash"),
        rs.getString("display_name"),
        rs.getBoolean("is_admin"),
        rs.getTimestamp("created_at").toInstant()
    );

    public Optional<AppUser> findByEmail(String email) {
        var results = jdbcTemplate.query("SELECT * FROM app_user WHERE email = ?", rowMapper, email);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<AppUser> findById(Long id) {
        var results = jdbcTemplate.query("SELECT * FROM app_user WHERE id = ?", rowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public void save(String email, String passwordHash, String displayName) {
        jdbcTemplate.update(
            "INSERT INTO app_user (email, password_hash, display_name) VALUES (?, ?, ?)",
            email, passwordHash, displayName
        );
    }
    
    public void updateDisplayName(Long id, String displayName) {
        jdbcTemplate.update("UPDATE app_user SET display_name = ? WHERE id = ?", displayName, id);
    }
    
    public void updatePassword(Long id, String passwordHash) {
        jdbcTemplate.update("UPDATE app_user SET password_hash = ? WHERE id = ?", passwordHash, id);
    }
    
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM app_user WHERE id = ?", id);
    }
}
