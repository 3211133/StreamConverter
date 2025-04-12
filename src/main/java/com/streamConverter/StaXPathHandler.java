package com.streamConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO 本当は既存のライブラリを使うべき。 StaXpathクラスは、XPathを表すクラスです。 XPathはXMLドキュメント内のノードを選択するための言語です。
 * このクラスは、上から読み下すことを想定して、XPathの機能を一部省略して実装します。 フルパスの指定及び、部分パスを指定することができます。
 * XPathの一部を指定する場合は、XPathの先頭に"/"を付けてください。 フルパスの場合は、"/"を付ける必要はありません。 例:
 * 部分パスの場合は、省略する部分を"//"で指定してください。 例:
 */
public class StaXPathHandler {
  private List<String> targetXpath;
  private List<String> currentXpath;

  public StaXPathHandler(String xpath) {
    this.targetXpath = List.of(xpath.split("/"));
    this.currentXpath = new ArrayList<>();
  }

  /**
   * Xpathを取得します。
   *
   * @return Xpathの文字列
   */
  public String getTargetXpath() {
    return this.targetXpath.stream().reduce("", (acc, s) -> acc + "/" + s);
  }

  /** 現在のXpathをルートノードに移動します。 */
  public void moveToRoot() {
    this.currentXpath.clear();
  }

  /**
   * 子ノードに移動したXPathに変換します。
   *
   * @param child 子ノードの名前
   */
  public void moveToChild(String child) {
    this.currentXpath.add(child);
  }

  /** 親ノードに移動したXPathに変換します。 */
  public void moveToParent() {
    if (this.currentXpath.size() == 0) {
      return;
    }
    this.currentXpath.remove(this.currentXpath.size() - 1);
  }

  /**
   * 指定されたXPathがこのXPathと一致するかどうかを判定します。 省略された部分は"//"で指定されているものとします。 省略された部分があるパスとフルパスを一致させます。
   * example: "/a/b/c"と"/a/b/c/d"は一致しません。 "/a/b/c"と"/a/b//d"は一致します。 "/a/b/c"と"/a//c"は一致します。
   * "/a/b/c"と"/a//d"は一致しません。 "/a/b/c"と"/a//"は一致します。 "/a/b/c"と"//c"は一致します。 "/a/b/c"と"//"は一致します。
   * "/a/b/c/d"と"/a//d"は一致します。
   *
   * @return 一致する場合はtrue、それ以外はfalse
   */
  public boolean isTarget() {
    // TODO 省略された部分を考慮して一致判定を行う
    return this.targetXpath == this.currentXpath;
  }
}
