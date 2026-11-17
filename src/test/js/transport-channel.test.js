import { JSDOM } from "jsdom";
import { expect, test } from "vitest";
import { readScript } from "./helpers/scripts.js";

test("transport bounds in-flight work, settles once, and never replays business packets", () => {
    const dom = new JSDOM("", { runScripts: "outside-only" });
    try {
        dom.window.eval(readScript("shared/transport-channel.js"));
        const requests = [],
            results = [];
        let aborts = 0;
        const transport = dom.window.createProtocolTransport(
            {
                send(packet, done) {
                    requests.push({ packet, done });
                    return () => {
                        aborts++;
                        done(null);
                    };
                }
            },
            { maxInFlight: 1 }
        );
        transport.connect();
        const channel = transport.channel({ token: "private", epoch: "boot", updateEpoch: "page" });
        channel.send({ events: [{ id: "#7" }], lastUpdate: "#3" }, value => results.push(value));
        channel.send({ events: [{ id: "#8" }] }, value => results.push(value));
        expect(results).toEqual([null]);
        expect(requests).toHaveLength(1);
        expect(requests[0].packet.query.lastUpdate).toBe("#3");
        channel.close();
        requests[0].done("late reply");
        transport.close();
        expect(aborts).toBe(1);
        expect(results).toEqual([null, null]);
        expect(transport.pending).toBe(0);
        expect(() => transport.connect()).toThrow();
    } finally {
        dom.window.close();
    }
});

test("multiplexed channels close independently and unsolicited delivery stops on close", () => {
    const dom = new JSDOM("", { runScripts: "outside-only" });
    try {
        dom.window.eval(readScript("shared/transport-channel.js"));
        let push;
        const requests = [],
            values = [],
            notifications = [];
        const transport = dom.window.createProtocolTransport({
            connect(deliver) {
                push = deliver;
            },
            send(packet, done) {
                requests.push({ packet, done });
                return () => done(null);
            }
        });
        transport.connect();
        transport.receive(value => notifications.push(value));
        const a = transport.channel({ updateEpoch: "a" });
        const b = transport.channel({ updateEpoch: "b" });
        a.send({ lastUpdate: "#1" }, value => values.push(["a", value]));
        b.send({ lastUpdate: "#9" }, value => values.push(["b", value]));
        a.close();
        requests[1].done("b reply");
        push({ page: "b", checkpoint: "#10" });
        transport.close();
        push({ page: "b", checkpoint: "#11" });
        expect(values).toEqual([
            ["a", null],
            ["b", "b reply"]
        ]);
        expect(notifications).toHaveLength(1);
        expect(requests[1].packet.channel.updateEpoch).toBe("b");
    } finally {
        dom.window.close();
    }
});
