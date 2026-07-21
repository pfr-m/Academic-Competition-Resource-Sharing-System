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

@Data                          // 自动生成所有 get/set/toString 等方法
@Builder                       // 支持链式编程 (例如 Resource.builder().title("xx").build())
@NoArgsConstructor             // 生成无参构造函数
@AllArgsConstructor            // 生成全参构造函数
@TableName("competition_resource")
public class Resource {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String contest;
    private String type;
    private String summary;
    private Long uploaderId;
    private String uploaderName;
    private String fileKey;
    private String originalFileName;
    private Long fileSize;
    private String contentType;
    private Integer downloadCount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}