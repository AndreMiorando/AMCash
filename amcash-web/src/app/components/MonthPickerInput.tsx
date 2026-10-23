"use client";

import { useEffect, useId, useState } from "react";
import { createPortal } from "react-dom";

const monthNames = Array.from({ length: 12 }, (_, month) => (
  new Intl.DateTimeFormat("pt-BR", { month: "long" }).format(new Date(2024, month, 1))
));

export function MonthPickerDialog({ open, selectedYear, selectedMonth, onClose, onSelect }: {
  open: boolean;
  selectedYear: number;
  selectedMonth: number;
  onClose: () => void;
  onSelect: (year: number, month: number) => void;
}) {
  if (!open) return null;
  return <OpenMonthPickerDialog selectedYear={selectedYear} selectedMonth={selectedMonth} onClose={onClose} onSelect={onSelect} />;
}

function OpenMonthPickerDialog({ selectedYear, selectedMonth, onClose, onSelect }: {
  selectedYear: number;
  selectedMonth: number;
  onClose: () => void;
  onSelect: (year: number, month: number) => void;
}) {
  const titleId = useId();
  const [pickerYear, setPickerYear] = useState(selectedYear);
  const today = new Date();

  useEffect(() => {
    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === "Escape") onClose();
    }
    window.addEventListener("keydown", closeOnEscape);
    return () => window.removeEventListener("keydown", closeOnEscape);
  }, [onClose]);

  return createPortal(<div className="month-picker-backdrop" role="presentation" onMouseDown={onClose}>
    <section className="month-picker" role="dialog" aria-modal="true" aria-labelledby={titleId} onMouseDown={(event) => event.stopPropagation()}>
      <div className="month-picker-heading">
        <div><span>PERÍODO</span><h2 id={titleId}>Selecionar mês</h2></div>
        <button type="button" className="month-picker-close" onClick={onClose} aria-label="Fechar seletor">×</button>
      </div>
      <div className="year-selector" aria-label="Selecionar ano">
        <button type="button" onClick={() => setPickerYear((year) => year - 1)} aria-label={`Ir para ${pickerYear - 1}`}><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="m15 18-6-6 6-6" /></svg></button>
        <strong aria-live="polite">{pickerYear}</strong>
        <button type="button" onClick={() => setPickerYear((year) => year + 1)} aria-label={`Ir para ${pickerYear + 1}`}><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="m9 18 6-6-6-6" /></svg></button>
      </div>
      <div className="month-grid">
        {monthNames.map((monthName, month) => {
          const isSelected = selectedYear === pickerYear && selectedMonth === month;
          const isCurrent = today.getFullYear() === pickerYear && today.getMonth() === month;
          return <button type="button" key={monthName} className={`${isSelected ? "selected" : ""}${isCurrent ? " current" : ""}`} onClick={() => onSelect(pickerYear, month)} aria-pressed={isSelected}>
            {monthName}{isCurrent && <span>Hoje</span>}
          </button>;
        })}
      </div>
      <button type="button" className="go-to-current-month" onClick={() => onSelect(today.getFullYear(), today.getMonth())}>Voltar para o mês atual</button>
    </section>
  </div>, document.body);
}

export default function MonthPickerInput({ label, value, displayValue, onChange }: {
  label: string;
  value: string;
  displayValue: string;
  onChange: (value: string) => void;
}) {
  const [isOpen, setIsOpen] = useState(false);
  const selectedDate = new Date(`${value}T12:00:00`);
  const reference = Number.isNaN(selectedDate.getTime()) ? new Date() : selectedDate;

  return <div className="field date-picker-field">
    <span>{label}</span>
    <button type="button" className="date-input-button" onClick={() => setIsOpen(true)} aria-label={label} aria-haspopup="dialog" aria-expanded={isOpen}>
      <span>{displayValue}</span>
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true"><rect x="3.5" y="5" width="17" height="15" rx="2" /><path d="M8 3v4M16 3v4M3.5 9.5h17" /></svg>
    </button>
    <MonthPickerDialog
      open={isOpen}
      selectedYear={reference.getFullYear()}
      selectedMonth={reference.getMonth()}
      onClose={() => setIsOpen(false)}
      onSelect={(year, month) => {
        onChange(`${year}-${String(month + 1).padStart(2, "0")}-01`);
        setIsOpen(false);
      }}
    />
  </div>;
}
