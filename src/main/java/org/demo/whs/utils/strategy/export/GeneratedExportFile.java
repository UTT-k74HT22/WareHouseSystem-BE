package org.demo.whs.utils.strategy.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GeneratedExportFile: Object representing the generated export file
 * NOTE: Đây là object đại diện cho file đã được generate sau khi export, nó chứa tên file, định dạng content type, phần mở rộng file, và nội dung file dưới dạng byte array
 * Các ExportStrategy sẽ trả về GeneratedExportFile này sau khi export thành công, và phần lưu file sẽ do một component khác xử lý chung cho tất cả các loại báo cáo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedExportFile {

    private String fileName;
    private String contentType;
    private String extension;
    private byte[] content;
}
