package com.streamconverter.command.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;

interface InetAddressResolver {
  InetAddress[] getAllByName(String host) throws UnknownHostException;
}
