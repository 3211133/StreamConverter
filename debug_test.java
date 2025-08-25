import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.impl.json.JsonNavigateCommand;
import com.streamConverter.command.rule.impl.casing.CamelToSnakeCaseRule;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class debug_test {
    public static void main(String[] args) {
        try {
            String inputJson = "{\"userName\": \"john_doe\", \"firstName\": \"John\"}";
            
            IStreamCommand command = 
                JsonNavigateCommand.createExtractValue("$.userName", CamelToSnakeCaseRule.create());
            
            ByteArrayInputStream inputStream = 
                new ByteArrayInputStream(inputJson.getBytes(StandardCharsets.UTF_8));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            command.execute(inputStream, outputStream);
            
            String result = outputStream.toString(StandardCharsets.UTF_8);
            System.out.println("Result: '" + result + "'");
            System.out.println("Expected: 'john_doe'");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}