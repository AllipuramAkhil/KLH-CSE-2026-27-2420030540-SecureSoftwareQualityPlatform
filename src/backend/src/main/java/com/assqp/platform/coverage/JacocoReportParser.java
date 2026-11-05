package com.assqp.platform.coverage;

import com.assqp.platform.exception.FileStorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Parses a JaCoCo XML coverage report (target/site/jacoco/jacoco.xml) into
 * a {@link JacocoCoverageResult}.
 *
 * JaCoCo's XML report references an external DTD
 * (https://www.jacoco.org/jacoco/trunk/coverage/report.dtd). We explicitly
 * disable external DTD loading and validation so this works in offline /
 * network-restricted environments (e.g. CI runners with no outbound
 * access, or this platform's own sandboxed deployments) and so a
 * malicious report file can't be used for XXE / SSRF.
 */
@Slf4j
@Component
public class JacocoReportParser {

    public JacocoCoverageResult parse(Path reportPath) {
        if (!Files.exists(reportPath)) {
            throw new FileStorageException("JaCoCo report not found at: " + reportPath.toAbsolutePath());
        }
        try (InputStream in = Files.newInputStream(reportPath)) {
            return parse(in);
        } catch (IOException ex) {
            throw new FileStorageException("Failed to read JaCoCo report at " + reportPath, ex);
        }
    }

    public JacocoCoverageResult parse(InputStream xmlInputStream) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setValidating(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlInputStream);
            Element report = doc.getDocumentElement();

            if (report == null || !"report".equals(report.getNodeName())) {
                throw new FileStorageException("File does not look like a JaCoCo report (missing <report> root element)");
            }

            int[] instruction = readCounter(report, "INSTRUCTION");
            int[] branch = readCounter(report, "BRANCH");
            int[] line = readCounter(report, "LINE");
            int[] clazz = readCounter(report, "CLASS");

            return new JacocoCoverageResult(
                    percent(line[0], line[1]),
                    percent(branch[0], branch[1]),
                    percent(instruction[0], instruction[1]),
                    line[0], line[1],
                    branch[0], branch[1],
                    clazz[0], clazz[1]
            );
        } catch (FileStorageException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to parse JaCoCo XML report", ex);
            throw new FileStorageException("Failed to parse JaCoCo XML report: " + ex.getMessage(), ex);
        }
    }

    /**
     * Reads the covered/missed counts from the direct-child &lt;counter
     * type="..."&gt; element of the report root, i.e. the project-wide
     * total (JaCoCo also emits per-package/per-class counters nested
     * deeper, which we intentionally ignore here).
     * Returns {covered, missed}, defaulting to {0, 0} if the counter type
     * is absent (e.g. no branches in a trivial project).
     */
    private int[] readCounter(Element report, String type) {
        NodeList children = report.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;
            Element el = (Element) node;
            if ("counter".equals(el.getNodeName()) && type.equals(el.getAttribute("type"))) {
                int covered = parseIntSafe(el.getAttribute("covered"));
                int missed = parseIntSafe(el.getAttribute("missed"));
                return new int[]{covered, missed};
            }
        }
        return new int[]{0, 0};
    }

    private int parseIntSafe(String value) {
        try {
            return value == null || value.isBlank() ? 0 : Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private double percent(int covered, int missed) {
        int total = covered + missed;
        if (total == 0) return 0.0;
        return Math.round((covered * 10000.0) / total) / 100.0;
    }
}
