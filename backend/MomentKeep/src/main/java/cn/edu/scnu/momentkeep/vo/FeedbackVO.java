package cn.edu.scnu.momentkeep.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FeedbackVO {
    
    private Long id;
    private String content;
    private String contact;
    private Integer status;
    private String reply;
    private LocalDateTime createTime;
}
