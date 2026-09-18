/** Ghép class có điều kiện. Gọn hơn clsx cho nhu cầu của dự án này. */
export function cn(...parts: Array<string | false | null | undefined>): string {
  return parts.filter(Boolean).join(" ");
}
