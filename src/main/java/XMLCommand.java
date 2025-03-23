public class XMLCommand extends ICommand {
    
    private String command;
    
    
    
    @Override
    public void run(InputStream in, OutputStream out) throws IOException {
        XMLParser parser = new XMLParser();
        parser.parse(in);
        
        while (in.available() > 0)
            out.write( in.read());

    }

}
