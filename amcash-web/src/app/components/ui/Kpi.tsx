import type { HTMLAttributes } from "react";

type SecondaryKpiGroupProps = HTMLAttributes<HTMLElement> & {
  columns?: 2 | 3;
};

function classes(...values: Array<string | undefined>) {
  return values.filter(Boolean).join(" ");
}

export function PrimaryKpiCard({ className, ...props }: HTMLAttributes<HTMLElement>) {
  return <section className={classes("ui-primary-kpi", className)} {...props} />;
}

export function SecondaryKpiGroup({ className, columns = 2, ...props }: SecondaryKpiGroupProps) {
  return <section className={classes("ui-secondary-kpis", `ui-secondary-kpis--${columns}`, className)} {...props} />;
}

export function SecondaryKpiItem({ className, ...props }: HTMLAttributes<HTMLElement>) {
  return <article className={classes("ui-secondary-kpi", className)} {...props} />;
}

export function KpiLabel({ className, ...props }: HTMLAttributes<HTMLSpanElement>) {
  return <span className={classes("ui-kpi-label", className)} {...props} />;
}

export function KpiValue({ className, ...props }: HTMLAttributes<HTMLElement>) {
  return <strong className={classes("ui-kpi-value", className)} {...props} />;
}
