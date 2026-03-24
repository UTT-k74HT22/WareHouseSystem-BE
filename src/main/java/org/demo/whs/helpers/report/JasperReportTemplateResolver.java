package org.demo.whs.helpers.report;

import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class JasperReportTemplateResolver {

    private final ResourceLoader resourceLoader;
    private final Map<String, JasperReport> templateCache = new ConcurrentHashMap<>();

    public JasperReport resolve(String templatePath) {
        return templateCache.computeIfAbsent(templatePath, this::compileTemplate);
    }

    private JasperReport compileTemplate(String templatePath) {
        Resource resource = resourceLoader.getResource("classpath:" + templatePath);
        if (!resource.exists()) {
            throw new NotFoundException("Report template not found: " + templatePath, ErrorCode.COM_004);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return JasperCompileManager.compileReport(inputStream);
        } catch (IOException | JRException exception) {
            throw new IllegalStateException("Failed to compile Jasper template: " + templatePath, exception);
        }
    }
}
