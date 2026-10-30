const countryNames = new Intl.DisplayNames(['en'], { type: 'region' });

/** "IN" -> "India". Falls back to the code for anything Intl does not know. */
export function countryName(code: string): string {
  return countryNames.of(code) ?? code;
}

/** Whole units in the viewer's own number style, e.g. "₹1,850,000" or "€70.000". */
export function formatMoney(amount: number, currency: string): string {
  return new Intl.NumberFormat(navigator.language, { style: 'currency', currency, maximumFractionDigits: 0 }).format(amount);
}

/** "2024-04-01" -> "1 Apr 2024" (viewer's locale). Read as UTC so no time zone shifts the day. */
export function formatDate(isoDate: string): string {
  return new Intl.DateTimeFormat(navigator.language, { dateStyle: 'medium', timeZone: 'UTC' })
    .format(new Date(`${isoDate}T00:00:00Z`));
}
