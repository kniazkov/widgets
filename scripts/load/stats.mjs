// Empirical nearest-rank percentiles. No tail-confidence claim is made for small samples.
export function distribution(values) {
    if (!values.length) return { samples: 0, p50: null, p95: null, p99: null, max: null };
    const ordered = [...values].sort((a, b) => a - b);
    if (ordered.some(n => !Number.isFinite(n) || n < 0)) throw new Error("Invalid timing sample");
    const at = q => ordered[Math.ceil(q * ordered.length) - 1];
    return {
        samples: ordered.length,
        p50: at(0.5),
        p95: at(0.95),
        p99: at(0.99),
        max: ordered.at(-1)
    };
}

export async function pool(items, concurrency, action) {
    if (!Number.isInteger(concurrency) || concurrency < 1) throw new Error("Invalid concurrency");
    let cursor = 0;
    let failure;
    // Finish in-flight work before cleanup; never leave rejected Promise.all siblings running.
    await Promise.all(
        Array.from({ length: Math.min(items.length, concurrency) }, async () => {
            while (!failure && cursor < items.length) {
                const index = cursor++;
                try {
                    await action(items[index], index);
                } catch (error) {
                    failure ||= error;
                }
            }
        })
    );
    if (failure) throw failure;
}
