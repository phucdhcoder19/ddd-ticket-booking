import { useEffect, useRef, type ReactNode } from "react";

/**
 * A dialog built on the browser's native <dialog>: it already has a focus
 * trap, closing with Esc and a ::backdrop overlay — no library needed.
 * On phones, the dialog slides up from the bottom so the thumb can reach the buttons.
 */
export function Modal({
  open, onClose, title, description, children, footer, dismissible = true,
}: {
  open: boolean;
  onClose: () => void;
  title: string;
  description?: string;
  children?: ReactNode;
  footer?: ReactNode;
  /** false when the user must pick an action (e.g. the hold has expired) */
  dismissible?: boolean;
}) {
  const ref = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    const onCancel = (e: Event) => {
      // Block Esc when the dialog requires an answer
      if (!dismissible) e.preventDefault();
      else onClose();
    };
    dialog.addEventListener("cancel", onCancel);
    return () => dialog.removeEventListener("cancel", onCancel);
  }, [dismissible, onClose]);

  return (
    <dialog
      ref={ref}
      aria-labelledby="modal-title"
      aria-describedby={description ? "modal-desc" : undefined}
      className="m-0 w-full max-w-lg rounded-t-3xl border-0 bg-white p-0 shadow-[var(--shadow-lift)] backdrop:bg-ink-900/45 sm:m-auto sm:rounded-3xl"
      style={{ marginTop: "auto", marginBottom: 0 }}
      onClick={(e) => {
        // Clicking outside the content closes it
        if (dismissible && e.target === ref.current) onClose();
      }}
    >
      <div className="px-5 pt-5 pb-4 sm:px-6">
        <h2 id="modal-title" className="text-xl font-bold text-ink-900">{title}</h2>
        {description && <p id="modal-desc" className="mt-1.5 text-ink-700">{description}</p>}
        {children && <div className="mt-4">{children}</div>}
      </div>
      {footer && (
        <div className="flex flex-col-reverse gap-2 border-t border-ink-200 bg-ink-50 px-5 py-4 sm:flex-row sm:justify-end sm:px-6">
          {footer}
        </div>
      )}
    </dialog>
  );
}
