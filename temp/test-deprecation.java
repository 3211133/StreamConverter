import com.streamConverter.command.impl.CsvNavigateCommand;
import com.streamConverter.command.impl.JsonNavigateCommand;
import com.streamConverter.command.impl.XmlNavigateCommand;

public class TestDeprecation {
    public void testDeprecatedConstructors() {
        // These should generate deprecation warnings
        CsvNavigateCommand csv1 = new CsvNavigateCommand("name");
        CsvNavigateCommand csv2 = new CsvNavigateCommand();
        JsonNavigateCommand json1 = new JsonNavigateCommand("$.path");
        JsonNavigateCommand json2 = new JsonNavigateCommand();
        XmlNavigateCommand xml1 = new XmlNavigateCommand("//element");
        XmlNavigateCommand xml2 = new XmlNavigateCommand();
        
        // These should NOT generate deprecation warnings
        CsvNavigateCommand csv3 = CsvNavigateCommand.extractOnly("name");
        CsvNavigateCommand csv4 = CsvNavigateCommand.extractAll();
        JsonNavigateCommand json3 = JsonNavigateCommand.extractOnly("$.path");
        JsonNavigateCommand json4 = JsonNavigateCommand.extractAll();
        XmlNavigateCommand xml3 = XmlNavigateCommand.extractOnly("//element");
        XmlNavigateCommand xml4 = XmlNavigateCommand.extractAll();
    }
}