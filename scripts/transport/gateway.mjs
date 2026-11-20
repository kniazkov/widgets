// R34 laboratory bridge only. Never imported by or shipped in the Java application.
import http from "node:http";
import { WebSocketServer } from "ws";

export async function gateway(backend) {
    const stats = {
        upgrades: 0,
        http: 0,
        frames: 0,
        frameBytes: 0,
        responseFrameBytes: 0,
        httpPost: 0,
        requestBodyBytes: 0,
        responseBodyBytes: 0,
        backendRequests: 0
    };
    const server = http.createServer((req, res) => {
        if (req.headers.origin && req.headers.origin !== origin) {
            res.writeHead(403).end();
            return;
        }
        stats.http++;
        if (req.method === "POST") {
            stats.httpPost++;
            req.on("data", data => {
                stats.requestBodyBytes += data.length;
            });
        }
        const target = new URL(req.url, backend);
        if (target.origin !== backend) {
            res.writeHead(400).end();
            return;
        }
        const headers = { ...req.headers, host: target.host };
        if (headers.origin) headers.origin = backend;
        const upstream = http.request(target, { method: req.method, headers }, response => {
            res.writeHead(response.statusCode, response.headers);
            if (req.method === "POST")
                response.on("data", data => {
                    stats.responseBodyBytes += data.length;
                });
            response.pipe(res);
        });
        upstream.on("error", () => {
            if (!res.headersSent) res.writeHead(502);
            res.end();
        });
        req.pipe(upstream);
    });
    const sockets = new WebSocketServer({
        noServer: true,
        maxPayload: 131072,
        perMessageDeflate: false,
        maxPendingAutoPongs: 16
    });
    server.on("upgrade", (req, socket, head) => {
        if (
            req.url !== "/__r34_socket" ||
            req.headers.origin !== origin ||
            sockets.clients.size >= 32
        ) {
            socket.end("HTTP/1.1 403 Forbidden\r\nConnection: close\r\n\r\n");
            return;
        }
        sockets.handleUpgrade(req, socket, head, ws => sockets.emit("connection", ws, req));
    });
    sockets.on("connection", (ws, req) => {
        stats.upgrades++;
        let inFlight = 0,
            alive = true;
        const ids = new Set(),
            controllers = new Set();
        const heartbeat = setInterval(() => {
            if (!alive) {
                ws.terminate();
                return;
            }
            alive = false;
            ws.ping();
        }, 10000);
        ws.on("pong", () => {
            alive = true;
        });
        ws.on("error", () => {});
        ws.on("close", () => {
            clearInterval(heartbeat);
            for (const c of controllers) c.abort();
        });
        ws.on("message", async (bytes, binary) => {
            if (binary || inFlight >= 16 || ws.bufferedAmount > 524288) {
                ws.close(1013);
                return;
            }
            let packet;
            try {
                packet = JSON.parse(bytes.toString());
                if (
                    !Number.isSafeInteger(packet.id) ||
                    packet.id < 1 ||
                    ids.has(packet.id) ||
                    !["new instance", "synchronize", "kill", "report error"].includes(
                        packet.query?.action
                    ) ||
                    typeof packet.auth?.csrf !== "string" ||
                    typeof packet.auth?.session !== "string" ||
                    packet.auth.csrf.length > 512 ||
                    packet.auth.session.length > 512
                )
                    throw new Error();
            } catch {
                ws.close(1008);
                return;
            }
            ids.add(packet.id);
            inFlight++;
            stats.frames++;
            stats.frameBytes += bytes.length;
            stats.backendRequests++;
            const controller = new AbortController();
            controllers.add(controller);
            const timeout = setTimeout(() => controller.abort(), 10000);
            try {
                const form = new FormData();
                for (const [key, value] of Object.entries(packet.query))
                    form.append(key, typeof value === "object" ? JSON.stringify(value) : value);
                const channel = packet.channel;
                const response = await fetch(backend, {
                    method: "POST",
                    body: form,
                    signal: controller.signal,
                    headers: {
                        Origin: backend,
                        Cookie: req.headers.cookie || "",
                        "X-Widgets-CSRF": packet.auth.csrf,
                        "X-Widgets-Session": packet.auth.session,
                        ...(channel
                            ? {
                                  "X-Widgets-Channel": channel.token,
                                  "X-Widgets-Epoch": channel.epoch,
                                  "X-Widgets-Update-Epoch": channel.updateEpoch || ""
                              }
                            : {})
                    }
                });
                const text = await response.text();
                const result = JSON.stringify({
                    id: packet.id,
                    status: response.status,
                    text,
                    headers: Object.fromEntries(
                        ["X-Widgets-Protocol-Version", "X-Widgets-Protocol-Error"].map(key => [
                            key,
                            response.headers.get(key)
                        ])
                    )
                });
                if (ws.bufferedAmount + Buffer.byteLength(result) > 524288) ws.close(1013);
                else if (ws.readyState === 1) {
                    stats.responseFrameBytes += Buffer.byteLength(result);
                    ws.send(result);
                }
            } catch {
                if (ws.readyState === 1) ws.send(JSON.stringify({ id: packet.id, status: 0 }));
            } finally {
                clearTimeout(timeout);
                controllers.delete(controller);
                ids.delete(packet.id);
                inFlight--;
            }
        });
    });
    await new Promise(resolve => server.listen(0, "127.0.0.1", resolve));
    const origin = `http://127.0.0.1:${server.address().port}`;
    return {
        origin,
        stats,
        disconnect() {
            for (const socket of sockets.clients) socket.terminate();
        },
        async close() {
            for (const socket of sockets.clients) socket.terminate();
            await new Promise(resolve => sockets.close(resolve));
            server.closeAllConnections();
            await new Promise(resolve => server.close(resolve));
        }
    };
}
