package com.streamConverter.command.impl.xml;

import com.streamConverter.command.ConsumerCommand;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.SAXException;

/**
 * XMLのバリデーションを行うコマンドクラス
 *
 * <p>XMLのバリデーションを行うコマンドクラスです。
 *
 * <p>このクラスは、XMLのスキーマを指定して、XMLのバリデーションを行います。
 *
 * <p>バリデーションエラーが発生した場合は、エラーメッセージを出力します。
 */
public class ValidateCommand extends ConsumerCommand {
  private String schema;

  /**
   * コンストラクタ
   *
   * <p>XMLのスキーマを指定して、XMLのバリデーションを行います。
   *
   * @param schema XMLのスキーマ
   */
  public ValidateCommand(String schema) {
    this.schema = schema;
  }

  /**
   * XMLのバリデーションを行うコマンドを実行します。
   *
   * <p>XMLのスキーマを指定して、XMLのバリデーションを行います。
   *
   * <p>バリデーションエラーが発生した場合は、エラーメッセージを出力します。
   *
   * @param inputStream 入力ストリーム
   * @throws IOException 入出力エラーが発生した場合
   */
  @Override
  public void consume(InputStream inputStream) throws IOException {
    Objects.requireNonNull(inputStream);

    // XMLのバリデーションを行う
    SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
    Schema schema;
    try {
      schema = schemaFactory.newSchema(new File(this.schema));
      Validator validator = schema.newValidator();
      validator.validate(new StreamSource(inputStream));
    } catch (SAXException e) {
      // バリデーションエラー時の処理を実装する
      // バリデーションエラーが発生した原因を取得する
      String errorMessage = e.getMessage();
      // エラーの詳細を出力する
      System.err.println("XML Validation Error: " + errorMessage);
      // TODO XMLのバリデーションエラー時の処理を実装する
      // 後続のCommandにエラー情報を渡す

      // e.printStackTrace();
    }
  }
}
