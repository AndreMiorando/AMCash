type DateValue = string | Date;

export function localDate(value: DateValue) {
  if (value instanceof Date) return value;
  return new Date(`${value}T12:00:00`);
}

export function formatLongDate(value: DateValue) {
  return new Intl.DateTimeFormat("pt-BR", {
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(localDate(value));
}

export function formatMonthYear(value: DateValue) {
  return new Intl.DateTimeFormat("pt-BR", {
    month: "long",
    year: "numeric",
  }).format(localDate(value));
}

export function formatDateRange(startValue: DateValue, endValue: DateValue) {
  const start = localDate(startValue);
  const end = localDate(endValue);
  if (start.getTime() === end.getTime()) return formatLongDate(start);

  const startDay = start.getDate();
  const endDay = end.getDate();
  const startMonth = new Intl.DateTimeFormat("pt-BR", { month: "long" }).format(start);
  const endMonth = new Intl.DateTimeFormat("pt-BR", { month: "long" }).format(end);
  const sameYear = start.getFullYear() === end.getFullYear();
  const sameMonth = sameYear && start.getMonth() === end.getMonth();

  if (sameMonth) return `${startDay} a ${endDay} de ${endMonth} de ${end.getFullYear()}`;
  if (sameYear) return `${startDay} de ${startMonth} a ${endDay} de ${endMonth} de ${end.getFullYear()}`;
  return `${formatLongDate(start)} a ${formatLongDate(end)}`;
}
