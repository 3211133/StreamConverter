package com.streamConverter.command.impl.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

import com.streamConverter.command.ConsumerCommand;

public class Validate extends ConsumerCommand {
    private String schema;

    public Validate(String schema) {
        this.schema = schema;
    }

    @Override
    public void consume(InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream);
        //InputStreamの消費先が２つになるため、出力先が二股のInputStreamを作成する
        //TODO InputStreamの二股処理を実装する

        

        // XMLのバリデーションを行う
        SchemaFactory  schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        Schema schema;
        try {
            schema = schemaFactory.newSchema(new File(this.schema));
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(inputStream));
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

    
}