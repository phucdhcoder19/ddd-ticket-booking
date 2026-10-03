import type { Discount, Passenger } from "@/api/types";

export type FieldErrors = Partial<Record<keyof Passenger, string>>;

/**
 * Passenger data validation rules.
 *
 * Three principles for reporting errors:
 *  - Only report AFTER the user leaves the field (blur) or submits; flagging
 *    from the very first character is the fastest way to annoy people.
 *  - Once a field has an error, re-check on every keystroke, so the user sees
 *    the error disappear as soon as it is fixed — that is the feedback they need.
 *  - Errors say what the CORRECT FORMAT is, not just "invalid".
 */

/** National ID card: 12 digits. Legacy 9-digit ID cards are still accepted for older people. */
export function validateIdNumber(value: string): string | null {
  const v = value.trim();
  if (!v) return "Please enter the ID number.";
  if (!/^\d+$/.test(v)) return "The ID number contains digits only, no spaces or dashes.";
  if (v.length !== 12 && v.length !== 9) {
    return `The ID number must have 12 digits (or 9 for a legacy ID card). You entered ${v.length} digits.`;
  }
  return null;
}

/** Mobile number: 10 digits starting with 0, or the +84 form */
export function validatePhone(value: string): string | null {
  const v = value.replace(/[\s.]/g, "");
  if (!v) return "Please enter a phone number.";
  const normalized = v.startsWith("+84") ? "0" + v.slice(3) : v;
  if (!/^0\d{9}$/.test(normalized)) {
    return "The phone number must have 10 digits and start with 0. For example: 0912345678.";
  }
  return null;
}

export function validateFullName(value: string): string | null {
  const v = value.trim().replace(/\s+/g, " ");
  if (!v) return "Please enter the full name.";
  if (v.length < 3) return "The name is too short, please enter the full name.";
  if (!v.includes(" ")) return "Please enter both first and last name, for example: Jane Doe.";
  if (/\d/.test(v)) return "The name cannot contain digits.";
  // Letters (including accented ones), spaces and apostrophes only
  if (!/^[\p{L}\s']+$/u.test(v)) return "The name may only contain letters, exactly as on the ID card.";
  return null;
}

export function validatePassenger(p: Passenger): FieldErrors {
  const errors: FieldErrors = {};
  const name = validateFullName(p.fullName);
  if (name) errors.fullName = name;
  const id = validateIdNumber(p.idNumber);
  if (id) errors.idNumber = id;
  const phone = validatePhone(p.phone);
  if (phone) errors.phone = phone;
  return errors;
}

/** Documents to bring on board, depending on the discount */
export const DISCOUNT_PROOF: Record<Discount, string | null> = {
  NONE: null,
  STUDENT: "A valid student card must be shown on board.",
  CHILD: "Children need a birth certificate or passport to verify their age.",
  SENIOR: "An ID card showing an age of 60 or over is required.",
};

export const isPassengerValid = (p: Passenger) => Object.keys(validatePassenger(p)).length === 0;
