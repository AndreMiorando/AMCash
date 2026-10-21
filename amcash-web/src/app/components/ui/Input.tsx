import { forwardRef, InputHTMLAttributes, ReactNode } from "react";

type InputProps = InputHTMLAttributes<HTMLInputElement> & {
  invalid?: boolean;
};

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { className = "", invalid = false, ...props },
  ref,
) {
  return <input {...props} ref={ref} className={`ui-input${className ? ` ${className}` : ""}`} aria-invalid={invalid || undefined} />;
});

type InputFieldProps = InputProps & {
  label?: ReactNode;
  prefix?: ReactNode;
  suffix?: ReactNode;
  error?: ReactNode;
  fieldClassName?: string;
  controlClassName?: string;
};

export const InputField = forwardRef<HTMLInputElement, InputFieldProps>(function InputField(
  {
    label,
    prefix,
    suffix,
    error,
    fieldClassName = "",
    controlClassName = "",
    invalid = false,
    ...props
  },
  ref,
) {
  const hasAffix = prefix !== undefined || suffix !== undefined;
  return (
    <label className={`ui-field${fieldClassName ? ` ${fieldClassName}` : ""}`}>
      {label !== undefined && <span>{label}</span>}
      {hasAffix ? (
        <span className={`ui-input-control${controlClassName ? ` ${controlClassName}` : ""}`}>
          {prefix !== undefined && <b className="ui-input-affix">{prefix}</b>}
          <Input {...props} ref={ref} invalid={invalid || Boolean(error)} />
          {suffix !== undefined && <b className="ui-input-affix">{suffix}</b>}
        </span>
      ) : <Input {...props} ref={ref} invalid={invalid || Boolean(error)} />}
      {error && <small className="ui-field-error">{error}</small>}
    </label>
  );
});
