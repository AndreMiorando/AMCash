export function maskCurrencyInput(value: string) {
  const digits = value.replace(/\D/g, "");
  if (!digits) return "";

  const normalized = digits.replace(/^0+(?=\d)/, "");
  const padded = normalized.padStart(3, "0");
  const rawInteger = padded.slice(0, -2);
  const integer = (rawInteger.replace(/^0+(?=\d)/, "") || "0")
    .replace(/\B(?=(\d{3})+(?!\d))/g, ".");
  const decimal = padded.slice(-2);

  return `${integer},${decimal}`;
}

export function completeCurrencyInput(value: string) {
  return maskCurrencyInput(value);
}

export function parseCurrencyInput(value: string) {
  return Number(value.replace(/\./g, "").replace(",", "."));
}

export function currencyInputFromNumber(value: number | null | undefined) {
  return value == null ? "" : maskCurrencyInput(String(Math.round(value * 100)));
}
