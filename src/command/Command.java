

public class Command implements ICommand {
    
    private String command;
    
    public Command(String command) {
        this.command = command;
    }
    
    @Override
    public void execute(InputStream in, OutputStream out) throws IOException {
        while (in.available() > 0)
            out.write( in.read());

    }
    
}