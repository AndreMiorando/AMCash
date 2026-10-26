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

export const MAX_DAILY_AMOUNT = 99999.99;

export function maskLimitedCurrencyInput(value: string, max = MAX_DAILY_AMOUNT) {
  const masked = maskCurrencyInput(value);
  return parseCurrencyInput(masked) > max ? maskCurrencyInput(String(Math.round(max * 100))) : masked;
}

const fullCurrency = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const compactCurrency = new Intl.NumberFormat("pt-BR", {
  style: "currency", currency: "BRL", notation: "compact", minimumFractionDigits: 0, maximumFractionDigits: 1,
});

export function formatCompactCurrency(value: number, threshold = 10000) {
  return Math.abs(value) >= threshold ? compactCurrency.format(value) : fullCurrency.format(value);
}
