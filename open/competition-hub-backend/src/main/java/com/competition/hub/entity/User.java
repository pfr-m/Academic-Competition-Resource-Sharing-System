package com.competition.hub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("competition_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String studentNo;
    private String username;
    private String passwordHash;
    private String realName;
    private String nickname;
    private String phone;
    private String email;
    private String wechat;
    private String skills;
    private String badges;
    private String role;
    private String realNameVisibility;
    private String phoneVisibility;
    private String wechatVisibility;
    private Integer firstLoginPrompted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}