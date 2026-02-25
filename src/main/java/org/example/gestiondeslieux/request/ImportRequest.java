package org.example.gestiondeslieux.request;

import lombok.Data;
import org.example.gestiondeslieux.enums.ExportFormat;

import java.util.List;

@Data
public class ImportRequest {
    private String content;
    private ExportFormat format;
    private List<String> defaultTags;
    private boolean skipDuplicates = true;
}
