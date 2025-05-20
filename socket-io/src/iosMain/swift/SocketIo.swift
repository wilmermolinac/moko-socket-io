/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

import SocketIO

@objc
public enum SocketIoTransport: Int {
  case websocket
  case polling
  case undefined
}

@objc
public enum SocketEvent: Int {
  case connect
  case connecting
  case disconnect
  case error
  case message
  case reconnect
  case reconnectAttempt
  case ping
  case pong
}

@objc
public class SocketIo: NSObject {
  private let socketManager: SocketManager
  private let socket: SocketIOClient

  @objc
  public init(
    endpoint: String,
    queryParams: [String: Any]?,
    transport: SocketIoTransport,
    log: Bool = false,
    reconnects: Bool = true,
    reconnectAttempts: Int = -1,
    reconnectWait: Int = 10
  ) {
    var configuration: SocketIOClientConfiguration = [.compress]

    if let queryParams = queryParams {
      configuration.insert(.connectParams(queryParams))
    }

    // Set logging
    if log {
      configuration.insert(.log(true))
    }

    // Set reconnection parameters
    configuration.insert(.reconnects(reconnects))
    configuration.insert(.reconnectAttempts(reconnectAttempts))
    configuration.insert(.reconnectWait(Double(reconnectWait)))

    // Set transport type
    switch transport {
    case .websocket:
      configuration.insert(.forceWebsockets(true))
    case .polling:
      configuration.insert(.forcePolling(true))
    case .undefined: break
    }

    socketManager = SocketManager(socketURL: URL(string: endpoint)!, config: configuration)
    socket = socketManager.defaultSocket
  }

  @objc
  public func connect() {
    socket.connect()
  }

  @objc
  public func disconnect() {
    socket.disconnect()
  }

  @objc
  public func isConnected() -> Bool {
    return socket.status == .connected
  }

  @objc
  public func status() -> String {
    switch socket.status {
    case .connected:
      return "connected"
    case .connecting:
      return "connecting"
    case .disconnected:
      return "disconnected"
    case .notConnected:
      return "notConnected"
    }
  }

  @objc
  public func on(event: String, action: @escaping (String) -> Void) {
    socket.on(event) { data, emitter in
      if let firstData = data.first {
        do {
          let jsonData = try JSONSerialization.data(withJSONObject: firstData, options: .prettyPrinted)
          let jsonString = String(data: jsonData, encoding: .utf8) ?? "{}"
          action(jsonString)
        } catch {
          print("Error serializing data: \(error.localizedDescription)")
          action("{}")
        }
      } else {
        action("{}")
      }
    }
  }

  @objc
  public func on(event: String, callback: @escaping ([Any], @escaping ([Any]) -> Void) -> Void) {
    socket.on(event) { data, ack in
      callback(data, ack.with)
    }
  }

  @objc
  public func on(socketEvent: SocketEvent, action: @escaping (Array<Any>) -> Void) {
    let clientEvent: SocketClientEvent

    switch socketEvent {
    case .connect:
      clientEvent = .connect
    case .error:
      clientEvent = .error
    case .message:
      socket.onAny { anyEvent in
        if let data = anyEvent.items {
          action(data)
        } else {
          action([])
        }
      }
      return
    case .disconnect:
      clientEvent = .disconnect
    case .reconnect:
      clientEvent = .reconnect
    case .reconnectAttempt:
      clientEvent = .reconnectAttempt
    case .ping:
      clientEvent = .ping
    case .pong:
      clientEvent = .pong
    case .connecting:
      socket.on(clientEvent: .statusChange) { [weak self] data, _ in
        if self?.socket.status == .connecting {
          action(data)
        }
      }
      return
    }

    socket.on(clientEvent: clientEvent) { data, _ in
      action(data)
    }
  }

  @objc
  public func emit(event: String, data: Array<Any>) {
    var transformedData = [Any]()

    for item in data {
      if let stringItem = item as? String, let itemData = stringItem.data(using: .utf8) {
        do {
          if let itemObject = try JSONSerialization.jsonObject(with: itemData, options: []) as? [String: Any] {
            transformedData.append(itemObject)
          } else {
            transformedData.append(item)
          }
        } catch {
          print("Error parsing JSON string: \(error.localizedDescription)")
          transformedData.append(item)
        }
      } else {
        transformedData.append(item)
      }
    }

    socket.emit(event, with: transformedData)
  }

  @objc
  public func emit(event: String, string: String) {
    socket.emit(event, with: [string])
  }

  @objc
  public func emitWithAck(event: String, data: Any, timeout: TimeInterval = 0, callback: @escaping (Array<Any>) -> Void) {
    let transformedData: Any

    if let stringData = data as? String, let itemData = stringData.data(using: .utf8) {
      do {
        if let itemObject = try JSONSerialization.jsonObject(with: itemData, options: []) as? [String: Any] {
          transformedData = itemObject
        } else {
          transformedData = data
        }
      } catch {
        print("Error parsing JSON string: \(error.localizedDescription)")
        transformedData = data
      }
    } else {
      transformedData = data
    }

    socket.emitWithAck(event, transformedData).timingOut(after: timeout) { ackData in
      callback(ackData)
    }
  }

  @objc
  public func emitWithAck(event: String, data: Array<Any>, timeout: TimeInterval = 0, callback: @escaping (Array<Any>) -> Void) {
    var transformedData = [Any]()

    for item in data {
      if let stringItem = item as? String, let itemData = stringItem.data(using: .utf8) {
        do {
          if let itemObject = try JSONSerialization.jsonObject(with: itemData, options: []) as? [String: Any] {
            transformedData.append(itemObject)
          } else {
            transformedData.append(item)
          }
        } catch {
          print("Error parsing JSON string: \(error.localizedDescription)")
          transformedData.append(item)
        }
      } else {
        transformedData.append(item)
      }
    }

    socket.emitWithAck(event, with: transformedData).timingOut(after: timeout) { ackData in
      callback(ackData)
    }
  }

  @objc
  public func removeAllHandlers() {
    socket.removeAllHandlers()
  }

  @objc
  public func off(event: String) {
    socket.off(event)
  }
}