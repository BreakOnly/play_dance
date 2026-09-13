package com.dance.studio.mapper;

import org.apache.ibatis.annotations.*;

import java.util.Map;

public interface OperationMapper {
    @Select(
            "SELECT payload_hash,result_json FROM operation_request WHERE scope=#{scope} AND"
                    + " request_key=#{key}")
    Map<String, Object> find(@Param("scope") String scope, @Param("key") String key);

    @Insert(
            "INSERT INTO operation_request(scope,request_key,payload_hash,result_json)"
                    + " VALUES(#{scope},#{key},#{hash},#{json})")
    int insert(
            @Param("scope") String scope,
            @Param("key") String key,
            @Param("hash") String hash,
            @Param("json") String json);
}
