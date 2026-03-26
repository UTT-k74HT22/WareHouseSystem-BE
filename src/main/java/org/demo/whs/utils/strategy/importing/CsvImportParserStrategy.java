package org.demo.whs.utils.strategy.importing;

import org.demo.whs.entity.enums.ImportFileType;
import org.demo.whs.exception.NotImplementedException;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class CsvImportParserStrategy implements ImportParserStrategy {

    @Override
    public ImportFileType getFileType() {
        return ImportFileType.CSV;
    }

    @Override
    public ImportParseResult parse(InputStream inputStream) {
        throw new NotImplementedException("CSV import parser is scaffolded but not implemented yet");
    }
}
