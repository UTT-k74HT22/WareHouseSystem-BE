package org.demo.whs.utils.strategy.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
