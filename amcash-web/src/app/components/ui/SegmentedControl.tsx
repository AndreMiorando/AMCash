import { CSSProperties, KeyboardEvent, ReactNode, useRef } from "react";

export type SegmentedOption<T extends string> = {
  value: T;
  label: ReactNode;
  disabled?: boolean;
};

type SegmentedControlProps<T extends string> = {
  value: T;
  options: readonly SegmentedOption<T>[];
  onChange: (value: T) => void;
  ariaLabel: string;
  className?: string;
  disabled?: boolean;
};

export default function SegmentedControl<T extends string>({
  value,
  options,
  onChange,
  ariaLabel,
  className = "",
  disabled = false,
}: SegmentedControlProps<T>) {
  const buttons = useRef<Array<HTMLButtonElement | null>>([]);

  function moveSelection(event: KeyboardEvent<HTMLButtonElement>, currentIndex: number) {
    if (!["ArrowLeft", "ArrowRight", "ArrowUp", "ArrowDown", "Home", "End"].includes(event.key)) return;
    event.preventDefault();
    const enabledIndexes = options.map((option, index) => ({ option, index })).filter(({ option }) => !disabled && !option.disabled).map(({ index }) => index);
    if (!enabledIndexes.length) return;
    const currentPosition = enabledIndexes.indexOf(currentIndex);
    const nextPosition = event.key === "Home"
      ? 0
      : event.key === "End"
        ? enabledIndexes.length - 1
        : (currentPosition + (["ArrowRight", "ArrowDown"].includes(event.key) ? 1 : -1) + enabledIndexes.length) % enabledIndexes.length;
    const nextIndex = enabledIndexes[nextPosition];
    buttons.current[nextIndex]?.focus();
    onChange(options[nextIndex].value);
  }

  return (
    <div
      className={`ui-segmented${className ? ` ${className}` : ""}`}
      role="radiogroup"
      aria-label={ariaLabel}
      style={{ "--ui-segment-count": options.length } as CSSProperties}
    >
      {options.map((option, index) => {
        const selected = option.value === value;
        return (
          <button
            key={option.value}
            ref={(element) => { buttons.current[index] = element; }}
            type="button"
            role="radio"
            aria-checked={selected}
            className={selected ? "selected" : ""}
            disabled={disabled || option.disabled}
            tabIndex={selected ? 0 : -1}
            onClick={() => onChange(option.value)}
            onKeyDown={(event) => moveSelection(event, index)}
          >
            {option.label}
          </button>
        );
      })}
    </div>
  );
}
