import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { api } from "@/api/client";
import type { AuthUser } from "@/api/types";
import { AUTH_CHANGED_EVENT, clearSession, getSession, saveSession } from "@/lib/auth";

/**
 * Who is signed in, for the whole app.
 *
 * The stored session is trusted right away so the header does not flash
 * "Sign in" on every reload; GET /me then checks it in the background. If the
 * server rejects the token, lib/http.ts clears the session and the
 * AUTH_CHANGED event below brings the UI back to signed out.
 */
type AuthApi = {
  user: AuthUser | null;
  signIn: (phone: string, password: string) => Promise<void>;
  register: (phone: string, fullName: string, password: string) => Promise<void>;
  signOut: () => void;
};

const Ctx = createContext<AuthApi | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => getSession()?.user ?? null);

  // Follow the session wherever it changes: this tab (sign in/out, a 401) or another tab
  useEffect(() => {
    const sync = () => setUser(getSession()?.user ?? null);
    window.addEventListener(AUTH_CHANGED_EVENT, sync);
    window.addEventListener("storage", sync);
    return () => {
      window.removeEventListener(AUTH_CHANGED_EVENT, sync);
      window.removeEventListener("storage", sync);
    };
  }, []);

  // Check the stored token once on startup. Only a 401 signs the user out
  // (inside lib/http.ts); being offline must not.
  useEffect(() => {
    const session = getSession();
    if (!session) return;
    api.me()
      .then((fresh) => saveSession({ ...session, user: fresh }))
      .catch(() => {});
  }, []);

  const signIn = useCallback(async (phone: string, password: string) => {
    saveSession(await api.login({ phone, password }));
  }, []);

  const register = useCallback(async (phone: string, fullName: string, password: string) => {
    saveSession(await api.register({ phone, fullName, password }));
  }, []);

  const signOut = useCallback(() => clearSession(), []);

  const value = useMemo(() => ({ user, signIn, register, signOut }), [user, signIn, register, signOut]);
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useAuth() {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
}
