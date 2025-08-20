import com.streamConverter.controller.PmdAnalysisController;
import java.io.*;
import java.util.List;
import com.streamConverter.CommandResult;

public class TestPmdConverter {
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
        System.out.println("🔄 Testing Markdown conversion...");
        PmdAnalysisController controller = PmdAnalysisController.forMarkdownConversion();
        
        try (FileInputStream input = new FileInputStream(xmlPath);
             FileOutputStream output = new FileOutputStream("test-output.md")) {
            
            List<CommandResult> results = controller.process(input, output);
            System.out.println("✅ Markdown conversion successful: " + results.get(0));
        } catch (Exception e) {
            System.err.println("❌ Markdown conversion failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testCsvConversion(String xmlPath) throws IOException {
        System.out.println("🔄 Testing CSV conversion...");
        PmdAnalysisController controller = PmdAnalysisController.forCsvConversion();
        
        try (FileInputStream input = new FileInputStream(xmlPath);
             FileOutputStream output = new FileOutputStream("test-output.csv")) {
            
            List<CommandResult> results = controller.process(input, output);
            System.out.println("✅ CSV conversion successful: " + results.get(0));
        } catch (Exception e) {
            System.err.println("❌ CSV conversion failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testJsonConversion(String xmlPath) throws IOException {
        System.out.println("🔄 Testing JSON conversion...");
        PmdAnalysisController controller = PmdAnalysisController.forJsonConversion();
        
        try (FileInputStream input = new FileInputStream(xmlPath);
             FileOutputStream output = new FileOutputStream("test-output.json")) {
            
            List<CommandResult> results = controller.process(input, output);
            System.out.println("✅ JSON conversion successful: " + results.get(0));
        } catch (Exception e) {
            System.err.println("❌ JSON conversion failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}