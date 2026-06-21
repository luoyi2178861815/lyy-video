package com.lyy.common.dto;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serial;
import java.io.Serializable;
@Data
@NoArgsConstructor
public class LikeIncrementMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long userId;
    private Long videoId;
    private Integer increment;

    // 工厂：视频点赞数增量
    public static LikeIncrementMessage buildVideoMsg(Long videoId, Integer increment) {
        LikeIncrementMessage msg = new LikeIncrementMessage();
        msg.videoId = videoId;
        msg.increment = increment;
        return msg;
    }

    // 工厂：用户点赞数增量
    public static LikeIncrementMessage buildUserMsg(Long userId, Integer increment) {
        LikeIncrementMessage msg = new LikeIncrementMessage();
        msg.userId = userId;
        msg.increment = increment;
        return msg;
    }
}
