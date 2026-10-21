"use client";

import { useState } from "react";
import { createPortal } from "react-dom";

export function dateToInputValue(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("pt-BR").format(new Date(`${value}T12:00:00`));
}

export default function DatePickerInput({ label, value, onChange, min, max }: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  min?: string;
  max?: string;
}) {
  const selectedDate = new Date(`${value}T12:00:00`);
  const initialDate = Number.isNaN(selectedDate.getTime()) ? new Date() : selectedDate;
  const [isOpen, setIsOpen] = useState(false);
  const [visibleMonth, setVisibleMonth] = useState(() => new Date(initialDate.getFullYear(), initialDate.getMonth(), 1));
  const monthLabel = new Intl.DateTimeFormat("pt-BR", { month: "long", year: "numeric" }).format(visibleMonth);
  const firstGridDate = new Date(visibleMonth.getFullYear(), visibleMonth.getMonth(), 1 - visibleMonth.getDay());
  const days = Array.from({ length: 42 }, (_, index) => {
    const date = new Date(firstGridDate);
    date.setDate(firstGridDate.getDate() + index);
    return date;
  });
  const todayValue = dateToInputValue(new Date());

  function openCalendar() {
    const current = new Date(`${value}T12:00:00`);
    const reference = Number.isNaN(current.getTime()) ? new Date() : current;
    setVisibleMonth(new Date(reference.getFullYear(), reference.getMonth(), 1));
    setIsOpen(true);
  }

  function chooseDate(date: Date) {
    const nextValue = dateToInputValue(date);
    if ((min && nextValue < min) || (max && nextValue > max)) return;
    onChange(nextValue);
    setIsOpen(false);
  }

  return <div className="field date-picker-field">
    <span>{label}</span>
    <button type="button" className="date-input-button" onClick={openCalendar} aria-haspopup="dialog" aria-expanded={isOpen}>
      <span>{value ? formatDate(value) : "Selecionar data"}</span>
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true"><rect x="3.5" y="5" width="17" height="15" rx="2" /><path d="M8 3v4M16 3v4M3.5 9.5h17" /></svg>
    </button>

    {isOpen && createPortal(<div className="date-picker-backdrop" role="presentation" onMouseDown={(event) => { event.stopPropagation(); setIsOpen(false); }}>
      <section className="date-picker-dialog" role="dialog" aria-modal="true" aria-label="Selecionar data" onMouseDown={(event) => event.stopPropagation()}>
        <div className="date-picker-header">
          <button type="button" onClick={() => setVisibleMonth((current) => new Date(current.getFullYear(), current.getMonth() - 1, 1))} aria-label="Mês anterior"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="m15 18-6-6 6-6" /></svg></button>
          <strong>{monthLabel}</strong>
          <button type="button" onClick={() => setVisibleMonth((current) => new Date(current.getFullYear(), current.getMonth() + 1, 1))} aria-label="Próximo mês"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="m9 18 6-6-6-6" /></svg></button>
        </div>
        <div className="date-picker-weekdays" aria-hidden="true">{["D", "S", "T", "Q", "Q", "S", "S"].map((weekday, index) => <span key={`${weekday}-${index}`}>{weekday}</span>)}</div>
        <div className="date-picker-days">{days.map((date) => {
          const dateValue = dateToInputValue(date);
          const outside = date.getMonth() !== visibleMonth.getMonth();
          const disabled = Boolean((min && dateValue < min) || (max && dateValue > max));
          return <button type="button" key={dateValue} disabled={disabled} className={`${outside ? "outside " : ""}${dateValue === value ? "selected " : ""}${dateValue === todayValue ? "today" : ""}`} onClick={() => chooseDate(date)} aria-pressed={dateValue === value}>{date.getDate()}</button>;
        })}</div>
        <div className="date-picker-actions"><button type="button" onClick={() => setIsOpen(false)}>Cancelar</button><button type="button" disabled={Boolean((min && todayValue < min) || (max && todayValue > max))} onClick={() => chooseDate(new Date())}>Hoje</button></div>
      </section>
    </div>, document.body)}
  </div>;
}
