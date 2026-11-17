import assert from "node:assert/strict";
import { Buffer } from "node:buffer";
import { setTimeout as delay } from "node:timers/promises";
import { pool } from "./stats.mjs";

export const names = ["polling", "burst", "large-tree", "slow-client", "uploads", "history"];
export async function profile(name, count, api, concurrency, observe) {
    const details = {
        logicalClients: count,
        peakLiveLimit:
            name === "large-tree" || name === "uploads" || name === "history" ? 16 : count,
        serverPageLimit:
            name === "history"
                ? Math.min(16, count) * 4
                : name === "large-tree" || name === "uploads"
                  ? Math.min(16, count)
                  : count + 1,
        businessActions: 0,
        duplicateBatches: 0,
        resyncs: 0,
        busyReplies: 0
    };
    async function createMany(n, address = "/load") {
        const clients = Array(n);
        await pool(clients, concurrency, async (_, i) => {
            clients[i] = await api.create(address);
        });
        return clients;
    }
    async function remove(clients) {
        await pool(clients, concurrency, c => api.kill(c));
    }
    async function burst(c, rounds = 4) {
        for (let round = 0; round < rounds; round++) {
            const expected = c.count + 8;
            const events = api.clicks(c, 8);
            const reply = await api.sync(c, events);
            assert.equal(reply.lastEvent, events.at(-1).id);
            assert.equal(c.count, expected, "Lost business actions");
            await api.sync(c, events); // a real replay of the same wire IDs
            assert.equal(c.count, expected, "Business action executed twice");
            details.businessActions += 8;
            details.duplicateBatches++;
        }
    }
    if (name === "polling" || name === "burst" || name === "slow-client") {
        const clients = await createMany(count);
        if (name === "polling") {
            for (let round = 0; round < 10; round++)
                await pool(clients, concurrency, async c => {
                    const reply = await api.sync(c);
                    assert.deepEqual(reply.updates, []);
                });
            if (observe) details.eventToDomMs = await observe(clients);
        } else if (name === "burst") {
            await pool(clients, concurrency, c => burst(c));
            const target = clients[0];
            const rejected = await api.sync(target, api.clicks(target, 65));
            assert.equal(rejected.eventBackpressure, true, "Oversized event batch was not bounded");
            assert.equal(target.count, 32, "Rejected events changed business state");
            const recovered = await api.create("/load", target.tabId);
            assert.equal(recovered.count, 32);
            await burst(recovered, 1);
            await api.kill(recovered);
        } else {
            await pool(clients, concurrency, async c => {
                c.withheldCheckpoint = c.lastUpdate;
                await api.sync(c, api.clicks(c, 1));
                assert.equal(c.count, 1);
            });
            await delay(1800); // fixture update retention is 1500ms
            await pool(clients, concurrency, async c => {
                const reply = await api.sync(c, [], c.withheldCheckpoint);
                assert.equal(
                    reply.updateResyncRequired,
                    true,
                    "Expired replay queue was not bounded"
                );
                details.resyncs++;
                const recovered = await api.create("/load", c.tabId);
                assert.equal(recovered.count, 1, "Resync lost or replayed a committed command");
                await api.kill(recovered);
            });
        }
        await remove(clients);
    } else {
        // Large trees, binary work and four-page histories use bounded live waves.
        // The report distinguishes total clients/documents from peak concurrency.
        for (let offset = 0; offset < count; offset += 16) {
            const clients = await createMany(
                Math.min(16, count - offset),
                name === "large-tree"
                    ? "/load-large"
                    : name === "uploads"
                      ? "/load-upload"
                      : "/load"
            );
            await pool(clients, concurrency, async c => {
                if (name === "large-tree") {
                    await burst(c, 1);
                } else if (name === "history") {
                    const siblings = [];
                    for (let i = 0; i < 3; i++)
                        siblings.push(await api.create("/load-next", c.tabId));
                    await api.sync(c, api.clicks(c, 1));
                    assert.equal(c.count, 1);
                    for (const sibling of siblings) {
                        await api.sync(sibling);
                        assert.equal(sibling.count, 1);
                    }
                    for (const sibling of siblings) await api.kill(sibling);
                    details.businessActions++;
                } else if (name === "uploads") {
                    c.fileId = 1;
                    await api.sync(c, [
                        {
                            id: "#" + ++c.event,
                            widget: c.loader,
                            type: "upload",
                            data: {
                                fileId: c.fileId,
                                name: "fixture.bin",
                                type: "application/octet-stream",
                                size: 65536,
                                totalChunks: 1
                            }
                        }
                    ]);
                    const bytes = Buffer.alloc(65536, 7);
                    let reply;
                    for (let attempt = 0; attempt < 300; attempt++) {
                        reply = await api.send(
                            {
                                action: "upload chunk",
                                client: c.id,
                                widget: c.loader,
                                fileId: c.fileId,
                                chunkIndex: 0,
                                lastUpdate: "#" + c.lastUpdate
                            },
                            c,
                            bytes
                        );
                        api.apply(c, reply);
                        if (reply.complete) break;
                        assert.equal(
                            reply.uploadError,
                            "processingBusy",
                            "Unexpected upload rejection"
                        );
                        details.busyReplies++;
                        await delay(20);
                        c.fileId++;
                        // processingBusy is terminal for this descriptor: create a fresh attempt.
                        await api.sync(c, [
                            {
                                id: "#" + ++c.event,
                                widget: c.loader,
                                type: "upload",
                                data: {
                                    fileId: c.fileId,
                                    name: "fixture.bin",
                                    type: "application/octet-stream",
                                    size: 65536,
                                    totalChunks: 1
                                }
                            }
                        ]);
                    }
                    assert.equal(reply.complete, true, "Upload retry deadline exceeded");
                    const duplicate = await api.send(
                        {
                            action: "upload chunk",
                            client: c.id,
                            widget: c.loader,
                            fileId: c.fileId,
                            chunkIndex: 0,
                            lastUpdate: "#" + c.lastUpdate
                        },
                        c,
                        bytes
                    );
                    api.apply(c, duplicate);
                    assert.equal(duplicate.complete, true);
                    for (let i = 0; i < 200 && c.uploads !== 1; i++) {
                        await delay(20);
                        await api.sync(c);
                    }
                    assert.equal(c.uploads, 1, "Upload callback lost or duplicated");
                } else throw new Error("Unknown load profile");
            });
            await remove(clients);
        }
    }
    return details;
}
