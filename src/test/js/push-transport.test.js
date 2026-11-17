import { JSDOM } from "jsdom";
import { afterEach, expect, test, vi } from "vitest";
import { readScript } from "./helpers/scripts.js";

let dom;
afterEach(() => {
    dom?.window.close();
    vi.useRealTimers();
});
function fixture() {
    vi.useFakeTimers();
    dom = new JSDOM("", { runScripts: "outside-only", url: "https://example.test" });
    Object.assign(dom.window, {
        TextEncoder,
        setTimeout,
        clearTimeout,
        setInterval,
        clearInterval,
        Date
    });
    dom.window.transportRetryDelay = () => 20;
    dom.window.eval(readScript("shared/push-transport.js"));
    const sockets = [],
        http = [],
        values = [];
    const coordinator = dom.window.createPushCoordinator({
        url: "wss://example.test/widgets-stream",
        auth: () => ({ session: "session", csrf: "csrf" }),
        http(query, done, credentials) {
            http.push({ query, done, credentials });
            return () => done(null);
        },
        socketFactory() {
            const socket = {
                readyState: 1,
                bufferedAmount: 0,
                frames: [],
                send(text) {
                    this.frames.push(JSON.parse(text));
                },
                close() {
                    this.readyState = 3;
                    this.onclose?.();
                },
                receive(packet) {
                    this.onmessage({ data: JSON.stringify(packet) });
                }
            };
            sockets.push(socket);
            return socket;
        }
    });
    const register = id =>
        coordinator.register(
            id,
            () => ({ token: `secret-${id}`, epoch: "boot", updateEpoch: id }),
            () => values.push(`wake-${id}`)
        );
    const ready = () => {
        const socket = sockets.at(-1);
        socket.onopen();
        socket.receive({ type: "ready", version: 1 });
        return socket;
    };
    return { coordinator, sockets, http, values, register, ready };
}

test("one socket multiplexes independent channels and routes interleaved replies", () => {
    const f = fixture(),
        a = f.register("a"),
        b = f.register("b"),
        socket = f.ready();
    expect(f.sockets).toHaveLength(1);
    socket.receive({ type: "subscribed", client: "a" });
    socket.receive({ type: "subscribed", client: "b" });
    a.send({ action: "synchronize", client: "a", lastUpdate: "#5" }, data => f.values.push(data));
    b.send({ action: "synchronize", client: "b", lastUpdate: "#2" }, data => f.values.push(data));
    const requests = socket.frames.filter(frame => frame.type === "request");
    expect(requests.map(frame => frame.channel.token)).toEqual(["secret-a", "secret-b"]);
    socket.receive({ type: "response", id: requests[1].id, data: { lastEvent: "#7" } });
    socket.receive({ type: "response", id: requests[0].id, data: { lastEvent: "#9" } });
    expect(f.values.slice(-2)).toEqual(['{"lastEvent":"#7"}', '{"lastEvent":"#9"}']);
    a.dispose();
    expect(b.available()).toBe(true);
    expect(socket.frames.at(-1)).toEqual({ type: "unsubscribe", client: "a" });
    b.dispose();
    expect(socket.readyState).toBe(3);
    expect(f.coordinator.pending).toBe(0);
});

test("lost ACK settles once, HTTP fallback keeps original event IDs, old sockets are fenced", () => {
    const f = fixture(),
        a = f.register("a"),
        first = f.ready();
    first.receive({ type: "subscribed", client: "a" });
    const query = { action: "synchronize", client: "a", events: [{ id: "#4" }], lastUpdate: "#8" };
    a.send(query, data => f.values.push(data));
    const old = first.frames.find(frame => frame.type === "request");
    first.close();
    expect(f.values.filter(value => value === null)).toHaveLength(1);
    a.send(query, () => {});
    expect(f.http[0].query).toBe(query);
    vi.advanceTimersByTime(21);
    const second = f.ready();
    second.receive({ type: "subscribed", client: "a" });
    const count = f.values.length;
    first.receive({ type: "response", id: old.id, data: { unexpected: true } });
    expect(f.values).toHaveLength(count);
    expect(a.available()).toBe(true);
    f.coordinator.close();
    expect(f.coordinator.subscriptions).toBe(0);
});

test("large frames and large responses use HTTP without dropping checkpoints", () => {
    const f = fixture(),
        a = f.register("a"),
        socket = f.ready();
    socket.receive({ type: "subscribed", client: "a" });
    a.send({ client: "a", events: "x".repeat(70000), lastUpdate: "#2" }, () => {});
    expect(f.http).toHaveLength(1);
    expect(f.coordinator.pending).toBe(0);
    const query = { action: "synchronize", client: "a", lastUpdate: "#3" };
    a.send(query, value => f.values.push(value));
    socket.receive({ type: "http", id: socket.frames.at(-1).id });
    expect(f.http[1].query).toBe(query);
    expect(f.coordinator.pending).toBe(0);
    f.http[1].done("result");
    expect(f.values.at(-1)).toBe("result");
    f.coordinator.close();
});

test("dirty notifications stay page-local and idle traffic contains leases, not synchronize", () => {
    const f = fixture(),
        a = f.register("a"),
        socket = f.ready();
    f.register("b");
    socket.receive({ type: "subscribed", client: "a" });
    socket.receive({ type: "dirty", client: "b" });
    expect(f.values.at(-1)).toBe("wake-b");
    vi.advanceTimersByTime(5000);
    expect(socket.frames.at(-1)).toEqual({ type: "lease" });
    socket.receive({ type: "lease" });
    expect(socket.frames.filter(frame => frame.type === "request")).toHaveLength(0);
    vi.advanceTimersByTime(15250);
    expect(a.available()).toBe(false);
    f.coordinator.close();
});

test("request deadline releases pending operations and permanent disposal prevents reconnect", () => {
    const f = fixture(),
        a = f.register("a"),
        socket = f.ready();
    socket.receive({ type: "subscribed", client: "a" });
    a.send({ client: "a" }, value => f.values.push(value));
    vi.advanceTimersByTime(10001);
    expect(f.coordinator.pending).toBe(0);
    expect(f.values).toContain(null);
    f.coordinator.close();
    const count = f.sockets.length;
    vi.advanceTimersByTime(100000);
    expect(f.sockets).toHaveLength(count);
});
