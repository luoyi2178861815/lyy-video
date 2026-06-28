package com.lyy.aigc.constants;

public interface Constant {

    String REQUEST_ID = "requestId";

    interface Tools{
        String QUERY_VIDEO_BY_ID = "根据视频ID查询视频详细信息（包括标题、作者、描述、时长、播放地址等）。仅当用户明确给出了具体的视频数字ID（如：帮我查一下视频ID=1000的详情）时才调用此方法。如果用户只是通过关键词、人物名称、分类等描述性信息来找视频，不要调用此方法，应直接根据已知信息回答。";
    }
    interface ToolParams{
        String VIDEO_ID = "要查询的视频的数字ID，例如：5、10、100";
    }
}
