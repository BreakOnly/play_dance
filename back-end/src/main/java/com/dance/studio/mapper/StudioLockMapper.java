package com.dance.studio.mapper;

import org.apache.ibatis.annotations.Select;

/** 单门店写事务的数据库锁；多进程也共享同一把锁。 */
public interface StudioLockMapper {
    @Select("SELECT id FROM store WHERE id = 1 FOR UPDATE")
    Long lock();
}
