import { ReactNode } from "react";
import { ApiWorkCostCategory } from "@/lib/api";

export const workCostCategoryLabels: Record<ApiWorkCostCategory, string> = {
  FUEL: "Combustível",
  MAINTENANCE: "Manutenção",
  TOLL: "Pedágio",
  WASH: "Lavagem",
  FOOD: "Alimentação",
  OTHER: "Outro",
};

export function CostCategoryIcon({ category }: { category: ApiWorkCostCategory }) {
  const paths: Record<ApiWorkCostCategory, ReactNode> = {
    FUEL: <><path d="M4 21V5a2 2 0 0 1 2-2h7a2 2 0 0 1 2 2v16" /><path d="M3 21h13M7 7h5v4H7zM15 8h2l2 2v7a1.5 1.5 0 0 0 3 0v-5l-2-2" /></>,
    MAINTENANCE: <path d="M14.5 6.5a4 4 0 0 0-5-5L7 4l3 3 2.5-2.5a4 4 0 0 0 2 5L6 18l-1 3 3-1 8.5-8.5a4 4 0 0 0 5-5L19 9l-3-3 2.5-2.5" />,
    TOLL: <><circle cx="7" cy="16" r="3" /><circle cx="12" cy="11" r="3" /><circle cx="17" cy="6" r="3" /></>,
    WASH: <><path d="M12 2C9 7 6 10 6 14a6 6 0 0 0 12 0c0-4-3-7-6-12Z" /><path d="M9.5 16.5c.8 1.2 2 1.7 3.5 1.4" /></>,
    FOOD: <><path d="M6 3v7M9 3v7M6 7h3M7.5 10v11" /><path d="M15.5 3v18M15.5 3c3 2 3.2 7 0 9" /></>,
    OTHER: <><circle cx="5" cy="12" r="1.4" fill="currentColor" stroke="none" /><circle cx="12" cy="12" r="1.4" fill="currentColor" stroke="none" /><circle cx="19" cy="12" r="1.4" fill="currentColor" stroke="none" /></>,
  };

  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[category]}</svg>;
}

export function CostCategoryGrid({
  value,
  onChange,
  ariaLabel,
}: {
  value: ApiWorkCostCategory;
  onChange: (value: ApiWorkCostCategory) => void;
  ariaLabel: string;
}) {
  return <div className="cost-category-grid" role="listbox" aria-label={ariaLabel}>
    {(Object.keys(workCostCategoryLabels) as ApiWorkCostCategory[]).map((category) =>
      <button type="button" role="option" aria-selected={category === value} className={category === value ? "selected" : ""} key={category} onClick={() => onChange(category)}>
        <CostCategoryIcon category={category} />
        <span>{workCostCategoryLabels[category]}</span>
      </button>)}
  </div>;
}
