package com.streamConverter.command.impl.charaCode;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Objects;

import com.streamConverter.command.AbstractStreamCommand;

public class convert extends AbstractStreamCommand {
    private String from;
    private String to;

    public convert(String from, String to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
        Objects.requireNonNull(inputStream);
        Objects.requireNonNull(outputStream);

        // 文字コードを変換する
        try (
                InputStreamReader reader = new InputStreamReader(inputStream, this.from);
                OutputStreamWriter writer = new OutputStreamWriter(outputStream, this.to);) {
            reader.transferTo(writer);
        }
    }

}
