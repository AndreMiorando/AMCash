"use client";

import { useEffect, useRef, useState } from "react";

type SelectOption<T extends string> = {
  value: T;
  label: string;
};

type SelectPickerInputProps<T extends string> = {
  label: string;
  value: T | "";
  options: readonly SelectOption<T>[];
  onChange: (value: T) => void;
  placeholder?: string;
  hint?: string;
  className?: string;
};

export default function SelectPickerInput<T extends string>({
  label,
  value,
  options,
  onChange,
  placeholder = "Selecione uma opção",
  hint,
  className = "",
}: SelectPickerInputProps<T>) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const selectedLabel = options.find((option) => option.value === value)?.label;

  useEffect(() => {
    if (!isOpen) return;
    function closeOnOutsideClick(event: MouseEvent) {
      if (!containerRef.current?.contains(event.target as Node)) setIsOpen(false);
    }
    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === "Escape") setIsOpen(false);
    }
    document.addEventListener("mousedown", closeOnOutsideClick);
    document.addEventListener("keydown", closeOnEscape);
    return () => {
      document.removeEventListener("mousedown", closeOnOutsideClick);
      document.removeEventListener("keydown", closeOnEscape);
    };
  }, [isOpen]);

  return (
    <div className={`field full-field select-picker-field${className ? ` ${className}` : ""}`} ref={containerRef}>
      <span>{label}</span>
      <div className="payment-picker-control">
        <button type="button" className={`category-input-button${value ? "" : " placeholder"}`} onClick={() => setIsOpen((current) => !current)} aria-haspopup="listbox" aria-expanded={isOpen}>
          <span>{selectedLabel ?? placeholder}</span>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="m9 18 6-6-6-6" /></svg>
        </button>
        {isOpen && <div className="payment-method-options" role="listbox" aria-label={label}>
          {options.map((option) => <button type="button" role="option" aria-selected={option.value === value} className={option.value === value ? "selected" : ""} key={option.value} onClick={() => { onChange(option.value); setIsOpen(false); }}>
            <span>{option.label}</span>
            {option.value === value && <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="m5 12 4 4L19 6" /></svg>}
          </button>)}
        </div>}
      </div>
      {hint && <small className="field-hint">{hint}</small>}
    </div>
  );
}
