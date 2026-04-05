package com.streamconverter.command.contract;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommandImplementationDiscoveryTest {

  private static final Path REPOSITORY_ROOT = Path.of("..").normalize();

  @Test
  void discoversTopLevelStreamCommandsAcrossModules() throws IOException {
    List<String> discovered =
        CommandImplementationDiscovery.discover(REPOSITORY_ROOT).stream()
            .map(CommandImplementationDiscovery.DiscoveredCommand::fqcn)
            .toList();

    assertAll(
        () ->
            assertTrue(
                discovered.contains("com.streamconverter.command.impl.SendHttpCommand"),
                "streamconverter-http の command 実装を検出できる必要がある"),
        () ->
            assertTrue(
                discovered.contains("com.streamconverter.pmd.command.PmdXmlToMarkdownCommand"),
                "streamconverter-tools の PMD command 実装を検出できる必要がある"),
        () ->
            assertTrue(
                discovered.contains("com.streamconverter.sloc.command.SlocAggregateCommand"),
                "streamconverter-tools の SLOC command 実装を検出できる必要がある"));
  }

  @Test
  void ignoresNonTopLevelAndAbstractCommandShapes() throws IOException {
    List<String> discovered =
        CommandImplementationDiscovery.discover(REPOSITORY_ROOT).stream()
            .map(CommandImplementationDiscovery.DiscoveredCommand::fqcn)
            .toList();

    assertAll(
        () ->
            assertFalse(
                discovered.contains("com.streamconverter.examples.PipelineBasicsExample"),
                "example 内の内部クラスをトップレベル command として誤検出してはいけない"),
        () ->
            assertFalse(
                discovered.contains("com.streamconverter.command.AbstractStreamCommand"),
                "抽象基底クラスを command 実装として扱ってはいけない"),
        () ->
            assertFalse(
                discovered.contains("com.streamconverter.command.ConsumerCommand"),
                "抽象 consumer 基底クラスを command 実装として扱ってはいけない"));
  }
}
