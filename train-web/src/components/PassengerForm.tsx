import { useState } from "react";
import { DISCOUNT_LABEL, DISCOUNT_RATE, type Discount, type HoldItem, type Passenger } from "@/api/types";
import { SelectInput, TextInput } from "./ui/Field";
import { Badge } from "./ui/Badge";
import { DISCOUNT_PROOF, validateFullName, validateIdNumber, validatePhone } from "@/lib/validate";
import { formatVnd } from "@/lib/format";

/**
 * Form for one passenger.
 *
 * Small details that matter on a phone:
 *  - inputMode="numeric" for the ID and phone number to bring up the number pad.
 *  - Proper autoComplete values so the browser can fill them in — typing a
 *    12-digit ID on a touch screen is hard work for older people.
 *  - The ID field only accepts digits: filtered while typing instead of
 *    complaining "no spaces allowed" after the user is done.
 */
export function PassengerForm({
  index, item, value, onChange, showErrors,
}: {
  index: number;
  item: HoldItem;
  value: Passenger;
  onChange: (p: Passenger) => void;
  /** On once the user pressed "Continue" — then errors show even for untouched fields */
  showErrors: boolean;
}) {
  const [touched, setTouched] = useState<Record<string, boolean>>({});

  const set = <K extends keyof Passenger>(key: K, v: Passenger[K]) => onChange({ ...value, [key]: v });
  const show = (field: string) => showErrors || touched[field];

  const nameError = show("fullName") ? validateFullName(value.fullName) : null;
  const idError = show("idNumber") ? validateIdNumber(value.idNumber) : null;
  const phoneError = show("phone") ? validatePhone(value.phone) : null;

  const discountRate = DISCOUNT_RATE[value.discount];
  const finalPrice = Math.round((item.price * (1 - discountRate)) / 1000) * 1000;

  return (
    <fieldset className="rounded-2xl border border-ink-200 bg-white p-4">
      <legend className="sr-only">Details of passenger {index + 1}</legend>

      <div className="mb-3 flex items-center justify-between gap-3">
        <h3 className="font-bold text-ink-900">Passenger {index + 1}</h3>
        <Badge tone="brand">Carriage {item.carriageNumber} · seat {item.seatLabel}</Badge>
      </div>

      <div className="flex flex-col gap-1">
        <TextInput
          label="Full name"
          placeholder="JANE DOE"
          hint="Exactly as on the ID card, no abbreviations."
          autoComplete="name"
          autoCapitalize="characters"
          required
          value={value.fullName}
          error={nameError}
          onChange={(e) => set("fullName", e.target.value)}
          onBlur={() => setTouched((t) => ({ ...t, fullName: true }))}
        />

        <TextInput
          label="ID number"
          placeholder="001203001234"
          hint="The 12 digits on the national ID card."
          inputMode="numeric"
          autoComplete="off"
          maxLength={12}
          required
          value={value.idNumber}
          error={idError}
          onChange={(e) => set("idNumber", e.target.value.replace(/\D/g, ""))}
          onBlur={() => setTouched((t) => ({ ...t, idNumber: true }))}
        />

        <TextInput
          label="Phone number"
          placeholder="0912345678"
          hint="Used to notify you if the departure time changes."
          inputMode="tel"
          type="tel"
          autoComplete="tel"
          maxLength={15}
          required
          value={value.phone}
          error={phoneError}
          onChange={(e) => set("phone", e.target.value.replace(/[^\d+]/g, ""))}
          onBlur={() => setTouched((t) => ({ ...t, phone: true }))}
        />

        <SelectInput
          label="Discount"
          hint={DISCOUNT_PROOF[value.discount] ?? "Choose one if you are eligible for a discounted fare."}
          value={value.discount}
          onChange={(e) => set("discount", e.target.value as Discount)}
          options={(Object.keys(DISCOUNT_LABEL) as Discount[]).map((d) => ({
            value: d,
            label: DISCOUNT_LABEL[d],
          }))}
        />
      </div>

      <div className="mt-2 flex items-center justify-between border-t border-ink-100 pt-3">
        <span className="text-sm text-ink-600">Fare</span>
        <span className="flex items-baseline gap-2">
          {discountRate > 0 && (
            <span className="tnum text-sm text-ink-400 line-through">{formatVnd(item.price)}</span>
          )}
          <span className="tnum font-bold text-brand-700">{formatVnd(finalPrice)}</span>
        </span>
      </div>
    </fieldset>
  );
}

/** Final price of one ticket after discount — shared by the form and the payment screen */
export const finalPriceOf = (item: HoldItem, discount: Discount) =>
  Math.round((item.price * (1 - DISCOUNT_RATE[discount])) / 1000) * 1000;
