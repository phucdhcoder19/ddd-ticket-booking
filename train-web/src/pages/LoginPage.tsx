import { useEffect, useState, type FormEvent } from "react";
import { useLocation, useNavigate, useSearchParams } from "react-router-dom";
import { ApiError, friendlyMessage } from "@/api/errors";
import { Button } from "@/components/ui/Button";
import { TextInput } from "@/components/ui/Field";
import { useSubmitLock } from "@/hooks/useSubmitLock";
import { useAuth } from "@/store/AuthContext";
import { validatePhone } from "@/lib/validate";
import { cn } from "@/lib/cn";

type Mode = "signin" | "register";

/**
 * Sign in / create an account — one page, two tabs (?mode=register).
 *
 * Whoever sent the user here passes `state.from`; after signing in they go
 * straight back to it (usually the seat map), instead of landing on the home
 * page and having to find their trip again.
 */
export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [params, setParams] = useSearchParams();
  const { user, signIn, register } = useAuth();

  const mode: Mode = params.get("mode") === "register" ? "register" : "signin";
  const requested = (location.state as { from?: string } | null)?.from ?? "/";
  // "Sign in" clicked on this very page must not send the user back here
  const from = requested.startsWith("/login") ? "/" : requested;

  const [phone, setPhone] = useState("");
  const [fullName, setFullName] = useState("");
  const [password, setPassword] = useState("");
  const [showErrors, setShowErrors] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  // Arrive at the top: the page that sent us here may have been scrolled far down
  useEffect(() => {
    window.scrollTo(0, 0);
  }, []);

  // Already signed in (or just signed in): go back where the user came from
  useEffect(() => {
    if (user) navigate(from, { replace: true });
  }, [user, from, navigate]);

  const errors = {
    phone: validatePhone(phone),
    fullName: mode === "register" && fullName.trim().length < 2 ? "Please enter your full name." : null,
    password:
      password.length === 0
        ? "Please enter a password."
        : mode === "register" && password.length < 8
          ? "Use at least 8 characters."
          : null,
  };
  const valid = !errors.phone && !errors.fullName && !errors.password;

  const { submit, pending } = useSubmitLock(async () => {
    setFormError(null);
    try {
      if (mode === "register") await register(phone.trim(), fullName.trim(), password);
      else await signIn(phone.trim(), password);
      // The effect above navigates once `user` is set
    } catch (e) {
      // 422 (wrong password, phone taken) and 429 (locked out) carry a message
      // written for the user; anything else gets the generic wording
      setFormError(
        e instanceof ApiError && (e.kind === "VALIDATION" || e.kind === "OVERLOADED")
          ? e.message
          : friendlyMessage(e).detail,
      );
      throw e;
    }
  });

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    setShowErrors(true);
    if (valid) void submit().catch(() => {});
  };

  const switchMode = (next: Mode) => {
    setParams(next === "register" ? { mode: "register" } : {}, { replace: true, state: location.state });
    setShowErrors(false);
    setFormError(null);
  };

  return (
    <div className="mx-auto flex w-full max-w-md flex-col gap-4 py-4">
      <div>
        <h1 className="text-xl font-bold">{mode === "register" ? "Create an account" : "Sign in"}</h1>
        <p className="mt-1 text-sm text-ink-600">
          {mode === "register"
            ? "Your tickets are kept in your account, so you can find them on any device."
            : "Sign in to hold seats and see your tickets."}
        </p>
      </div>

      <div className="flex gap-2" role="tablist" aria-label="Sign in or register">
        {([
          { key: "signin" as const, label: "Sign in" },
          { key: "register" as const, label: "Register" },
        ]).map((t) => (
          <button
            key={t.key}
            type="button"
            role="tab"
            aria-selected={mode === t.key}
            onClick={() => switchMode(t.key)}
            className={cn(
              "min-h-11 flex-1 rounded-xl border-2 px-3 text-sm font-semibold transition-colors",
              mode === t.key
                ? "border-brand-600 bg-brand-600 text-white"
                : "border-ink-200 bg-white text-ink-700 hover:border-ink-300",
            )}
          >
            {t.label}
          </button>
        ))}
      </div>

      <form onSubmit={onSubmit} noValidate className="flex flex-col gap-1 rounded-2xl border border-ink-200 bg-white p-4">
        <TextInput
          label="Phone number"
          required
          type="tel"
          inputMode="tel"
          autoComplete="tel"
          placeholder="0912345678"
          value={phone}
          onChange={(e) => setPhone(e.target.value)}
          error={showErrors ? errors.phone : null}
        />
        {mode === "register" && (
          <TextInput
            label="Full name"
            required
            autoComplete="name"
            placeholder="Nguyen Van An"
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            error={showErrors ? errors.fullName : null}
          />
        )}
        <TextInput
          label="Password"
          required
          type="password"
          autoComplete={mode === "register" ? "new-password" : "current-password"}
          hint={mode === "register" ? "At least 8 characters." : undefined}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          error={showErrors ? errors.password : null}
        />

        {formError && (
          <p role="alert" className="mb-3 rounded-xl border border-brand-200 bg-brand-50 px-3 py-2 text-sm font-medium text-brand-700">
            {formError}
          </p>
        )}

        <Button
          type="submit"
          size="lg"
          fullWidth
          loading={pending}
          loadingText={mode === "register" ? "Creating your account…" : "Signing in…"}
        >
          {mode === "register" ? "Create account" : "Sign in"}
        </Button>
      </form>
    </div>
  );
}
