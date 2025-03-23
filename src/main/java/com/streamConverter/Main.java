package com.streamConverter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.impl.SampleStreamCommand;

public class Main {

    public static void main(String[] args) throws IOException{
        System.out.println("Hello World!");
        IStreamCommand[] commands = {
            new SampleStreamCommand("0"),
            new SampleStreamCommand("1"),
            new SampleStreamCommand("2")
        };
        StreamConverter converter = new StreamConverter(commands);
        try(InputStream inputStream = new ByteArrayInputStream("any message".getBytes());
            OutputStream outputStream = new ByteArrayOutputStream()) {
            converter.run(inputStream, outputStream);
            System.out.println("result:"+outputStream.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        System.out.println("Goodbye World!");
    }          
}
