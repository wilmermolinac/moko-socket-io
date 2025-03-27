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
        transport: SocketIoTransport
    ) {
        var configuration: SocketIOClientConfiguration = [.compress]

        if let queryParams = queryParams {
            configuration.insert(.connectParams(queryParams))
        }

        switch transport {
        case .websocket:
            configuration.insert(.forceWebsockets(true))
        case .polling:
            configuration.insert(.forcePolling(true))
        case .undefined:
            // do nothing
            break
        }

        socketManager = SocketManager(socketURL: URL(string: endpoint)!,
                                      config: configuration)
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
    public func on(event: String, action: @escaping (String) -> Void) {
        // FIXME: Actualmente parseamos la data devuelta por socketIO,
        // la transformamos en JSON string y la devolvemos al callback.
        socket.on(event) { data, emitter in
            guard !data.isEmpty else {
                action("") // o un "{}" si prefieres
                return
            }

            // data[0] podría ser un objeto, un string, etc
            // Asumamos que es un objeto o array.
            do {
                let jsonData = try JSONSerialization.data(withJSONObject: data[0], options: [])
                let jsonString = String(data: jsonData, encoding: .utf8) ?? ""
                action(jsonString)
            } catch {
                print("JSONSerialization error: \(error)")
                action("")
            }
        }
    }

    @objc
    public func on(socketEvent: SocketEvent, action: @escaping ([Any]) -> Void) {
        let clientEvent: SocketClientEvent

        switch socketEvent {
        case .connect:
            clientEvent = .connect
        case .error:
            clientEvent = .error
        case .message:
            // Para .message no existe un clientEvent directo;
            // Usamos onAny y verificamos "message"
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
            // No existe un .connecting exacto, así que monitoreamos
            // statusChange y verificamos si es .connecting
            socket.on(clientEvent: .statusChange) { [weak self] data, _ in
                if self?.socket.status == .connecting {
                    action(data)
                }
            }
            return
        default:
            // Por si agregas en el enum algo que no está mapeado
            return
        }

        socket.on(clientEvent: clientEvent) { data, _ in
            action(data)
        }
    }

    @objc
    public func emit(event: String, data: [Any]) {
        // Transformamos cada elemento del array si es JSON en string
        var result = [Any]()

        for item in data {
            if let jsonString = item as? String,
               let itemData = jsonString.data(using: .utf8)
            {
                do {
                    // parseamos el string como JSON
                    // itemObject puede ser [String: Any], [Any], etc
                    let itemObject = try JSONSerialization.jsonObject(with: itemData, options: [])
                    result.append(itemObject)
                } catch {
                    print("emit parsing error: \(error.localizedDescription)")
                    // Si falla el parse, lo mandamos tal cual
                    result.append(jsonString)
                }
            } else {
                result.append(item)
            }
        }

        // Llamamos a la nueva versión de emit
        // Nota: 'result' es [Any], pero socket.emit requiere [SocketData].
        // Si no quieres castear, podrías dejar 'result' como [SocketData].
        // Para eso, deberías garantizar que los tipos de 'result' sean conformes a 'SocketData'.
        // En la práctica, String, Int, Dictionary, Array, etc. ya conforman SocketData.
        // Pero con 'Any' a lo mejor no todos. Podríamos forzar un cast.

        // Si no deseas el completion, omítelo (usa la versión con nil)
        socket.emit(event, with: result as! [SocketData]) // Forzamos cast, asumiendo que todos son SocketData
    }

    @objc
    public func emit(event: String, string: String) {
        // Simplemente emitimos el string
        socket.emit(event, with: [string]) // con completion por defecto = nil
    }
}
