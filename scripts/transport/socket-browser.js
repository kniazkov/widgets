// Loaded explicitly by the R34 lab runner; absent from the production script manifest.
function createPrototypeSocketTransport() {
    let socket,
        next = 0;
    const replies = new Map();
    return createProtocolTransport(
        {
            connect(deliver, disconnected) {
                return new Promise((resolve, reject) => {
                    socket = new WebSocket(
                        location.origin.replace(/^http/, "ws") + "/__r34_socket"
                    );
                    socket.onopen = resolve;
                    socket.onerror = () => reject(new Error("Prototype socket unavailable"));
                    socket.onclose = () => {
                        reject(new Error("Prototype socket closed"));
                        disconnected();
                        replies.clear();
                    };
                    socket.onmessage = event => {
                        const response = JSON.parse(event.data);
                        const pending = replies.get(response.id);
                        if (!pending) return; // cancelled/old physical delivery; no ACK advancement
                        replies.delete(response.id);
                        pending.done(
                            classifyHttpResponse(pending.query, {
                                status: response.status,
                                responseText: response.text,
                                getResponseHeader: name => response.headers?.[name] ?? null
                            })
                        );
                    };
                });
            },
            send(packet, done) {
                if (packet.files?.length) throw new Error("Binary uploads use HTTP");
                if (socket.readyState !== WebSocket.OPEN || socket.bufferedAmount > 131072)
                    throw new Error("Socket backpressure");
                const id = ++next;
                replies.set(id, { query: packet.query, done });
                socket.send(
                    JSON.stringify({
                        id,
                        query: packet.query,
                        channel: packet.channel,
                        auth: { csrf: protocolCsrf, session: protocolSession }
                    })
                );
                return () => replies.delete(id);
            },
            close() {
                socket?.close();
                replies.clear();
            }
        },
        { maxInFlight: 16 }
    );
}
