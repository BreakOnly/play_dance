package com.dance.studio.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.ibatis.type.*;

import java.sql.*;
import java.util.List;

public class JsonStringListTypeHandler extends BaseTypeHandler<List<String>> {
    private static final ObjectMapper JSON = new ObjectMapper();

    public void setNonNullParameter(PreparedStatement s, int i, List<String> value, JdbcType type)
            throws SQLException {
        try {
            s.setString(i, JSON.writeValueAsString(value));
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    private List<String> parse(String s) throws SQLException {
        if (s == null || s.isBlank()) return List.of();
        try {
            return JSON.readValue(s, new TypeReference<>() {});
        } catch (Exception e) {
            throw new SQLException("Invalid stored image list", e);
        }
    }

    public List<String> getNullableResult(ResultSet r, String c) throws SQLException {
        return parse(r.getString(c));
    }

    public List<String> getNullableResult(ResultSet r, int c) throws SQLException {
        return parse(r.getString(c));
    }

    public List<String> getNullableResult(CallableStatement s, int c) throws SQLException {
        return parse(s.getString(c));
    }
}
