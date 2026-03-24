package org.demo.whs.utils.strategy.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * ExportPayload: Payload object containing all necessary information for exporting a report
 * NOTE: Đây là payload chung được truyền vào ExportStrategy để export file, nó chứa tất cả thông tin cần thiết cho việc export như tên file, header, dữ liệu rows, và các parameters khác nếu cần
 * Các ExportStrategy sẽ nhận ExportPayload này và dựa vào đó để generate file tương ứng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportPayload {

    private String baseFileName;

    @Builder.Default
    private List<String> headers = List.of();

    @Builder.Default
    private List<Map<String, Object>> rows = List.of();

    @Builder.Default
    private Map<String, Object> parameters = Map.of();
}
