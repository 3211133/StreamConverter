
package com.streamConverter.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

public class SampleStreamCommand implements IStreamCommand {

    private String id;
    
    public SampleStreamCommand() {
        this.id = "default";
    }
    public SampleStreamCommand(String arg) {
        this.id = arg;
    }

    @Override
    public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
        Logger.getGlobal().info("SampleStreamCommand start:"+this.id);
        while(inputStream.available() > 0) {
            inputStream.transferTo(outputStream);
        }
        Logger.getGlobal().info("SampleStreamCommand end:"+this.id);
    }

}