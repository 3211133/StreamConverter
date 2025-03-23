package com.streamConverter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.streamConverter.command.SampleStreamCommand;

public class Main {

    public static void main(String[] args) throws IOException{
        System.out.println("Hello World!");
        StreamConverter converter = new StreamConverter();
        converter.addCommand(new SampleStreamCommand("0"));
        converter.addCommand(new SampleStreamCommand("1"));
        converter.addCommand(new SampleStreamCommand("2"));
        try(InputStream inputStream = new ByteArrayInputStream("any message".getBytes());
                OutputStream outputStream = new ByteArrayOutputStream()) {
            converter.run(inputStream, outputStream);
            System.out.println("result:"+outputStream.toString());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        System.out.println("Goodbye World!");
    }          
}
