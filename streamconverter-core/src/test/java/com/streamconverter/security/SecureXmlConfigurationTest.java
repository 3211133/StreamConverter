package com.streamconverter.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.xml.stream.XMLOutputFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** SecureXmlConfiguration の XMLOutputFactory 生成メソッドのテスト */
class SecureXmlConfigurationTest {

  @Test
  @DisplayName("createSecureXMLOutputFactory が null でない XMLOutputFactory を返す")
  void createSecureXMLOutputFactory_returnsNonNull() {
    XMLOutputFactory factory = SecureXmlConfiguration.createSecureXMLOutputFactory();
    assertNotNull(factory);
  }

  @Test
  @DisplayName("createSecureXMLOutputFactory が IS_REPAIRING_NAMESPACES=false の設定で返す")
  void createSecureXMLOutputFactory_repairingNamespacesDisabled() {
    XMLOutputFactory factory = SecureXmlConfiguration.createSecureXMLOutputFactory();
    Object value = factory.getProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES);
    assertFalse(
        Boolean.TRUE.equals(value),
        "IS_REPAIRING_NAMESPACES should be false to prevent namespace auto-repair");
  }
}
