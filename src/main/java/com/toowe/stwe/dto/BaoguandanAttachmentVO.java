package com.toowe.stwe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaoguandanAttachmentVO {
    private List<AttachmentItem> attachments;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AttachmentItem {
        private String fileName;
        private String parsedContent;
        private String fileId;  // 附件ID，用于生成下载链接
    }
}
