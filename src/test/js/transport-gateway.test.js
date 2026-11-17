import { test, expect } from "vitest";
import { WebSocket } from "ws";
import { gateway } from "../../../scripts/transport/gateway.mjs";

test("lab gateway rejects foreign Origin, unauthenticated frames and oversized messages", async () => {
    const bridge = await gateway("http://127.0.0.1:1");
    const active = [];
    const connect = origin => {
        const ws = new WebSocket(bridge.origin.replace("http:", "ws:") + "/__r34_socket", {
            origin
        });
        active.push(ws);
        return ws;
    };
    try {
        const rejected = connect("https://untrusted.invalid");
        const error = await new Promise(resolve => rejected.once("error", resolve));
        expect(error.message).toContain("403");
        for (const [payload, code] of [
            ["{}", 1008],
            ["x".repeat(131073), 1009]
        ]) {
            const ws = connect(bridge.origin);
            await new Promise((resolve, reject) => {
                ws.once("open", resolve);
                ws.once("error", reject);
            });
            const closed = new Promise(resolve => ws.once("close", resolve));
            ws.send(payload);
            expect(await closed).toBe(code);
        }
        expect(bridge.stats.backendRequests).toBe(0);
    } finally {
        for (const ws of active) ws.terminate();
        await bridge.close();
    }
}, 10000);
