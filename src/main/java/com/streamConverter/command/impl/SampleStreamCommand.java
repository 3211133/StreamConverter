
package com.streamConverter.command.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.logging.Logger;

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
    public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
        Objects.requireNonNull(inputStream);
        Objects.requireNonNull(outputStream);

        // TODO ロガーをslf4j/logbackに変更する
        // TODO 開始・終了ログを外部化する
        Logger.getGlobal().info("SampleStreamCommand start:"+this.id);
        inputStream.transferTo(outputStream);
        Logger.getGlobal().info("SampleStreamCommand end:"+this.id);
    }

    @Override
    public String toString() {
        return "SampleStreamCommand [id=" + id + "]";
    }

}