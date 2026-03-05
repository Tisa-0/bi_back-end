package com.rightmanage.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 用户DTO
 */
@Data
public class UserDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private Integer status;
    private String createTime;
    private List<Long> roleIds;
    private List<String> roleNames;
}
