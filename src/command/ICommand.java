
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
/*
 * IOStream Command Interface
 * 
 * 
 */
public interface ICommand {
	
	void execute(InputStream in, OutputStream out) throws IOException;
	
}
