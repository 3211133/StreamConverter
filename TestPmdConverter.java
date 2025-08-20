import com.streamConverter.controller.PmdAnalysisController;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import com.streamConverter.CommandResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestPmdConverter {
    private static final Logger LOG = LoggerFactory.getLogger(TestPmdConverter.class);
    public static void main(String[] args) throws IOException {
        // PMD XMLファイルをテスト
        String xmlPath = "build/reports/pmd/main.xml";
        
        // Markdownテスト
        testMarkdownConversion(xmlPath);
        
        // CSVテスト
        testCsvConversion(xmlPath);
        
        // JSONテスト  
        testJsonConversion(xmlPath);
    }
    
    private static void testMarkdownConversion(String xmlPath) throws IOException {
        if (LOG.isInfoEnabled()) {
            LOG.info("🔄 Testing Markdown conversion...");
        }
        PmdAnalysisController controller = PmdAnalysisController.forMarkdownConversion();
        
        try (var input = Files.newInputStream(Paths.get(xmlPath));
             var output = Files.newOutputStream(Paths.get("test-output.md"))) {
            
            List<CommandResult> results = controller.process(input, output);
            if (LOG.isInfoEnabled()) {
                LOG.info("✅ Markdown conversion successful: {}", results.get(0));
            }
        } catch (Exception e) {
            LOG.error("❌ Markdown conversion failed: {}", e.getMessage(), e);
        }
    }
    
    private static void testCsvConversion(String xmlPath) throws IOException {
        if (LOG.isInfoEnabled()) {
            LOG.info("🔄 Testing CSV conversion...");
        }
        PmdAnalysisController controller = PmdAnalysisController.forCsvConversion();
        
        try (var input = Files.newInputStream(Paths.get(xmlPath));
             var output = Files.newOutputStream(Paths.get("test-output.csv"))) {
            
            List<CommandResult> results = controller.process(input, output);
            if (LOG.isInfoEnabled()) {
                LOG.info("✅ CSV conversion successful: {}", results.get(0));
            }
        } catch (Exception e) {
            LOG.error("❌ CSV conversion failed: {}", e.getMessage(), e);
        }
    }
    
    private static void testJsonConversion(String xmlPath) throws IOException {
        if (LOG.isInfoEnabled()) {
            LOG.info("🔄 Testing JSON conversion...");
        }
        PmdAnalysisController controller = PmdAnalysisController.forJsonConversion();
        
        try (var input = Files.newInputStream(Paths.get(xmlPath));
             var output = Files.newOutputStream(Paths.get("test-output.json"))) {
            
            List<CommandResult> results = controller.process(input, output);
            if (LOG.isInfoEnabled()) {
                LOG.info("✅ JSON conversion successful: {}", results.get(0));
            }
        } catch (Exception e) {
            LOG.error("❌ JSON conversion failed: {}", e.getMessage(), e);
        }
    }
}