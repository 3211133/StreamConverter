package com.streamconverter.path;

/**
 * 最小限のPath基底クラス
 *
 * <p>Don't Ask, Tell原則に従い、matches()メソッドのみを提供する最小実装
 *
 * @param <T> コンテキスト型
 */
public abstract class AbstractPath<T> implements IPath<T> {

  /**
   * AbstractPathのコンストラクタ
   *
   * @param path パス文字列（構築時に検証される）
   */
  protected AbstractPath(String path) {
    validateAndNormalize(path);
  }

  /**
   * パス文字列の検証と正規化
   *
   * @param rawPath 生のパス文字列
   * @throws IllegalArgumentException 不正なパスの場合
   */
  protected abstract void validateAndNormalize(String rawPath);

  /**
   * 文字列がnullまたは空かチェック
   *
   * @param str チェック対象文字列
   * @return nullまたは空の場合true
   */
  protected static boolean isNullOrEmpty(String str) {
    return str == null || str.trim().isEmpty();
  }
}
