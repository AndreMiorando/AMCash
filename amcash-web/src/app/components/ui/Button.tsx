import { ButtonHTMLAttributes, ReactNode } from "react";

type ButtonVariant = "primary" | "secondary" | "danger";

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  fullWidth?: boolean;
  loading?: boolean;
  loadingLabel?: ReactNode;
};

export default function Button({
  variant = "primary",
  fullWidth = false,
  loading = false,
  loadingLabel = "Carregando...",
  className = "",
  disabled,
  children,
  ...props
}: ButtonProps) {
  return (
    <button
      {...props}
      className={`ui-button ui-button--${variant}${fullWidth ? " ui-button--full" : ""}${className ? ` ${className}` : ""}`}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
    >
      {loading ? loadingLabel : children}
    </button>
  );
}
