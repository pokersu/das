package org.muguang.mybatisenhance.entity;

import lombok.Data;
import org.muguang.mybatisenhance.das.Pk;
import org.muguang.mybatisenhance.das.Tbl;

import java.util.Date;

@Data
@Tbl(value = "t_user")
public class User {

    @Pk(value = "id", seq = "seq_user_id")
    private Long id;
    private String name;
    private String password;
    private String email;
    private String phone;
    private String address;
    private String remark;
    private Integer status;
    private String lastLoginIp;
    private Date lastLoginTime;
    private Date createTime;
    private Date updateTime;
    private Date deleteTime;
    private Integer isDeleted;
}
