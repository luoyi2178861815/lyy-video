package com.lyy.aigc.constants;

public interface Constant {

    interface Tools{
        String QUERY_VIDEO_BY_ID = "根据视频ID查询视频详情（包括标题、作者、描述、时长、播放地址等）。当用户提到要查询某个视频、获取视频信息、或给出了具体的视频ID时，必须调用此方法。";
    }
    interface ToolParams{
        String VIDEO_ID = "要查询的视频的数字ID，例如：5、10、100";
    }
}
