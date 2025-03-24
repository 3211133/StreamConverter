
package com.streamConverter.command.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

import com.streamConverter.command.AbstractStreamCommand;

public class SampleStreamCommand extends AbstractStreamCommand {

    private String id;
    
    public SampleStreamCommand() {
        this.id = "default";
    }
    public SampleStreamCommand(String arg) {
        this.id = arg;
    }

    @Override
    public void _execute(InputStream inputStream, OutputStream outputStream) throws IOException {
        Objects.requireNonNull(inputStream);
        Objects.requireNonNull(outputStream);
        inputStream.transferTo(outputStream);
    }

    @Override
    public String toString() {
        return "SampleStreamCommand [id=" + id + "]";
    }

}