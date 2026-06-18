package com.streamconverter.command.rule;

/**
 * Per-request ライフサイクルを持つルールのマーカーインターフェース。
 *
 * <p>このインターフェースを実装した {@link IRule} は、{@code StreamConverter.run()} の呼び出しごとに
 * 新規インスタンスを要求する。インスタンス生成はファクトリ（ラムダまたは {@code Supplier<IRule>}）を経由する。
 *
 * <p>このインターフェースを実装しない {@link IRule} はデフォルトで<b>共有前提</b>となり、 スレッドセーフな実装が実装者の義務となる。
 *
 * <p>EXCEPTION_POLICY.md §4.1.3 参照。
 */
public interface PerRequestRule extends IRule {}
