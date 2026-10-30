const countryNames = new Intl.DisplayNames(['en'], { type: 'region' });

/** "IN" -> "India". Falls back to the code for anything Intl does not know. */
export function countryName(code: string): string {
  return countryNames.of(code) ?? code;
}

/** Whole units in the viewer's own number style, e.g. "₹1,850,000" or "€70.000". */
export function formatMoney(amount: number, currency: string): string {
  return new Intl.NumberFormat(navigator.language, { style: 'currency', currency, maximumFractionDigits: 0 }).format(amount);
}
