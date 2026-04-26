package com.njhtr.marltsc.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Generic pagination DTO.
 *
 * @param <T> the type of items in the page
 */
@Data
public class PageDTO<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int pageNum;
    private int pageSize;
    private long total;
    private List<T> list;
}
