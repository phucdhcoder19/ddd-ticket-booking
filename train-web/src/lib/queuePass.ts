/**
 * Waiting room admission pass.
 *
 * When the waiting room calls your turn, the queue token becomes an
 * "admission pass": from then on every request carries it in the
 * X-Queue-Token header (see lib/http.ts), and the server only allows holds
 * while the pass is still valid.
 *
 * Stored in sessionStorage, not localStorage: the pass belongs to ONE tab.
 * A second tab is a new place in the queue — with a shared localStorage, a
 * person opening 10 tabs would queue once and be able to buy in all 10.
 */
const PASS_KEY = "trainbooking.queue.pass";

export function getQueuePass(): string | null {
  try {
    return sessionStorage.getItem(PASS_KEY);
  } catch {
    return null;
  }
}

export function setQueuePass(token: string): void {
  try {
    sessionStorage.setItem(PASS_KEY, token);
  } catch {
    /* private mode blocks storage — the pass only lives until the tab closes */
  }
}

export function clearQueuePass(): void {
  try {
    sessionStorage.removeItem(PASS_KEY);
  } catch {
    /* ignore */
  }
}
