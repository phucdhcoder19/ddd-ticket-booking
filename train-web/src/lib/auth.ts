import type { AuthSession } from "@/api/types";

/**
 * The signed-in session (JWT + user).
 *
 * localStorage, unlike the queue pass (sessionStorage): being signed in should
 * survive closing the tab and hold for every tab, the way every site behaves.
 *
 * Trade-off worth knowing: any script running on the page can read
 * localStorage, so an XSS bug would leak the token. An httpOnly cookie hides
 * it from scripts but needs CSRF protection and credentialed CORS. The token
 * lives 24h at most, which bounds the damage.
 */
const SESSION_KEY = "trainbooking.auth";

/** Fired on this tab when the session is cleared; other tabs get the native "storage" event. */
export const AUTH_CHANGED_EVENT = "trainbooking:auth-changed";

export function getSession(): AuthSession | null {
  try {
    const raw = localStorage.getItem(SESSION_KEY);
    if (!raw) return null;
    const session = JSON.parse(raw) as AuthSession;
    if (new Date(session.expiresAt).getTime() <= Date.now()) {
      localStorage.removeItem(SESSION_KEY);
      return null;
    }
    return session;
  } catch {
    return null;
  }
}

export function getAuthToken(): string | null {
  return getSession()?.token ?? null;
}

export function saveSession(session: AuthSession): void {
  try {
    localStorage.setItem(SESSION_KEY, JSON.stringify(session));
  } catch {
    /* private mode blocks storage — the user stays signed in until the page reloads */
  }
  window.dispatchEvent(new Event(AUTH_CHANGED_EVENT));
}

export function clearSession(): void {
  try {
    localStorage.removeItem(SESSION_KEY);
  } catch {
    /* ignore */
  }
  window.dispatchEvent(new Event(AUTH_CHANGED_EVENT));
}
