package com.streamConverter;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Mainクラスのテスト")
class MainTest {
    
    private final PrintStream standardOut = System.out;
    private ByteArrayOutputStream outputStreamCaptor;
    
    @BeforeEach
    void setUp() {
        // 標準出力をキャプチャするための設定
        outputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStreamCaptor));
    }
    
    @AfterEach
    void tearDown() {
        // 標準出力を元に戻す
        System.setOut(standardOut);
    }
    
    @Test
    @DisplayName("main メソッドの実行テスト")
    void testMainMethod() throws Exception {
        // mainメソッドを実行
        Main.main(new String[]{});
        
        // 出力をキャプチャ
        String output = outputStreamCaptor.toString();
        
        // 期待される出力が含まれていることを確認
        assertTrue(output.contains("Hello World!"));
        assertTrue(output.contains("result:any message"));
        assertTrue(output.contains("Goodbye World!"));
    }
    
    @Test
    @DisplayName("引数付きmainメソッドの実行テスト")
    void testMainMethodWithArguments() throws Exception {
        // 引数付きでmainメソッドを実行
        String[] args = {"arg1", "arg2"};
        Main.main(args);
        
        // 出力をキャプチャ
        String output = outputStreamCaptor.toString();
        
        // 期待される出力が含まれていることを確認
        assertTrue(output.contains("Hello World!"));
        assertTrue(output.contains("result:any message"));
        assertTrue(output.contains("Goodbye World!"));
    }
}
