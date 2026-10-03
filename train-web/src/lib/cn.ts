/** Join class names conditionally. Lighter than clsx for this project's needs. */
export function cn(...parts: Array<string | false | null | undefined>): string {
  return parts.filter(Boolean).join(" ");
}
