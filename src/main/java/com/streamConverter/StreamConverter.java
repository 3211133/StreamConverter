package com.streamConverter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.SampleStreamCommand;

public class StreamConverter {
    private List<IStreamCommand> commands;
    StreamConverter() {
        this.commands = new ArrayList<>();
    }
    StreamConverter(IStreamCommand[] commands) {
        this.commands = new ArrayList<>();
        for (IStreamCommand command : commands) {
            this.commands.add(command);
        }
    }
    /**
     * 非同期並列処理でストリームを変換する。
     * @param inputStream
     * @param outputStream
    * @throws IOException 
    */ 
    void run(InputStream inputStream, OutputStream outputStream) throws IOException{
        //スレッドを生成
        ExecutorService executor = Executors.newFixedThreadPool(this.commands.size());
        List<Future<?>> futures = new ArrayList<>();
        //N+1回目のInputStreamの引数。N回目のOutputStream生成時に都度更新される。
        PipedOutputStream pipedOut = new PipedOutputStream();
        // 各コマンドを非同期で実行
        for (int i = 0; i < this.commands.size(); i++) {
            IStreamCommand command = this.commands.get(i);
    
            /* PipedInputStreamとPipedOutputStreamは、一組にする必要がある。
                * PipedInXとPipedOutXで一組にする
                * 
                * i 	InputStream	OutputStream
                * 0 	(引数の)in	PipedOut1
                * 1 	PipedIn1	PipedOut2
                * 2 	PipedIn2	PipedOut3
                * ...
                * N-1 	PipedInN1	PipedOutN
                * N 	PipedInN	(引数の)out
                */
            
            InputStream currentIn = (i == 0) ? inputStream : new PipedInputStream(pipedOut);
            OutputStream currentOut = (i == this.commands.size() - 1) ? outputStream : (pipedOut = new PipedOutputStream());
    
            //Thread実行。
            futures.add(executor.submit(() -> {
                try (OutputStream currentOut1 = currentOut;
                    InputStream currentIn1 = currentIn;){
                    command.execute(inputStream, outputStream);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } 
            }));
        }
        executor.shutdown();
        
        this.throwIfThrowed(futures);//Thread内で例外が投げられていたら投げる。
    }

    void addCommand(SampleStreamCommand sampleStreamCommand) {
        this.commands.add(sampleStreamCommand);
    }
    
    private void throwIfThrowed(List<Future<?>> futures) throws IOException {
    for (Future<?> future : futures) {
        try {
            future.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException ioe) {
                throw ioe;
            } else {//発生しないと思われる。
                throw new RuntimeException(cause);
            }
        } catch (InterruptedException e) {//発生しないと思われる。
            throw new RuntimeException(e);
        }
    }
}

}