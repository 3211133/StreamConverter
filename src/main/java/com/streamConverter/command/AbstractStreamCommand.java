package com.streamConverter.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class AbstractStreamCommand implements IStreamCommand {
    public AbstractStreamCommand() {
        super();
    }
    //TODO execute以外の共通処理を実装する
    public abstract void execute(InputStream inputStream, OutputStream outputStream) throws IOException;

}
