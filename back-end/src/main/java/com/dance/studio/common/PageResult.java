package com.dance.studio.common;

import com.github.pagehelper.PageInfo;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页返回体
 */
@Data
public class PageResult<T> implements Serializable {

    private List<T> list;
    private long total;
    private int page;
    private int size;

    public PageResult() {
    }

    public PageResult(List<T> list, long total, int page, int size) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public static <T> PageResult<T> of(List<T> list) {
        PageInfo<T> info = new PageInfo<>(list);
        return new PageResult<>(info.getList(), info.getTotal(), info.getPageNum(), info.getPageSize());
    }
}
