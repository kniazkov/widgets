import { expect, test } from "vitest";
import { distribution, pool } from "../../../scripts/load/stats.mjs";

test("reported tails use measured samples and never invent an empty latency", () => {
    expect(distribution([])).toEqual({ samples: 0, p50: null, p95: null, p99: null, max: null });
    expect(distribution(Array.from({ length: 100 }, (_, i) => 100 - i))).toEqual({
        samples: 100,
        p50: 50,
        p95: 95,
        p99: 99,
        max: 100
    });
    expect(() => distribution([1, NaN])).toThrow();
    expect(() => distribution([-1])).toThrow();
});

test("failed load waits for in-flight work before resource cleanup", async () => {
    let release,
        exited = false;
    const gate = new Promise(resolve => {
        release = resolve;
    });
    const visited = [];
    const work = pool([0, 1, 2, 3], 2, async n => {
        visited.push(n);
        if (n === 0) {
            await Promise.resolve();
            throw new Error("expected failure");
        }
        await gate;
        exited = true;
    });
    let finished = false;
    const checked = work.catch(error => {
        expect(error.message).toBe("expected failure");
        finished = true;
    });
    await Promise.resolve();
    await Promise.resolve();
    expect(finished).toBe(false);
    release();
    await checked;
    expect(exited).toBe(true);
    expect(visited).toEqual([0, 1]);
});
