// Realtime client. Polls GET /api/sensors for now; the backend has no push
// channel yet (WebSocket vs. SSE is still open, see ADR-0002). Callers only see
// onData / onError, so swapping the transport later does not touch them.
import type { SensorsResponse } from '../types';

export const POLL_INTERVAL_MS = 2000;

export function subscribeToSensors(
  onData: (response: SensorsResponse) => void,
  onError: (error: unknown) => void,
): () => void {
  let stopped = false;
  let timer: ReturnType<typeof setTimeout> | undefined;
  let controller: AbortController | undefined;

  async function poll() {
    controller = new AbortController();
    try {
      const res = await fetch('/api/sensors', { signal: controller.signal, cache: 'no-store' });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const body = (await res.json()) as SensorsResponse;
      if (!stopped) onData(body);
    } catch (error) {
      if (!stopped) onError(error);
    } finally {
      if (!stopped) timer = setTimeout(poll, POLL_INTERVAL_MS);
    }
  }

  poll();
  return () => {
    stopped = true;
    clearTimeout(timer);
    controller?.abort();
  };
}
