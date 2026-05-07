package com.streamconverter.path;

import java.util.List;

/**
 * 木構造ノードに対する反復マッチャー。
 *
 * <p>XML・JSON などの木構造を走査する際、ノードごとに呼び出されて現在のパスが 対象かどうかを判定する。走査のたびに {@link #matches(List)}
 * が呼ばれる反復呼び出し型。
 *
 * <p>このインターフェースは関数型インターフェースです。ラムダ式やメソッド参照で実装できます。
 *
 * <p><b>実装上の制約:</b> 実装はステートレスであること。また、渡された {@code currentPath} リストを変更してはならない。
 * 走査側が同一リストを再利用しながらパスを更新するため、リストの変更は走査状態の破壊につながる。
 *
 * <p>使用例:
 *
 * <pre>{@code
 * ITreeMatcher rootOnly = path -> path.isEmpty();
 * ITreeMatcher userNode = TreePath.fromJson("$.user.name");
 * }</pre>
 */
@FunctionalInterface
public interface ITreeMatcher {

  /**
   * 指定された現在パスがこのマッチャーの条件に一致するか判定する。
   *
   * @param currentPath 走査中の現在パス（ルートからのセグメントリスト）。null 不可。実装はこのリストを変更してはならない。
   * @return マッチする場合 true
   */
  boolean matches(List<String> currentPath);
}
