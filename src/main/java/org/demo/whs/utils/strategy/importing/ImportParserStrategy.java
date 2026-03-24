package org.demo.whs.utils.strategy.importing;

import org.demo.whs.entity.enums.ImportFileType;

import java.io.InputStream;

public interface ImportParserStrategy {

    ImportFileType getFileType();

    default boolean supports(ImportFileType fileType) {
        return getFileType() == fileType;
    }

    ImportParseResult parse(InputStream inputStream);
}
