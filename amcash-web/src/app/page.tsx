"use client";

import Image from "next/image";
import Script from "next/script";
import { createContext, useCallback, useContext, useEffect, useRef, useState } from "react";
import type { PointerEvent as ReactPointerEvent } from "react";
import {
  ApiEntry,
  ApiEntryCategory,
  ApiError,
  ApiFinancialNotification,
  ApiForecast,
  ApiGoal,
  ApiHome,
  ApiMileageSource,
  ApiMileageSuggestions,
  ApiPaymentMethod,
  ApiWorkCostCategory,
  ApiWorkEarningSource,
  ApiWorkOverview,
  ApiRecurrenceFrequency,
  ApiSeriesScope,
  ApiSubexpense,
  ApiUsageProfile,
  ApiVehicle,
  financeApi,
} from "@/lib/api";
import DatePickerInput, { dateToInputValue } from "@/app/components/DatePickerInput";
import { MonthPickerDialog } from "@/app/components/MonthPickerInput";
import WorkScreen from "@/app/components/WorkScreen";
import VehicleScreen from "@/app/components/VehicleScreen";
import OperationalCostScreen from "@/app/components/OperationalCostScreen";
import CostDialog, { CostDialogValue } from "@/app/components/CostDialog";
import EarningSourceDialog from "@/app/components/EarningSourceDialog";
import AppHeader from "@/app/components/AppHeader";
import { CostCategoryGrid, CostCategoryIcon, workCostCategoryLabels } from "@/app/components/CostCategoryGrid";
import Button from "@/app/components/ui/Button";
import { Input, InputField } from "@/app/components/ui/Input";
import SegmentedControl from "@/app/components/ui/SegmentedControl";
import SelectPickerInput from "@/app/components/ui/SelectPickerInput";
import { KpiLabel, KpiValue, PrimaryKpiCard, SecondaryKpiGroup, SecondaryKpiItem } from "@/app/components/ui/Kpi";
import { completeCurrencyInput, maskCurrencyInput, parseCurrencyInput, formatCompactCurrency, maskLimitedCurrencyInput } from "@/lib/currency";
import { formatDateRange, formatLongDate, formatMonthYear, formatShortDate } from "@/lib/date";
import { isWorkPlatformSource, profileWorkPlatforms } from "@/lib/workPlatforms";

type GoogleCredentialResponse = {
  credential?: string;
};

type AuthenticatedUser = {
  id: string;
  name: string;
  email: string;
  pictureUrl: string | null;
  usageProfile: ApiUsageProfile | null;
  deletionScheduledAt?: string | null;
};

type GoogleLoginResponse = {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: AuthenticatedUser;
};

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (options: {
            client_id: string;
            callback: (response: GoogleCredentialResponse) => void;
          }) => void;
          renderButton: (
            parent: HTMLElement,
            options: {
              type: "standard";
              theme: "outline";
              size: "large";
              text: "signin_with" | "signup_with";
              shape: "pill";
              width: number;
              logo_alignment: "left";
              locale: string;
            },
          ) => void;
        };
      };
    };
  }
}

type RecurrenceFrequency = "none" | "daily" | "weekly" | "monthly";
type TransactionFilter = "pending" | "paid" | "all";
const recurrenceFrequencyOptions = [
  { value: "daily", label: "Diária" },
  { value: "weekly", label: "Semanal" },
  { value: "monthly", label: "Mensal" },
] satisfies ReadonlyArray<{ value: Exclude<RecurrenceFrequency, "none">; label: string }>;
const itemRecurrenceOptions = [
  { value: "none", label: "Não repetir" },
  ...recurrenceFrequencyOptions,
] satisfies ReadonlyArray<{ value: RecurrenceFrequency; label: string }>;
const transactionFilterOptions = [
  { value: "all", label: "Todos" },
  { value: "pending", label: "Pendentes" },
  { value: "paid", label: "Pagas" },
] satisfies ReadonlyArray<{ value: TransactionFilter; label: string }>;
const expenseCategories: { value: ApiEntryCategory; label: string }[] = [
  { value: "FOOD", label: "Alimentação" },
  { value: "HOUSING", label: "Moradia" },
  { value: "TRANSPORTATION", label: "Transporte" },
  { value: "HEALTH", label: "Saúde" },
  { value: "EDUCATION", label: "Educação" },
  { value: "CLOTHING", label: "Vestuário" },
  { value: "LEISURE", label: "Lazer" },
  { value: "BILLS_AND_SERVICES", label: "Contas e serviços" },
  { value: "SUBSCRIPTIONS", label: "Assinaturas" },
  { value: "CREDIT_CARD", label: "Cartão de crédito" },
  { value: "LOANS_AND_FINANCING", label: "Empréstimos e financiamentos" },
  { value: "FINANCIAL", label: "Serviços financeiros" },
  { value: "INSURANCE", label: "Seguros" },
  { value: "TAXES", label: "Impostos" },
  { value: "FAMILY", label: "Família" },
  { value: "PETS", label: "Pets" },
  { value: "PERSONAL_CARE", label: "Cuidados pessoais" },
  { value: "TRAVEL", label: "Viagens" },
  { value: "SHOPPING", label: "Compras" },
  { value: "OTHER_EXPENSE", label: "Outras despesas" },
];

const incomeCategories: { value: ApiEntryCategory; label: string }[] = [
  { value: "SALARY", label: "Salário" },
  { value: "FREELANCE", label: "Trabalho autônomo" },
  { value: "BUSINESS", label: "Negócio" },
  { value: "INVESTMENT_INCOME", label: "Rendimentos" },
  { value: "RENTAL_INCOME", label: "Aluguel recebido" },
  { value: "BENEFITS", label: "Benefícios" },
  { value: "REFUND", label: "Reembolso" },
  { value: "GIFT", label: "Presente recebido" },
  { value: "UBER", label: "Uber" },
  { value: "NINETY_NINE", label: "99" },
  { value: "OTHER_INCOME", label: "Outras receitas" },
];

type Transaction = {
  id: string;
  name: string;
  category: ApiEntryCategory;
  date: string;
  value: number;
  balance: number;
  kind: "food" | "card" | "home" | "shield" | "phone" | "wifi" | "wallet";
  income: boolean;
  hasSubexpenses: boolean;
  recurrenceFrequency: RecurrenceFrequency;
  recurrenceCount: number;
  recurrenceIndex: number;
  seriesId?: string;
  paid: boolean;
  paidOccurrences: number;
  totalOccurrences: number;
  subexpenses: Subexpense[];
  paymentMethod: ApiPaymentMethod | null;
  notes: string;
};

type TransactionGroup = {
  date: string;
  items: Transaction[];
  total: number;
};

type NewTransaction = {
  name: string;
  category: ApiEntryCategory;
  date: string;
  value: number;
  income: boolean;
  recurrenceFrequency: RecurrenceFrequency;
  recurrenceCount: number;
  hasSubexpenses: boolean;
  items: { name: string; value: number; recurrenceFrequency: RecurrenceFrequency; recurrenceCount: number }[];
  paymentMethod: ApiPaymentMethod | null;
  notes: string;
};

const expensePaymentMethods: { value: ApiPaymentMethod; label: string }[] = [
  { value: "CASH", label: "Dinheiro" },
  { value: "PIX", label: "Pix" },
  { value: "DEBIT_CARD", label: "Cartão de débito" },
  { value: "CREDIT_CARD", label: "Cartão de crédito" },
  { value: "BANK_TRANSFER", label: "Transferência bancária" },
  { value: "OTHER", label: "Outra" },
];

const incomePaymentMethods = expensePaymentMethods.filter(({ value }) => value !== "DEBIT_CARD" && value !== "CREDIT_CARD");

type Subexpense = {
  id: string;
  name: string;
  amount: string;
  installment: string;
  paid: boolean;
  recurrenceFrequency: RecurrenceFrequency;
  recurrenceCount: number;
  recurrenceIndex: number;
  seriesId?: string;
};

type IconName = Transaction["kind"] | "search" | "more" | "back" | "forward" | "home" | "radar" | "target" | "work" | "edit" | "trash" | "plus" | "check" | "save" | "logout" | "clock" | "calendar" | "income" | "expense" | "bell" | "warning";

function Icon({ name }: { name: IconName }) {
  if (name === "more") {
    return <svg viewBox="0 0 24 24"><circle cx="12" cy="5" r="1.6" /><circle cx="12" cy="12" r="1.6" /><circle cx="12" cy="19" r="1.6" /></svg>;
  }

  const paths = {
    search: <><circle cx="10.8" cy="10.8" r="6.2" /><path d="m15.4 15.4 4.2 4.2" /></>,
    back: <path d="m15 18-6-6 6-6" />,
    forward: <path d="m9 18 6-6-6-6" />,
    home: <><path d="m3.5 10 8.5-7 8.5 7" /><path d="M5.5 9v11h13V9M9.5 20v-6h5v6" /></>,
    radar: <><path d="M4 20V10M10 20V4M16 20v-7M22 20H2" /><path d="m3 7 6-4 6 6 6-5" /></>,
    target: <><circle cx="12" cy="12" r="8" /><circle cx="12" cy="12" r="3" /><path d="M12 2v3M22 12h-3M12 22v-3M2 12h3" /></>,
    work: <><rect x="3" y="7" width="18" height="13" rx="2" /><path d="M8 7V4h8v3M3 12h18M10 12v2h4v-2" /></>,
    food: <><path d="M6 3v7M9 3v7M6 7h3M7.5 10v11" /><path d="M15.5 3v18M15.5 3c3 2 3.2 7 0 9" /></>,
    card: <><rect x="3" y="5" width="18" height="14" rx="2" /><path d="M3 9h18M7 15h4" /></>,
    shield: <><path d="M12 3 5 6v5c0 4.6 3 8.2 7 10 4-1.8 7-5.4 7-10V6l-7-3Z" /><path d="m9.5 12 1.7 1.7 3.5-4" /></>,
    phone: <><rect x="6.5" y="2.5" width="11" height="19" rx="2" /><path d="M10 5h4M11 18.5h2" /></>,
    wifi: <><path d="M3.5 9a13 13 0 0 1 17 0M6.5 12.5a8.5 8.5 0 0 1 11 0M9.5 16a4 4 0 0 1 5 0" /><circle cx="12" cy="19" r="1" fill="currentColor" stroke="none" /></>,
    wallet: <><path d="M4 6.5h14a2 2 0 0 1 2 2v10H4a2 2 0 0 1-2-2v-12a2 2 0 0 1 2-2h12" /><path d="M15 11h6v4h-6a2 2 0 0 1 0-4Z" /></>,
    edit: <><path d="m14 5 5 5M4 20l3.5-.8L19 7.7a2 2 0 0 0-2.7-2.7L4.8 16.5 4 20Z" /></>,
    trash: <><path d="M4 7h16M9 7V4h6v3M7 7l1 14h8l1-14M10 11v6M14 11v6" /></>,
    plus: <path d="M12 5v14M5 12h14" />,
    check: <path d="m5 12 4.5 4.5L19 7" />,
    save: <><path d="M5 3h12l2 2v16H5V3Z" /><path d="M8 3v6h8V3M8 21v-7h8v7" /></>,
    logout: <><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" /><path d="m16 17 5-5-5-5" /><path d="M21 12H9" /></>,
    clock: <><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>,
    calendar: <><rect x="3.5" y="5" width="17" height="15" rx="2" /><path d="M8 3v4M16 3v4M3.5 9.5h17" /></>,
    income: <><path d="M17 7 7 17M7 9v8h8" /></>,
    expense: <><path d="m7 17 10-10M9 7h8v8" /></>,
    bell: <><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" /><path d="M10 21h4" /></>,
    warning: <><path d="M10.3 3.7 2.6 17a2 2 0 0 0 1.7 3h15.4a2 2 0 0 0 1.7-3L13.7 3.7a2 2 0 0 0-3.4 0Z" /><path d="M12 9v4M12 17h.01" /></>,
  };

  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">{paths[name]}</svg>;
}

type NavigationTab = "home" | "finance" | "radar" | "goals" | "work" | "vehicles" | "operational-cost";
const NavigationLoadingContext = createContext<NavigationTab | null>(null);
const VehicleNavigationContext = createContext<(() => void) | null>(null);
const OperationalCostNavigationContext = createContext<(() => void) | null>(null);

function BottomNavigation({ active, loading = false, onHome, onFinance, onRadar, onGoals, onWork, showWorkFeatures = true }: {
  active: NavigationTab;
  loading?: boolean;
  onHome: () => void;
  onFinance: () => void;
  onRadar: () => void;
  onGoals: () => void;
  onWork: () => void;
  showWorkFeatures?: boolean;
}) {
  const [isMoreOpen, setIsMoreOpen] = useState(false);
  const [moreDragOffset, setMoreDragOffset] = useState(0);
  const [isMoreDragging, setIsMoreDragging] = useState(false);
  const moreDragStart = useRef<number | null>(null);
  const pendingTab = useContext(NavigationLoadingContext);
  const onVehicles = useContext(VehicleNavigationContext);
  const onOperationalCost = useContext(OperationalCostNavigationContext);
  const loadingTab = pendingTab ?? (loading ? active : null);
  const moreContainsActiveTab = active === "goals" || active === "radar" || active === "vehicles" || active === "operational-cost";
  const moreIsActive = isMoreOpen || moreContainsActiveTab;
  const moreIsLoading = loadingTab === "goals" || loadingTab === "radar" || loadingTab === "vehicles" || loadingTab === "operational-cost";
  const navigationIcon = (tab: "home" | "finance" | "radar" | "goals" | "work", icon: IconName) => (
    loadingTab === tab ? <span className="nav-icon-loader" role="status" aria-label="Carregando página" /> : <Icon name={icon} />
  );
  useEffect(() => {
    if (!isMoreOpen) return;
    const closeOnEscape = (event: KeyboardEvent) => { if (event.key === "Escape") setIsMoreOpen(false); };
    window.addEventListener("keydown", closeOnEscape);
    return () => window.removeEventListener("keydown", closeOnEscape);
  }, [isMoreOpen]);
  function startMoreDrag(event: ReactPointerEvent<HTMLElement>) {
    if (event.pointerType === "mouse" && event.button !== 0) return;
    if ((event.target as HTMLElement).closest("button")) return;
    moreDragStart.current = event.clientY;
    setIsMoreDragging(true);
    event.currentTarget.setPointerCapture(event.pointerId);
  }
  function moveMoreDrag(event: ReactPointerEvent<HTMLElement>) {
    if (moreDragStart.current == null) return;
    const offset = Math.max(0, event.clientY - moreDragStart.current);
    setMoreDragOffset(offset);
    if (offset > 0) event.preventDefault();
  }
  function finishMoreDrag(event: ReactPointerEvent<HTMLElement>) {
    if (moreDragStart.current == null) return;
    const offset = Math.max(0, event.clientY - moreDragStart.current);
    moreDragStart.current = null;
    setIsMoreDragging(false);
    setMoreDragOffset(0);
    if (event.currentTarget.hasPointerCapture(event.pointerId)) event.currentTarget.releasePointerCapture(event.pointerId);
    if (offset >= 72) setIsMoreOpen(false);
  }
  return (
    <>
    <nav className="bottom-nav" aria-label="Navegação principal">
      <button type="button" className={`nav-item${!isMoreOpen && active === "home" ? " active" : ""}`} onClick={onHome} aria-current={active === "home" ? "page" : undefined} aria-busy={loadingTab === "home"}>
        {navigationIcon("home", "home")}
        <span>Home</span>
      </button>
      <button type="button" className={`nav-item${!isMoreOpen && active === "finance" ? " active" : ""}`} onClick={onFinance} aria-current={active === "finance" ? "page" : undefined} aria-busy={loadingTab === "finance"}>
        {navigationIcon("finance", "wallet")}
        <span>Finanças</span>
      </button>
      {showWorkFeatures && <button type="button" className={`nav-item${!isMoreOpen && active === "work" ? " active" : ""}`} onClick={onWork} aria-current={active === "work" ? "page" : undefined} aria-busy={loadingTab === "work"}>
        {navigationIcon("work", "work")}
        <span>Resultado</span>
      </button>}
      <button type="button" className={`nav-item${moreIsActive ? " active" : ""}`} onClick={() => setIsMoreOpen((current) => !current)} aria-current={moreContainsActiveTab ? "page" : undefined} aria-expanded={isMoreOpen} aria-haspopup="dialog" aria-busy={moreIsLoading}>
        {moreIsLoading ? <span className="nav-icon-loader" role="status" aria-label="Carregando página" /> : <svg className="nav-more-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true"><circle cx="7" cy="7" r="2.25" /><circle cx="17" cy="7" r="2.25" /><circle cx="7" cy="17" r="2.25" /><circle cx="17" cy="17" r="2.25" /></svg>}
        <span>Mais</span>
      </button>
    </nav>
    {isMoreOpen && <div className="more-menu-backdrop" role="presentation" onMouseDown={() => setIsMoreOpen(false)}>
      <section className={`more-menu-sheet${isMoreDragging ? " dragging" : ""}`} style={{ translate: `0 ${moreDragOffset}px` }} role="dialog" aria-modal="true" aria-label="Mais opções" onMouseDown={(event) => event.stopPropagation()} onPointerDown={startMoreDrag} onPointerMove={moveMoreDrag} onPointerUp={finishMoreDrag} onPointerCancel={finishMoreDrag}>
        <div className="more-menu-handle" aria-hidden="true" />
        <div className="more-menu-actions">
          {showWorkFeatures && <button type="button" className={active === "goals" ? "active" : ""} onClick={() => { setIsMoreOpen(false); onGoals(); }}>
            <span className="more-menu-icon goals"><Icon name="target" /></span><strong>Objetivos</strong><Icon name="forward" />
          </button>}
          {showWorkFeatures && onVehicles && <button type="button" className={active === "vehicles" ? "active" : ""} onClick={() => { setIsMoreOpen(false); onVehicles(); }}>
            <span className="more-menu-icon vehicles"><Image src="/vehicle-menu-icon.svg" width={24} height={24} alt="" /></span><strong>Veículos</strong><Icon name="forward" />
          </button>}
          {showWorkFeatures && onOperationalCost && <button type="button" className={active === "operational-cost" ? "active" : ""} onClick={() => { setIsMoreOpen(false); onOperationalCost(); }}>
            <span className="more-menu-icon operational-cost"><Icon name="wallet" /></span><strong>Custo operacional</strong><Icon name="forward" />
          </button>}
          <button type="button" className={active === "radar" ? "active" : ""} onClick={() => { setIsMoreOpen(false); onRadar(); }}>
            <span className="more-menu-icon radar"><Icon name="radar" /></span><strong>Projeção</strong><Icon name="forward" />
          </button>
        </div>
      </section>
    </div>}
    </>
  );
}

function formatKpiCurrency(value: number) {
  return formatCompactCurrency(Math.abs(value));
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(Math.abs(value));
}

function formatDate(date: string) {
  return formatLongDate(date);
}

function formatTransactionGroupDate(date: string) {
  const parsed = new Date(`${date}T12:00:00`);
  const calendarDate = formatLongDate(parsed);
  const weekday = new Intl.DateTimeFormat("pt-BR", { weekday: "long" }).format(parsed);
  return { calendarDate, weekday };
}

function maskRepetitionCount(value: string) {
  return value.replace(/\D/g, "").slice(0, 3);
}

function maskDecimalInput(value: string) {
  const cleaned = value.replace(/[^\d,.]/g, "").replace(".", ",");
  const [integer = "", ...decimalParts] = cleaned.split(",");
  const decimals = decimalParts.join("").slice(0, 2);
  return decimalParts.length ? `${integer},${decimals}` : integer;
}

function parseDecimalInput(value: string) {
  return Number(value.replace(",", ".")) || 0;
}

const recurrenceToApi: Record<RecurrenceFrequency, ApiRecurrenceFrequency> = {
  none: "NONE",
  daily: "DAILY",
  weekly: "WEEKLY",
  monthly: "MONTHLY",
};

const recurrenceFromApi: Record<ApiRecurrenceFrequency, RecurrenceFrequency> = {
  NONE: "none",
  DAILY: "daily",
  WEEKLY: "weekly",
  MONTHLY: "monthly",
};

function mapSubexpense(item: ApiSubexpense): Subexpense {
  return {
    id: item.id,
    name: item.name,
    amount: Number(item.amount).toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 }),
    installment: item.installmentDescription ?? (item.recurrenceFrequency === "NONE" ? "Única" : `${item.recurrenceIndex + 1}/${item.recurrenceCount}`),
    paid: item.paid,
    recurrenceFrequency: recurrenceFromApi[item.recurrenceFrequency],
    recurrenceCount: item.recurrenceCount,
    recurrenceIndex: item.recurrenceIndex,
    seriesId: item.seriesId ?? undefined,
  };
}

function SeriesScopeDialog({
  title,
  description,
  currentLabel = "Somente este lançamento",
  danger = false,
  busy,
  onCancel,
  onSelect,
}: {
  title: string;
  description: string;
  currentLabel?: string;
  danger?: boolean;
  busy: boolean;
  onCancel: () => void;
  onSelect: (scope: ApiSeriesScope) => void;
}) {
  return (
    <div className="sheet-backdrop confirm-backdrop" role="presentation" onMouseDown={() => !busy && onCancel()}>
      <section className={`confirm-dialog scope-dialog${danger ? " scope-dialog--danger" : ""}`} role="alertdialog" aria-modal="true" aria-labelledby="series-scope-title" onMouseDown={(event) => event.stopPropagation()}>
        <h2 id="series-scope-title">{title}</h2>
        <p>{description}</p>
        <div className="scope-actions">
          <button type="button" disabled={busy} onClick={onCancel}>Cancelar</button>
          <button type="button" disabled={busy} onClick={() => onSelect("CURRENT")}>{currentLabel}</button>
          <button type="button" disabled={busy} onClick={() => onSelect("CURRENT_AND_FUTURE")}>Este e os futuros</button>
          <button type="button" disabled={busy} onClick={() => onSelect("ALL")}>Toda a série, incluindo anteriores</button>
        </div>
      </section>
    </div>
  );
}

function mapEntries(entries: ApiEntry[]) {
  let balance = 0;
  return entries.map((entry): Transaction => {
    const income = entry.type === "INCOME";
    balance += income ? Number(entry.amount) : entry.paid ? 0 : -Number(entry.amount);
    return {
      id: entry.id,
      name: entry.name,
      category: entry.category,
      date: entry.dueDate,
      value: Number(entry.amount),
      balance,
      kind: iconForCategory(entry.category, income),
      income,
      hasSubexpenses: entry.hasSubexpenses,
      recurrenceFrequency: recurrenceFromApi[entry.recurrenceFrequency],
      recurrenceCount: entry.recurrenceCount,
      recurrenceIndex: entry.recurrenceIndex,
      seriesId: entry.seriesId ?? undefined,
      paid: entry.paid,
      paidOccurrences: entry.paidOccurrences,
      totalOccurrences: entry.totalOccurrences,
      subexpenses: entry.subexpenses.map(mapSubexpense),
      paymentMethod: entry.paymentMethod ?? null,
      notes: entry.notes ?? "",
    };
  });
}

function iconForCategory(category: ApiEntryCategory, income: boolean): Transaction["kind"] {
  if (income) return "wallet";
  if (category === "FOOD") return "food";
  if (category === "HOUSING") return "home";
  if (category === "HEALTH") return "shield";
  if (category === "BILLS_AND_SERVICES") return "wifi";
  return "card";
}

function messageFromError(error: unknown) {
  return error instanceof Error ? error.message : "Não foi possível concluir a operação.";
}

function CategoryPickerInput({
  value,
  options,
  onChange,
}: {
  value: ApiEntryCategory | "";
  options: { value: ApiEntryCategory; label: string }[];
  onChange: (value: ApiEntryCategory) => void;
}) {
  const [isOpen, setIsOpen] = useState(false);
  const [query, setQuery] = useState("");
  const selectedLabel = options.find((option) => option.value === value)?.label;
  const income = options === incomeCategories;
  const normalizedQuery = query.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLocaleLowerCase("pt-BR").trim();
  const filteredOptions = normalizedQuery
    ? options.filter((option) => option.label.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLocaleLowerCase("pt-BR").includes(normalizedQuery))
    : options;

  function closePicker() {
    setQuery("");
    setIsOpen(false);
  }

  return (
    <div className="field full-field category-field">
      <span>Categoria</span>
      <button
        type="button"
        className={`category-input-button${value ? "" : " placeholder"}`}
        onClick={() => setIsOpen(true)}
        aria-haspopup="dialog"
        aria-expanded={isOpen}
      >
        <span className="category-input-content">
          {value && <span className="category-option-icon"><Icon name={iconForCategory(value, income)} /></span>}
          <span>{selectedLabel ?? "Selecione uma categoria"}</span>
        </span>
        <Icon name="forward" />
      </button>

      {isOpen && (
        <div className="category-picker-backdrop" role="presentation">
          <section className="category-picker-dialog" role="dialog" aria-modal="true" aria-labelledby="category-picker-title">
            <div className="category-picker-heading">
              <button type="button" onClick={closePicker} aria-label="Voltar"><Icon name="back" /></button>
              <h2 id="category-picker-title">Selecionar categoria</h2>
            </div>
            <label className="category-picker-search">
              <Icon name="search" />
              <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Buscar categoria..." autoFocus />
              {query && <button type="button" onClick={() => setQuery("")} aria-label="Limpar busca">×</button>}
            </label>
            <div className="category-picker-options">
              {filteredOptions.map((option) => (
                <button
                  type="button"
                  key={option.value}
                  className={value === option.value ? "selected" : ""}
                  onClick={() => { onChange(option.value); closePicker(); }}
                  aria-pressed={value === option.value}
                >
                  <span className="category-option-icon"><Icon name={iconForCategory(option.value, income)} /></span>
                  <span>{option.label}</span>
                  <Icon name={value === option.value ? "check" : "forward"} />
                </button>
              ))}
              {filteredOptions.length === 0 && <p className="category-picker-empty">Nenhuma categoria encontrada.</p>}
            </div>
          </section>
        </div>
      )}
    </div>
  );
}

function PaymentMethodPickerInput({
  label,
  value,
  options,
  onChange,
}: {
  label: string;
  value: ApiPaymentMethod | "";
  options: { value: ApiPaymentMethod; label: string }[];
  onChange: (value: ApiPaymentMethod | "") => void;
}) {
  return <SelectPickerInput label={label} value={value} options={options} onChange={onChange} className="transaction-method-field" hint="Informação registrada apenas para consulta e auditoria." />;
}

function LoadingScreen() {
  return (
    <main className="loading-screen" aria-label="Carregando AMCash">
      <div className="background-orb background-orb--top" aria-hidden="true" />
      <div className="background-orb background-orb--bottom" aria-hidden="true" />

      <section className="brand" aria-labelledby="brand-name">
        <div className="brand-mark" aria-hidden="true">
          <Image src="/amcash-logo.png" alt="" width={1254} height={1254} priority />
        </div>
        <h1 id="brand-name" className="brand-name"><span>AM</span><span>Cash</span></h1>
        <p className="tagline">Suas finanças no seu ritmo</p>
        <div className="loader" role="status" aria-label="Carregando"><span className="sr-only">Carregando</span></div>
      </section>
    </main>
  );
}

function AuthScreen({ onAuthenticated }: { onAuthenticated: (user: AuthenticatedUser) => void }) {
  const [mode, setMode] = useState<"login" | "signup">("login");
  const [scriptReady, setScriptReady] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState("");
  const googleButton = useRef<HTMLDivElement>(null);
  const clientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID;
  const apiUrl = (process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080").replace(/\/$/, "");

  const authenticate = useCallback(async (response: GoogleCredentialResponse) => {
    if (!response.credential) {
      setError("Não foi possível receber seus dados do Google. Tente novamente.");
      return;
    }

    setIsSubmitting(true);
    setError("");

    try {
      const result = await fetch(`${apiUrl}/api/v1/auth/google`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ idToken: response.credential }),
      });

      if (!result.ok) {
        throw new Error("A autenticação não pôde ser concluída.");
      }

      const session = await result.json() as GoogleLoginResponse;
      window.localStorage.setItem("amcash.accessToken", session.accessToken);
      window.localStorage.setItem("amcash.user", JSON.stringify(session.user));
      onAuthenticated(session.user);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Não foi possível entrar agora.");
      setIsSubmitting(false);
    }
  }, [apiUrl, onAuthenticated]);

  useEffect(() => {
    if (!scriptReady || !clientId || !window.google || !googleButton.current) return;

    googleButton.current.replaceChildren();
    window.google.accounts.id.initialize({ client_id: clientId, callback: authenticate });
    window.google.accounts.id.renderButton(googleButton.current, {
      type: "standard",
      theme: "outline",
      size: "large",
      text: mode === "signup" ? "signup_with" : "signin_with",
      shape: "pill",
      width: 304,
      logo_alignment: "left",
      locale: "pt-BR",
    });
  }, [authenticate, clientId, mode, scriptReady]);

  return (
    <main className="auth-screen">
      <Script
        src="https://accounts.google.com/gsi/client"
        strategy="afterInteractive"
        onReady={() => setScriptReady(true)}
        onError={() => setError("Não foi possível carregar o acesso com Google.")}
      />

      <div className="auth-orb auth-orb--top" aria-hidden="true" />
      <div className="auth-orb auth-orb--bottom" aria-hidden="true" />

      <header className="auth-header" aria-label="AMCash">
        <Image src="/amcash-logo.png" alt="" width={1254} height={1254} priority />
        <span><b>AM</b>Cash</span>
      </header>

      <section className="auth-card" aria-labelledby="auth-title">
        <div className="auth-symbol" aria-hidden="true">
          <Image src="/amcash-logo.png" alt="" width={1254} height={1254} priority />
        </div>

        <p className="auth-eyebrow">SUAS FINANÇAS NO SEU RITMO</p>
        <h1 id="auth-title">{mode === "login" ? "Que bom ter você de volta" : "Comece sua jornada financeira"}</h1>
        <p className="auth-description">
          {mode === "login"
            ? "Entre para acompanhar seus gastos, receitas e planos em um só lugar."
            : "Crie sua conta em poucos segundos e assuma o controle da sua vida financeira."}
        </p>

        <div className="auth-tabs" role="tablist" aria-label="Acesso à conta">
          <button type="button" role="tab" aria-selected={mode === "login"} className={mode === "login" ? "active" : ""} onClick={() => { setMode("login"); setError(""); }}>Entrar</button>
          <button type="button" role="tab" aria-selected={mode === "signup"} className={mode === "signup" ? "active" : ""} onClick={() => { setMode("signup"); setError(""); }}>Criar conta</button>
        </div>

        <div className={`google-login-area${isSubmitting ? " is-loading" : ""}`} aria-busy={isSubmitting}>
          {clientId ? (
            <div ref={googleButton} className="google-rendered-button" />
          ) : (
            <button type="button" className="google-placeholder" onClick={() => setError("Configure NEXT_PUBLIC_GOOGLE_CLIENT_ID para ativar o acesso.")}>
              <span className="google-g" aria-hidden="true">G</span>
              {mode === "login" ? "Continuar com o Google" : "Cadastrar com o Google"}
            </button>
          )}
          {isSubmitting && <span className="auth-spinner" aria-label="Autenticando" />}
        </div>

        {error && <p className="auth-error" role="alert">{error}</p>}

        <div className="auth-security">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true"><path d="M12 3 5 6v5c0 4.6 3 8.2 7 10 4-1.8 7-5.4 7-10V6l-7-3Z" /><path d="m9.5 12 1.7 1.7 3.5-4" /></svg>
          <span>Seus dados são protegidos e sua senha do Google nunca é compartilhada.</span>
        </div>
      </section>

      <footer className="auth-footer">
        Ao continuar, você concorda com os <a href="#termos">Termos de Uso</a> e a <a href="#privacidade">Política de Privacidade</a>.
      </footer>
    </main>
  );
}

function CreateTransactionSheet({
  defaultDate,
  initialType,
  onClose,
  onCreate,
}: {
  defaultDate: string;
  initialType: "expense" | "income";
  onClose: () => void;
  onCreate: (transaction: NewTransaction) => Promise<void>;
}) {
  type DraftExpenseItem = { id: string; name: string; amount: string; recurrenceFrequency: RecurrenceFrequency; recurrenceCount: string };
  type LaunchType = "single" | "recurring";

  const type = initialType;
  const [name, setName] = useState("");
  const [category, setCategory] = useState<ApiEntryCategory | "">("");
  const [amount, setAmount] = useState("");
  const [date, setDate] = useState(defaultDate);
  const [launchType, setLaunchType] = useState<LaunchType>("single");
  const [recurrenceFrequency, setRecurrenceFrequency] = useState<RecurrenceFrequency>("none");
  const [recurrenceCount, setRecurrenceCount] = useState("");
  const [paymentMethod, setPaymentMethod] = useState<ApiPaymentMethod | "">("");
  const [notes, setNotes] = useState("");
  const [hasSubexpenses, setHasSubexpenses] = useState(false);
  const [expenseItems, setExpenseItems] = useState<DraftExpenseItem[]>([]);
  const [editingItemId, setEditingItemId] = useState<string | null>(null);
  const [discardAction, setDiscardAction] = useState<"disable" | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState("");

  const createEmptyItem = (): DraftExpenseItem => ({ id: crypto.randomUUID(), name: "", amount: "", recurrenceFrequency: "none", recurrenceCount: "" });
  const numericAmount = parseCurrencyInput(amount);
  const parsedItems = expenseItems.map((item) => ({
    name: item.name.trim(),
    value: parseCurrencyInput(item.amount),
    recurrenceFrequency: item.recurrenceFrequency,
    recurrenceCount: item.recurrenceFrequency === "none" ? 0 : Number(item.recurrenceCount),
  }));
  const calculatedTotal = parsedItems.reduce((total, item) => total + (Number.isFinite(item.value) ? item.value : 0), 0);
  const itemsAreValid = expenseItems.length > 0 && parsedItems.every((item) => item.name.length > 0 && Number.isFinite(item.value) && item.value > 0 && (item.recurrenceFrequency === "none" || (Number.isInteger(item.recurrenceCount) && item.recurrenceCount >= 2 && item.recurrenceCount <= 120)));
  const hasFilledItems = expenseItems.some((item) => item.name.trim().length > 0 || item.amount.trim().length > 0);
  const numericRecurrenceCount = Number(recurrenceCount);
  const recurrenceIsValid = recurrenceFrequency === "none" || (recurrenceCount !== "" && Number.isInteger(numericRecurrenceCount) && numericRecurrenceCount >= 2 && numericRecurrenceCount <= 120);
  const amountIsValid = hasSubexpenses
    ? itemsAreValid && calculatedTotal > 0
    : Number.isFinite(numericAmount) && numericAmount > 0;
  const canSave = name.trim().length > 0 && category.length > 0 && amountIsValid && date.length > 0 && recurrenceIsValid;

  function clearDetailedExpense() {
    setHasSubexpenses(false);
    setExpenseItems([]);
  }

  function selectLaunchType(nextType: LaunchType) {
    setLaunchType(nextType);
    if (nextType === "single") {
      setRecurrenceFrequency("none");
      setRecurrenceCount("");
      return;
    }
    if (recurrenceFrequency === "none") setRecurrenceFrequency("monthly");
  }

  function toggleDetailedExpense(checked: boolean) {
    if (checked) {
      setLaunchType("single");
      setRecurrenceFrequency("none");
      setRecurrenceCount("");
      setHasSubexpenses(true);
      setExpenseItems((current) => current.length > 0 ? current : [createEmptyItem()]);
      return;
    }
    if (hasFilledItems) {
      setDiscardAction("disable");
      return;
    }
    clearDetailedExpense();
  }

  function confirmDiscardItems() {
    clearDetailedExpense();
    setDiscardAction(null);
  }

  function updateExpenseItem(id: string, field: "name" | "amount" | "recurrenceFrequency" | "recurrenceCount", value: string) {
    setExpenseItems((current) => current.map((item) => item.id === id ? { ...item, [field]: value } : item));
  }

  function itemRecurrenceSummary(item: DraftExpenseItem) {
    if (item.recurrenceFrequency === "none") return "Repetir";
    const labels: Record<Exclude<RecurrenceFrequency, "none">, string> = { daily: "Diária", weekly: "Semanal", monthly: "Mensal" };
    return `${labels[item.recurrenceFrequency]}${item.recurrenceCount ? ` • ${item.recurrenceCount}x` : ""}`;
  }

  function removeExpenseItem(id: string) {
    setExpenseItems((current) => {
      const remaining = current.filter((item) => item.id !== id);
      return remaining.length > 0 ? remaining : [createEmptyItem()];
    });
  }

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!canSave || isSubmitting) return;
    setIsSubmitting(true);
    setError("");
    try {
      await onCreate({
        name: name.trim(),
        category: category as ApiEntryCategory,
        date,
        value: hasSubexpenses ? calculatedTotal : numericAmount,
        income: type === "income",
        recurrenceFrequency,
        recurrenceCount: recurrenceFrequency === "none" ? 0 : numericRecurrenceCount,
        hasSubexpenses: type === "expense" && hasSubexpenses,
        items: type === "expense" && hasSubexpenses ? parsedItems : [],
        paymentMethod: paymentMethod || null,
        notes: notes.trim(),
      });
    } catch (requestError) {
      setError(messageFromError(requestError));
      setIsSubmitting(false);
    }
  }

  return (
    <div className="create-transaction-screen">
      <section className="subexpense-sheet create-transaction-sheet" aria-labelledby="create-transaction-title">
        <header className="form-page-header">
          <button type="button" className="create-sheet-back" onClick={onClose} aria-label="Voltar"><Icon name="back" /></button>
          <h2 id="create-transaction-title">{type === "expense" ? "Nova despesa" : "Nova receita"}</h2>
          <span aria-hidden="true" />
        </header>

        <form onSubmit={submit}>
          <InputField fieldClassName="field full-field" label="Descrição" maxLength={40} value={name} onChange={(event) => setName(event.target.value)} placeholder={type === "expense" ? "Ex.: Supermercado" : "Ex.: Salário"} />
          <CategoryPickerInput value={category} options={type === "expense" ? expenseCategories : incomeCategories} onChange={setCategory} />

          <PaymentMethodPickerInput label={type === "expense" ? "Forma de pagamento (opcional)" : "Forma de recebimento (opcional)"} value={paymentMethod} options={type === "expense" ? expensePaymentMethods : incomePaymentMethods} onChange={setPaymentMethod} />

          {type === "expense" && (
            <button type="button" role="switch" aria-checked={hasSubexpenses} className={`detail-expense-selector${hasSubexpenses ? " active" : ""}`} onClick={() => toggleDetailedExpense(!hasSubexpenses)}>
              <span className="detail-expense-copy"><b>Detalhar despesa</b><span>Dividir o valor em itens</span></span>
              <span className="switch-indicator" aria-hidden="true"><i /></span>
            </button>
          )}

          {!hasSubexpenses && (
            <div className="field-grid">
              <InputField fieldClassName="field" controlClassName="currency-input" label="Valor" prefix="R$" value={amount} onChange={(event) => setAmount(maskCurrencyInput(event.target.value))} onBlur={() => setAmount(completeCurrencyInput(amount))} placeholder="0,00" inputMode="numeric" />
              <DatePickerInput label="Data" value={date} onChange={setDate} />
            </div>
          )}

          {!hasSubexpenses && (
            <section className="launch-type-section" aria-labelledby="launch-type-title">
              <div className="launch-type-title" id="launch-type-title"><Icon name="clock" /> Tipo de lançamento</div>
              <div className="launch-type-options">
                <button type="button" className={launchType === "single" ? "active" : ""} onClick={() => selectLaunchType("single")}><b>Única</b><small>Um lançamento que acontece uma vez</small></button>
                <button type="button" className={launchType === "recurring" ? "active" : ""} onClick={() => selectLaunchType("recurring")}><b>Recorrente</b><small>{type === "expense" ? "Contas que se repetem" : "Entradas que se repetem"}</small></button>
              </div>
              {launchType === "recurring" && (
                <fieldset className="recurrence-choice launch-frequency">
                  <legend>Frequência</legend>
                  <SegmentedControl ariaLabel="Frequência do lançamento" value={recurrenceFrequency} options={recurrenceFrequencyOptions} onChange={setRecurrenceFrequency} />
                </fieldset>
              )}
              {launchType !== "single" && (
                <InputField fieldClassName="field recurrence-count" label="Total de repetições (incluindo esta)" type="text" inputMode="numeric" pattern="[0-9]*" value={recurrenceCount} placeholder="0" onChange={(event) => setRecurrenceCount(maskRepetitionCount(event.target.value))} />
              )}
            </section>
          )}

          {hasSubexpenses && (
            <>
              <section className="expense-items-editor" aria-labelledby="expense-items-title">
                <h3 id="expense-items-title">Itens da despesa</h3>
                <div className="expense-items-list">
                  {expenseItems.map((item, index) => (
                    <div className="expense-item-card" key={item.id}>
                      <div className="expense-item-row">
                        <Input className="expense-item-name" value={item.name} onChange={(event) => updateExpenseItem(item.id, "name", event.target.value)} placeholder="Descrição do item" aria-label={`Descrição do item ${index + 1}`} maxLength={40} />
                        <span className="ui-input-control currency-input expense-item-amount"><b className="ui-input-affix">R$</b><Input value={item.amount} onChange={(event) => updateExpenseItem(item.id, "amount", maskCurrencyInput(event.target.value))} onBlur={() => updateExpenseItem(item.id, "amount", completeCurrencyInput(item.amount))} placeholder="0,00" inputMode="numeric" aria-label={`Valor do item ${index + 1}`} /></span>
                        <button type="button" className="expense-item-remove" onClick={() => removeExpenseItem(item.id)} aria-label={`Remover item ${index + 1}`}><Icon name="trash" /></button>
                      </div>
                      <button type="button" className="expense-item-recurrence" onClick={() => setEditingItemId((current) => current === item.id ? null : item.id)} aria-expanded={editingItemId === item.id}>{itemRecurrenceSummary(item)}</button>
                      {editingItemId === item.id && (
                        <div className="item-recurrence-editor">
                          <fieldset className="recurrence-choice"><legend>Repetição do item</legend><SegmentedControl ariaLabel={`Repetição do item ${index + 1}`} value={item.recurrenceFrequency} options={itemRecurrenceOptions} onChange={(frequency) => updateExpenseItem(item.id, "recurrenceFrequency", frequency)} /></fieldset>
                          {item.recurrenceFrequency !== "none" && <InputField fieldClassName="field recurrence-count" label="Total de repetições (incluindo esta)" type="text" inputMode="numeric" pattern="[0-9]*" value={item.recurrenceCount} placeholder="0" onChange={(event) => updateExpenseItem(item.id, "recurrenceCount", maskRepetitionCount(event.target.value))} />}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
                <button type="button" className="add-expense-item" onClick={() => setExpenseItems((current) => [...current, createEmptyItem()])}><Icon name="plus" /> Adicionar item</button>
                <div className="expense-items-total"><span>Total da despesa</span><strong>{formatCurrency(calculatedTotal)}</strong></div>
              </section>
              <div className="field-grid field-grid--single"><DatePickerInput label="Data" value={date} onChange={setDate} /></div>
            </>
          )}

          <label className={`field full-field transaction-notes${hasSubexpenses ? " transaction-notes--detailed" : ""}`}><span>Observações (opcional)</span><textarea maxLength={120} value={notes} onChange={(event) => setNotes(event.target.value)} placeholder="Adicione um comentário..." /><small className="field-character-count">{notes.length}/120</small></label>

          {error && <p className="auth-error" role="alert">{error}</p>}
          <Button type="submit" className={`create-transaction-submit${type === "income" ? " create-transaction-submit--income" : ""}`} fullWidth loading={isSubmitting} loadingLabel="Adicionando..." disabled={!canSave}><Icon name="plus" /> Adicionar {type === "expense" ? "despesa" : "receita"}</Button>
        </form>

        {discardAction && (
          <div className="sheet-backdrop confirm-backdrop" role="presentation" onMouseDown={() => setDiscardAction(null)}>
            <section className="confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="discard-items-title" onMouseDown={(event) => event.stopPropagation()}>
              <div className="confirm-icon"><Icon name="trash" /></div>
              <h2 id="discard-items-title">Descartar itens?</h2>
              <p>Os itens preenchidos serão removidos e não poderão ser recuperados.</p>
              <div><button type="button" onClick={() => setDiscardAction(null)}>Cancelar</button><button type="button" onClick={confirmDiscardItems}>Descartar</button></div>
            </section>
          </div>
        )}
      </section>
    </div>
  );
}

function TransactionRow({
  transaction,
  onOpen,
  onDelete,
  onPaid,
}: {
  transaction: Transaction;
  onOpen: (transaction: Transaction) => void;
  onDelete: (transaction: Transaction) => void;
  onPaid: (transaction: Transaction) => Promise<void>;
}) {
  const [isOpen, setIsOpen] = useState(false);
  const [isUpdating, setIsUpdating] = useState(false);
  const pointerStart = useRef<{ x: number; y: number } | null>(null);
  const didSwipe = useRef(false);
  const hasActions = !transaction.income;

  function startSwipe(event: React.PointerEvent<HTMLButtonElement>) {
    if (!hasActions) return;
    pointerStart.current = { x: event.clientX, y: event.clientY };
  }

  function finishSwipe(event: React.PointerEvent<HTMLButtonElement>) {
    const start = pointerStart.current;
    pointerStart.current = null;
    if (!start || !hasActions) return;
    const horizontal = event.clientX - start.x;
    const vertical = event.clientY - start.y;
    if (Math.abs(horizontal) <= Math.abs(vertical) || Math.abs(horizontal) < 28) return;
    didSwipe.current = true;
    setIsOpen(horizontal < 0);
  }

  async function togglePaid() {
    if (isUpdating) return;
    setIsUpdating(true);
    try {
      await onPaid(transaction);
      setIsOpen(false);
    } finally {
      setIsUpdating(false);
    }
  }

  return (
    <div className={`transaction-swipe${isOpen ? " is-open" : ""}`}>
      {hasActions && (
        <div className="transaction-actions" aria-hidden={!isOpen}>
          <button type="button" className={`transaction-paid${transaction.paid ? " is-paid" : ""}`} disabled={isUpdating} tabIndex={isOpen ? 0 : -1} onClick={togglePaid}>
            <Icon name="check" />
            <span>{transaction.paid ? "Pendente" : "Pago"}</span>
          </button>
          <button type="button" className="transaction-delete" tabIndex={isOpen ? 0 : -1} onClick={() => onDelete(transaction)}>
            <Icon name="trash" />
            <span>Excluir</span>
          </button>
        </div>
      )}
      <button
        type="button"
        className={`transaction${transaction.paid ? " transaction--paid" : ""}`}
        onPointerDown={startSwipe}
        onPointerUp={finishSwipe}
        onPointerCancel={() => { pointerStart.current = null; }}
        onClick={() => { if (didSwipe.current) { didSwipe.current = false; return; } if (isOpen) setIsOpen(false); else onOpen(transaction); }}
        aria-label={`Abrir detalhes de ${transaction.name}`}
      >
        <div className={`transaction-icon${transaction.income ? " transaction-icon--income" : ""}`}>
          <Icon name={transaction.kind} />
        </div>
        <div className="transaction-main">
          <strong>{transaction.name}</strong>
          <div className="transaction-meta">
            <time dateTime={transaction.date}>{formatDate(transaction.date)}</time>
            {!transaction.income && <span className={`transaction-status${transaction.paid ? " paid" : " pending"}`}>{transaction.paid ? "Pago" : "Pendente"}</span>}
          </div>
        </div>
        <div className="transaction-values">
          <strong className={transaction.income ? "value-income" : "value-expense"}>{transaction.income ? "+ " : "- "}{formatCurrency(transaction.value)}</strong>
          <small>Saldo: {transaction.balance >= 0 ? "+ " : "- "}{formatCurrency(transaction.balance)}</small>
        </div>
      </button>
    </div>
  );
}
function Dashboard({
  items,
  onOpen,
  onCreate,
  onDelete,
  onPaid,
  onHome,
  onRadar,
  onGoals,
  onWork,
  showWorkFeatures = true,
  onMonthChange,
  initialPeriod,
  initialCreateType = null,
  onInitialCreateHandled,
  onReady,
  error = "",
  isLoading = false,
}: {
  items: Transaction[];
  onOpen: (transaction: Transaction) => void;
  onCreate: (transaction: NewTransaction) => Promise<void>;
  onDelete: (transaction: Transaction, scope: ApiSeriesScope) => Promise<void>;
  onPaid: (transaction: Transaction) => Promise<void>;
  onHome: () => void;
  onRadar: () => void;
  onGoals: () => void;
  onWork: () => void;
  showWorkFeatures?: boolean;
  onMonthChange: (year: number, month: number) => void;
  initialPeriod: { year: number; month: number };
  initialCreateType?: "expense" | "income" | null;
  onInitialCreateHandled?: () => void;
  onReady?: () => void;
  error?: string;
  isLoading?: boolean;
}) {
  const [selectedMonth, setSelectedMonth] = useState(() => {
    return new Date(initialPeriod.year, initialPeriod.month - 1, 1);
  });
  const [isMonthPickerOpen, setIsMonthPickerOpen] = useState(false);
  const [createType, setCreateType] = useState<"expense" | "income" | null>(initialCreateType);
  const [pendingDelete, setPendingDelete] = useState<Transaction | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const [actionError, setActionError] = useState("");
  const [transactionFilter, setTransactionFilter] = useState<TransactionFilter>("pending");

  useEffect(() => {
    if (initialCreateType) onInitialCreateHandled?.();
  }, [initialCreateType, onInitialCreateHandled]);
  useEffect(() => { if (!isLoading) onReady?.(); }, [isLoading, onReady]);
  const periodFormatter = new Intl.DateTimeFormat("pt-BR", {
    month: "long",
    year: "numeric",
  });
  const monthLabel = formatMonthYear(selectedMonth);
  const previousMonth = new Date(selectedMonth.getFullYear(), selectedMonth.getMonth() - 1, 1);
  const nextMonth = new Date(selectedMonth.getFullYear(), selectedMonth.getMonth() + 1, 1);
  const today = new Date();
  const visibleItems = items.filter((item) => {
    const [year, month] = item.date.split("-").map(Number);
    return year === selectedMonth.getFullYear() && month === selectedMonth.getMonth() + 1;
  });
  const filteredItems = visibleItems.filter((item) => {
    if (transactionFilter === "all") return true;
    if (item.income) return transactionFilter === "pending";
    return transactionFilter === "paid" ? item.paid : !item.paid;
  });
  const groupedItems = [...filteredItems]
    .sort((left, right) => {
      const dateOrder = left.date.localeCompare(right.date);
      if (dateOrder !== 0) return dateOrder;
      const priority = (item: Transaction) => item.income ? 1 : item.paid ? 2 : 0;
      return priority(left) - priority(right);
    })
    .reduce<TransactionGroup[]>((groups, item) => {
      const current = groups.at(-1);
      if (current?.date === item.date) {
        current.items.push(item);
        current.total += item.income ? item.value : -item.value;
      } else {
        groups.push({ date: item.date, items: [item], total: item.income ? item.value : -item.value });
      }
      return groups;
    }, []);
  const totalIncome = visibleItems.filter((item) => item.income).reduce((total, item) => total + item.value, 0);
  const totalExpenses = visibleItems.filter((item) => !item.income && !item.paid).reduce((total, item) => total + item.value, 0);
  const monthBalance = totalIncome - totalExpenses;
  const defaultDay = selectedMonth.getFullYear() === today.getFullYear() && selectedMonth.getMonth() === today.getMonth() ? today.getDate() : 1;
  const defaultDate = `${selectedMonth.getFullYear()}-${String(selectedMonth.getMonth() + 1).padStart(2, "0")}-${String(defaultDay).padStart(2, "0")}`;

  useEffect(() => {
    onMonthChange(selectedMonth.getFullYear(), selectedMonth.getMonth() + 1);
  }, [onMonthChange, selectedMonth]);

  function changeMonth(offset: number) {
    setSelectedMonth((current) => new Date(current.getFullYear(), current.getMonth() + offset, 1));
  }

  function openMonthPicker() {
    setIsMonthPickerOpen(true);
  }

  function selectMonth(month: number, year: number) {
    setSelectedMonth(new Date(year, month, 1));
    setIsMonthPickerOpen(false);
  }

  async function markPaid(transaction: Transaction) {
    setActionError("");
    try {
      await onPaid(transaction);
    } catch (requestError) {
      setActionError(messageFromError(requestError));
    }
  }

  async function confirmDeleteTransaction(scope: ApiSeriesScope = "CURRENT") {
    if (!pendingDelete || isDeleting) return;
    setIsDeleting(true);
    setActionError("");
    try {
      await onDelete(pendingDelete, scope);
      setPendingDelete(null);
    } catch (requestError) {
      setActionError(messageFromError(requestError));
    } finally {
      setIsDeleting(false);
    }
  }

  return (
    <main className="dashboard-screen">
      <AppHeader>
        <div className="header-create-actions">
          <button type="button" className="header-add-button header-add-button--income" disabled={isLoading} onClick={() => setCreateType("income")} aria-label="Nova receita" title="Nova receita"><Icon name="income" /></button>
          <button type="button" className="header-add-button header-add-button--expense" disabled={isLoading} onClick={() => setCreateType("expense")} aria-label="Nova despesa" title="Nova despesa"><Icon name="expense" /></button>
        </div>
      </AppHeader>

      <section className="month-selector" aria-label="Período selecionado">
        <button type="button" className="month-arrow" onClick={() => changeMonth(-1)} aria-label={`Ir para ${periodFormatter.format(previousMonth)}`}><Icon name="back" /></button>
        <button type="button" className="month-current" onClick={openMonthPicker} aria-haspopup="dialog" aria-expanded={isMonthPickerOpen}>
          {monthLabel}
          <Icon name="calendar" />
        </button>
        <button type="button" className="month-arrow" onClick={() => changeMonth(1)} aria-label={`Ir para ${periodFormatter.format(nextMonth)}`}><Icon name="forward" /></button>
      </section>

      <SegmentedControl className="transaction-filter" ariaLabel="Filtrar lançamentos por pagamento" value={transactionFilter} options={transactionFilterOptions} onChange={setTransactionFilter} />

      {(error || actionError) && <p className="auth-error" role="alert">{error || actionError}</p>}

      <section className={`transactions${isLoading || filteredItems.length === 0 ? " transactions--empty" : ""}`} aria-label={`Movimentações de ${monthLabel}`} aria-busy={isLoading}>
        {isLoading ? null : filteredItems.length === 0 ? (
          <div className="empty-transactions">
            <div className="empty-transactions-icon" aria-hidden="true"><Icon name="wallet" /></div>
            <h2>{transactionFilter === "pending" ? "Nenhuma despesa pendente" : transactionFilter === "paid" ? "Nenhuma despesa paga" : "Nenhum lançamento neste mês"}</h2>
            <p>{transactionFilter === "pending" ? "As despesas que ainda precisam ser pagas aparecerão aqui." : transactionFilter === "paid" ? "As despesas marcadas como pagas aparecerão aqui." : "Suas receitas e despesas aparecerão aqui quando forem adicionadas."}</p>
          </div>
        ) : groupedItems.map((group) => {
          const groupDate = formatTransactionGroupDate(group.date);
          return (
            <section className="transaction-date-group" key={group.date} aria-label={`Lançamentos de ${groupDate.calendarDate}`}>
              <header className="transaction-date-header">
                <div><strong>{groupDate.calendarDate}</strong><span>·</span><small>{groupDate.weekday}</small></div>
                <b className={group.total >= 0 ? "positive" : "negative"}>{group.total >= 0 ? "+ " : "- "}{formatCurrency(group.total)}</b>
              </header>
              <div className="transaction-group-list">
                {group.items.map((transaction) => (
                  <TransactionRow
                    key={transaction.id}
                    transaction={transaction}
                    onOpen={onOpen}
                    onDelete={setPendingDelete}
                    onPaid={markPaid}
                  />
                ))}
              </div>
            </section>
          );
        })}      </section>

      {!isLoading && <section className="monthly-summary" aria-label="Resumo do mês">
        <div><span>Receitas</span><strong>{formatCurrency(totalIncome)}</strong></div>
        <div><span>Despesas</span><strong>{formatCurrency(totalExpenses)}</strong></div>
        <div><span>Saldo do mês</span><strong>{monthBalance < 0 ? "- " : ""}{formatCurrency(monthBalance)}</strong></div>
      </section>}

      <BottomNavigation active="finance" loading={isLoading} onHome={onHome} onFinance={() => {}} onRadar={onRadar} onGoals={onGoals} onWork={onWork} showWorkFeatures={showWorkFeatures} />

      <MonthPickerDialog
        open={isMonthPickerOpen}
        selectedYear={selectedMonth.getFullYear()}
        selectedMonth={selectedMonth.getMonth()}
        onClose={() => setIsMonthPickerOpen(false)}
        onSelect={(year, month) => selectMonth(month, year)}
      />

      {createType && (
        <CreateTransactionSheet
          defaultDate={defaultDate}
          initialType={createType}
          onClose={() => setCreateType(null)}
          onCreate={async (transaction) => { await onCreate(transaction); setCreateType(null); }}
        />
      )}

      {pendingDelete?.seriesId ? (
        <SeriesScopeDialog
          title="Quais lançamentos deseja excluir?"
          description={`${pendingDelete.subexpenses.length > 0 ? "Os itens dessas despesas também serão excluídos. " : ""}Escolha se a exclusão vale só para este lançamento, para este e os futuros ou para toda a série, incluindo os anteriores.`}
          danger
          busy={isDeleting}
          onCancel={() => setPendingDelete(null)}
          onSelect={confirmDeleteTransaction}
        />
      ) : pendingDelete && (
        <div className="sheet-backdrop confirm-backdrop" role="presentation" onMouseDown={() => !isDeleting && setPendingDelete(null)}>
          <section className="confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="swipe-delete-title" onMouseDown={(event) => event.stopPropagation()}>
            <div className="confirm-icon"><Icon name="trash" /></div>
            <h2 id="swipe-delete-title">Excluir despesa?</h2>
            <p>{pendingDelete.subexpenses.length > 0
              ? `Esta despesa possui ${pendingDelete.subexpenses.length} ${pendingDelete.subexpenses.length === 1 ? "item" : "itens"}. Todos serão excluídos junto com ela.`
              : "Esta despesa será excluída permanentemente."}</p>
            <div><button type="button" disabled={isDeleting} onClick={() => setPendingDelete(null)}>Cancelar</button><button type="button" disabled={isDeleting} onClick={() => confirmDeleteTransaction("CURRENT")}>{isDeleting ? "Excluindo..." : "Excluir"}</button></div>
          </section>
        </div>
      )}
    </main>
  );
}

function SubexpenseEditor({
  item,
  onClose,
  onSave,
  onDelete,
}: {
  item: Subexpense;
  onClose: () => void;
  onSave: (item: Subexpense, scope: ApiSeriesScope) => Promise<void>;
  onDelete: (id: string, scope: ApiSeriesScope) => Promise<void>;
}) {
  const [draft, setDraft] = useState(item);
  const recurrenceCountMemory = useRef(item.recurrenceCount >= 2 ? item.recurrenceCount : 0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [pendingAction, setPendingAction] = useState<"save" | "delete" | null>(null);
  const amountValue = parseCurrencyInput(draft.amount);
  const itemRecurrenceIsValid = draft.recurrenceFrequency === "none" || (Number.isInteger(draft.recurrenceCount) && draft.recurrenceCount >= 2 && draft.recurrenceCount <= 120);
  const canSave = draft.name.trim().length > 0 && Number.isFinite(amountValue) && amountValue > 0 && itemRecurrenceIsValid;

  async function executeSave(scope: ApiSeriesScope) {
    if (!canSave || isSubmitting) return;
    setPendingAction(null);
    setIsSubmitting(true);
    setError("");
    try {
      await onSave({ ...draft, name: draft.name.trim(), amount: completeCurrencyInput(draft.amount) }, scope);
    } catch (requestError) {
      setError(messageFromError(requestError));
      setIsSubmitting(false);
    }
  }

  function save() {
    if (!canSave || isSubmitting) return;
    if (item.id && item.seriesId) {
      setPendingAction("save");
      return;
    }
    void executeSave("CURRENT");
  }

  async function executeRemove(scope: ApiSeriesScope) {
    if (!item.id || isSubmitting) {
      onClose();
      return;
    }
    setPendingAction(null);
    setIsSubmitting(true);
    setError("");
    try {
      await onDelete(item.id, scope);
    } catch (requestError) {
      setError(messageFromError(requestError));
      setIsSubmitting(false);
    }
  }

  function remove() {
    if (!item.id) {
      onClose();
      return;
    }
    if (item.seriesId) {
      setPendingAction("delete");
      return;
    }
    void executeRemove("CURRENT");
  }

  return (
    <div className="sheet-backdrop" role="presentation" onMouseDown={onClose}>
      <section
        className="subexpense-sheet"
        role="dialog"
        aria-modal="true"
        aria-labelledby="subexpense-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="sheet-handle" aria-hidden="true" />
        <div className="sheet-heading">
          <div>
            <span>Item da despesa</span>
            <h2 id="subexpense-title">Editar item</h2>
          </div>
          <button type="button" className="sheet-close" onClick={onClose} aria-label="Fechar">×</button>
        </div>

        <InputField fieldClassName="field full-field" label="Descrição" maxLength={40} value={draft.name} onChange={(event) => setDraft({ ...draft, name: event.target.value })} placeholder="Descrição do item" />
        <div className="field-grid field-grid--single">
          <InputField fieldClassName="field" controlClassName="currency-input" label="Valor" prefix="R$" value={draft.amount} onChange={(event) => setDraft({ ...draft, amount: maskCurrencyInput(event.target.value) })} onBlur={() => setDraft({ ...draft, amount: completeCurrencyInput(draft.amount) })} inputMode="numeric" />
        </div>
        <section className="launch-type-section item-editor-recurrence" aria-labelledby="item-launch-type-title">
          <div className="launch-type-title" id="item-launch-type-title"><Icon name="clock" /> Tipo de lançamento</div>
          <div className="launch-type-options">
            <button type="button" className={draft.recurrenceFrequency === "none" ? "active" : ""} onClick={() => setDraft((current) => ({ ...current, recurrenceFrequency: "none", recurrenceCount: 0 }))}><b>Única</b><small>Um lançamento que acontece uma vez</small></button>
            <button type="button" className={draft.recurrenceFrequency !== "none" ? "active" : ""} onClick={() => setDraft((current) => ({ ...current, recurrenceFrequency: current.recurrenceFrequency === "none" ? "monthly" : current.recurrenceFrequency, recurrenceCount: current.recurrenceCount >= 2 ? current.recurrenceCount : recurrenceCountMemory.current }))}><b>Recorrente</b><small>Contas que se repetem</small></button>
          </div>
          {draft.recurrenceFrequency !== "none" && (
            <fieldset className="recurrence-choice launch-frequency">
              <legend>Frequência</legend>
              <SegmentedControl ariaLabel="Frequência do item" value={draft.recurrenceFrequency} options={recurrenceFrequencyOptions} onChange={(frequency) => setDraft((current) => ({ ...current, recurrenceFrequency: frequency, recurrenceCount: current.recurrenceCount >= 2 ? current.recurrenceCount : recurrenceCountMemory.current }))} />
            </fieldset>
          )}
          {draft.recurrenceFrequency !== "none" && (
            <InputField fieldClassName="field recurrence-count" label="Total de repetições (incluindo esta)" type="text" inputMode="numeric" pattern="[0-9]*" value={draft.recurrenceCount || ""} placeholder="0" onChange={(event) => { const nextCount = Number(maskRepetitionCount(event.target.value)); if (nextCount >= 2) recurrenceCountMemory.current = nextCount; setDraft((current) => ({ ...current, recurrenceCount: nextCount })); }} />
          )}
        </section>
        <label className="paid-toggle">
          <input type="checkbox" checked={draft.paid} onChange={(event) => setDraft({ ...draft, paid: event.target.checked })} />
          <span>Marcar como paga</span>
        </label>

        {error && <p className="auth-error" role="alert">{error}</p>}
        <div className="sheet-actions">
          <button type="button" className="sub-delete" disabled={isSubmitting} onClick={remove}><Icon name="trash" /> {item.id ? "Excluir" : "Cancelar"}</button>
          <Button type="button" fullWidth loading={isSubmitting} loadingLabel="Salvando..." disabled={!canSave} onClick={save}>Salvar alteração</Button>
        </div>
      </section>
      {pendingAction && (
        <SeriesScopeDialog
          title={pendingAction === "save" ? "Quais itens deseja alterar?" : "Quais itens deseja excluir?"}
          description="Escolha se a ação vale só para este item, para este e os futuros ou para toda a série, incluindo os anteriores."
          currentLabel="Somente este item"
          danger={pendingAction === "delete"}
          busy={isSubmitting}
          onCancel={() => setPendingAction(null)}
          onSelect={(scope) => pendingAction === "save" ? void executeSave(scope) : void executeRemove(scope)}
        />
      )}
    </div>
  );
}

function DetailScreen({
  transaction,
  onBack,
  onDelete,
  onChanged,
}: {
  transaction: Transaction;
  onBack: () => void;
  onDelete: (id: string, scope: ApiSeriesScope) => Promise<void>;
  onChanged: () => Promise<void>;
}) {
  const [type, setType] = useState<"expense" | "income">(transaction.income ? "income" : "expense");
  const [name, setName] = useState(transaction.name);
  const [category, setCategory] = useState<ApiEntryCategory | "">(transaction.category);
  const [amount, setAmount] = useState(transaction.value.toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 }));
  const [dueDate, setDueDate] = useState(transaction.date);
  const [recurrenceFrequency, setRecurrenceFrequency] = useState<RecurrenceFrequency>(transaction.recurrenceFrequency);
  const [recurrenceCount, setRecurrenceCount] = useState(String(transaction.recurrenceCount || 1));
  const recurrenceCountMemory = useRef(String(transaction.recurrenceCount >= 2 ? transaction.recurrenceCount : ""));
  const [paymentMethod, setPaymentMethod] = useState<ApiPaymentMethod | "">(transaction.paymentMethod ?? "");
  const [notes, setNotes] = useState(transaction.notes);
  const [subexpenses, setSubexpenses] = useState<Subexpense[]>(transaction.subexpenses);
  const [editing, setEditing] = useState<Subexpense | null>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [confirmSaveScope, setConfirmSaveScope] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState("");
  const numericAmount = parseCurrencyInput(amount);
  const numericRecurrenceCount = Number(recurrenceCount);
  const recurrenceIsValid = transaction.hasSubexpenses || recurrenceFrequency === "none" || (recurrenceCount !== "" && Number.isInteger(numericRecurrenceCount) && numericRecurrenceCount >= 2 && numericRecurrenceCount <= 120);
  const parentFormIsValid = name.trim().length > 0
    && category !== ""
    && dueDate !== ""
    && Number.isFinite(numericAmount)
    && numericAmount > 0
    && recurrenceIsValid;
  const recurrenceCountChanged = recurrenceFrequency !== "none"
    && numericRecurrenceCount !== transaction.recurrenceCount;
  const hasParentChanges = type !== (transaction.income ? "income" : "expense")
    || name.trim() !== transaction.name
    || category !== transaction.category
    || dueDate !== transaction.date
    || paymentMethod !== (transaction.paymentMethod ?? "")
    || notes.trim() !== transaction.notes
    || (!transaction.hasSubexpenses && (
      Math.round(numericAmount * 100) !== Math.round(transaction.value * 100)
      || recurrenceFrequency !== transaction.recurrenceFrequency
      || recurrenceCountChanged
    ));
  const progress = transaction.totalOccurrences
    ? Math.round((transaction.paidOccurrences / transaction.totalOccurrences) * 100)
    : 0;

  useEffect(() => {
    const synchronize = window.setTimeout(() => {
      setAmount(transaction.value.toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 }));
      setSubexpenses(transaction.subexpenses);
    }, 0);
    return () => window.clearTimeout(synchronize);
  }, [transaction.subexpenses, transaction.value]);

  async function executeSaveDetails(scope: ApiSeriesScope) {
    if (!hasParentChanges || !parentFormIsValid || isSaving) return;
    setIsSaving(true);
    setError("");
    try {
      await financeApi.update(transaction.id, {
        name: name.trim(),
        category: category as ApiEntryCategory,
        type: type === "income" ? "INCOME" : "EXPENSE",
        amount: numericAmount,
        dueDate,
        recurrenceFrequency: transaction.hasSubexpenses ? "NONE" : recurrenceToApi[recurrenceFrequency],
        recurrenceCount: transaction.hasSubexpenses || recurrenceFrequency === "none" ? 0 : numericRecurrenceCount,
        hasSubexpenses: type === "expense" && transaction.hasSubexpenses,
        paymentMethod: paymentMethod || null,
        notes: notes.trim() || null,
      }, scope);
      await onChanged();
      onBack();
    } catch (requestError) {
      setError(messageFromError(requestError));
    } finally {
      setIsSaving(false);
    }
  }

  function saveDetails() {
    if (!hasParentChanges || !parentFormIsValid || isSaving) return;
    if (transaction.seriesId) {
      setConfirmSaveScope(true);
      return;
    }
    void executeSaveDetails("CURRENT");
  }

  function addSubexpense() {
    if (!transaction.hasSubexpenses || type === "income") return;
    const newItem: Subexpense = {
      id: "",
      name: "",
      amount: "0,00",
      installment: "",
      paid: false,
      recurrenceFrequency: "none",
      recurrenceCount: 0,
      recurrenceIndex: 0,
    };
    setEditing(newItem);
  }

  async function saveSubexpense(item: Subexpense, scope: ApiSeriesScope) {
    const payload = {
      name: item.name,
      amount: parseCurrencyInput(item.amount),
      installmentDescription: item.installment.trim() || null,
      paid: item.paid,
      recurrenceFrequency: recurrenceToApi[item.recurrenceFrequency],
      recurrenceCount: item.recurrenceFrequency === "none" ? 0 : item.recurrenceCount,
    };
    if (item.id) {
      const updated = mapSubexpense(await financeApi.updateSubexpense(transaction.id, item.id, payload, scope));
      setSubexpenses((current) => current.map((currentItem) => currentItem.id === updated.id ? updated : currentItem));
    } else {
      const created = mapSubexpense(await financeApi.addSubexpense(transaction.id, payload));
      setSubexpenses((current) => [...current, created]);
    }
    setEditing(null);
    await onChanged();
  }

  async function deleteSubexpense(subexpenseId: string, scope: ApiSeriesScope) {
    await financeApi.deleteSubexpense(transaction.id, subexpenseId, scope);
    setSubexpenses((current) => current.filter((item) => item.id !== subexpenseId));
    setEditing(null);
    await onChanged();
  }

  async function deleteEntry(scope: ApiSeriesScope = "CURRENT") {
    if (isSaving) return;
    setIsSaving(true);
    setError("");
    try {
      await onDelete(transaction.id, scope);
    } catch (requestError) {
      setConfirmDelete(false);
      setError(messageFromError(requestError));
      setIsSaving(false);
    }
  }

  return (
    <main className="detail-screen edit-transaction-screen">
      <header className="form-page-header edit-transaction-header">
        <button type="button" className="detail-back-button" onClick={onBack} aria-label="Voltar"><Icon name="back" /></button>
        <h1>Editar lançamento</h1>
        <button
          type="button"
          className="detail-save-button"
          disabled={!hasParentChanges || !parentFormIsValid || isSaving}
          aria-label={isSaving ? "Salvando lançamento" : "Salvar alterações"}
          aria-busy={isSaving || undefined}
          title="Salvar alterações"
          onClick={saveDetails}
        >
          {isSaving ? <span className="detail-save-spinner" aria-hidden="true" /> : <Icon name="save" />}
        </button>
      </header>
      <div className="detail-content">
        <section className="detail-card edit-card">
          <div className="type-switch" aria-label="Tipo do lançamento">
            <button type="button" className={type === "expense" ? "active expense" : ""} onClick={() => { setType("expense"); if (type !== "expense") setCategory(""); }}>Despesa</button>
            <button type="button" className={type === "income" ? "active income" : ""} onClick={() => { setType("income"); if (type !== "income") setCategory(""); if (paymentMethod === "DEBIT_CARD" || paymentMethod === "CREDIT_CARD") setPaymentMethod(""); }}>Receita</button>
          </div>

          <InputField fieldClassName="field full-field" label="Descrição" maxLength={40} value={name} onChange={(event) => setName(event.target.value)} />
          <CategoryPickerInput value={category} options={type === "expense" ? expenseCategories : incomeCategories} onChange={setCategory} />
          <PaymentMethodPickerInput label={type === "expense" ? "Forma de pagamento (opcional)" : "Forma de recebimento (opcional)"} value={paymentMethod} options={type === "expense" ? expensePaymentMethods : incomePaymentMethods} onChange={setPaymentMethod} />
          <div className="field-grid">
            <InputField fieldClassName="field" controlClassName="currency-input" label="Valor" prefix="R$" value={amount} onChange={(event) => setAmount(maskCurrencyInput(event.target.value))} onBlur={() => setAmount(completeCurrencyInput(amount))} inputMode="numeric" readOnly={transaction.hasSubexpenses} aria-readonly={transaction.hasSubexpenses} />
            <DatePickerInput label="Vencimento" value={dueDate} onChange={setDueDate} />
          </div>
          {!transaction.hasSubexpenses && (
            <section className="launch-type-section" aria-labelledby="edit-launch-type-title">
              <div className="launch-type-title" id="edit-launch-type-title"><Icon name="clock" /> Tipo de lançamento</div>
              <div className="launch-type-options">
                <button type="button" className={recurrenceFrequency === "none" ? "active" : ""} onClick={() => { setRecurrenceFrequency("none"); setRecurrenceCount(""); }}><b>Única</b><small>Um lançamento que acontece uma vez</small></button>
                <button type="button" className={recurrenceFrequency !== "none" ? "active" : ""} onClick={() => { if (recurrenceFrequency === "none") setRecurrenceFrequency("monthly"); if (Number(recurrenceCount) < 2) setRecurrenceCount(recurrenceCountMemory.current); }}><b>Recorrente</b><small>{type === "expense" ? "Contas que se repetem" : "Entradas que se repetem"}</small></button>
              </div>
              {recurrenceFrequency !== "none" && (
                <fieldset className="recurrence-choice launch-frequency">
                  <legend>Frequência</legend>
                  <SegmentedControl ariaLabel="Frequência do lançamento" value={recurrenceFrequency} options={recurrenceFrequencyOptions} onChange={(frequency) => { setRecurrenceFrequency(frequency); if (Number(recurrenceCount) < 2) setRecurrenceCount(recurrenceCountMemory.current); }} />
                </fieldset>
              )}
              {recurrenceFrequency !== "none" && <InputField fieldClassName="field recurrence-count" label="Total de repetições (incluindo esta)" type="text" inputMode="numeric" pattern="[0-9]*" value={recurrenceCount} placeholder="0" onChange={(event) => { const nextCount = maskRepetitionCount(event.target.value); if (Number(nextCount) >= 2) recurrenceCountMemory.current = nextCount; setRecurrenceCount(nextCount); }} />}
            </section>
          )}
          <label className={`field full-field transaction-notes${transaction.hasSubexpenses ? " transaction-notes--detailed" : ""}`}><span>Observações (opcional)</span><textarea maxLength={120} value={notes} onChange={(event) => setNotes(event.target.value)} placeholder="Adicione um comentário..." /><small className="field-character-count">{notes.length}/120</small></label>
        </section>

        {transaction.recurrenceFrequency !== "none" && (
          <section className="detail-card progress-card">
            <div className="progress-heading"><div><span>PROGRESSO</span><strong>{transaction.paidOccurrences} de {transaction.totalOccurrences} parcelas pagas</strong></div><b>{progress}%</b></div>
            <div className="progress-track"><span style={{ width: `${progress}%` }} /></div>
            <p>O progresso considera as parcelas desta série marcadas como pagas.</p>
          </section>
        )}

        {transaction.hasSubexpenses && type === "expense" && (
          <section className="detail-card subexpenses-card">
            <div className="section-title subexpense-title">
              <div><span>COMPOSIÇÃO</span><h2>Itens da despesa <b>{subexpenses.length}</b></h2></div>
              <button type="button" onClick={addSubexpense}><Icon name="plus" /> Adicionar</button>
            </div>
            <div className="subexpense-list">
              {subexpenses.map((item) => (
                <button type="button" className="subexpense-row" key={item.id} onClick={() => setEditing(item)}>
                  <span className="sub-copy"><strong>{item.name}</strong><small>{item.installment.toLocaleLowerCase("pt-BR").startsWith("parcela") ? item.installment : `Parcela ${item.installment}`}</small></span>
                  <span className="sub-value">R$ {item.amount}</span>
                  <span className="sub-edit"><Icon name="edit" /></span>
                </button>
              ))}
            </div>
          </section>
        )}

        {error && <p className="auth-error" role="alert">{error}</p>}
        <Button type="button" className="save-details" fullWidth loading={isSaving} loadingLabel="Salvando..." disabled={!hasParentChanges || !parentFormIsValid} onClick={saveDetails}>{hasParentChanges ? "Salvar alterações" : "Tudo salvo"}</Button>
        <button type="button" className="danger-button" onClick={() => setConfirmDelete(true)}><Icon name="trash" /> Excluir lançamento</button>
      </div>

      {editing && (
        <SubexpenseEditor
          item={editing}
          onClose={() => setEditing(null)}
          onSave={saveSubexpense}
          onDelete={deleteSubexpense}
        />
      )}

      {confirmSaveScope && (
        <SeriesScopeDialog
          title="Quais lançamentos deseja alterar?"
          description="Escolha se a alteração vale só para este lançamento, para este e os futuros ou para toda a série, incluindo os anteriores."
          busy={isSaving}
          onCancel={() => setConfirmSaveScope(false)}
          onSelect={(scope) => { setConfirmSaveScope(false); void executeSaveDetails(scope); }}
        />
      )}

      {confirmDelete && transaction.seriesId ? (
        <SeriesScopeDialog
          title="Quais lançamentos deseja excluir?"
          description={`${subexpenses.length > 0 ? "Os itens dessas despesas também serão excluídos. " : ""}Escolha se a exclusão vale só para este lançamento, para este e os futuros ou para toda a série, incluindo os anteriores.`}
          danger
          busy={isSaving}
          onCancel={() => setConfirmDelete(false)}
          onSelect={(scope) => void deleteEntry(scope)}
        />
      ) : confirmDelete && (
        <div className="sheet-backdrop confirm-backdrop" role="presentation" onMouseDown={() => setConfirmDelete(false)}>
          <section className="confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="delete-title" onMouseDown={(event) => event.stopPropagation()}>
            <div className="confirm-icon"><Icon name="trash" /></div>
            <h2 id="delete-title">Excluir lançamento?</h2>
            <p>{subexpenses.length > 0 ? `Esta despesa possui ${subexpenses.length} ${subexpenses.length === 1 ? "item" : "itens"}. Todos serão excluídos junto com ela.` : "Esta despesa será excluída permanentemente."}</p>
            <div><button type="button" disabled={isSaving} onClick={() => setConfirmDelete(false)}>Cancelar</button><button type="button" disabled={isSaving} onClick={() => deleteEntry("CURRENT")}>{isSaving ? "Excluindo..." : "Excluir"}</button></div>
          </section>
        </div>
      )}
    </main>
  );
}

function RadarScreen({ onHome, onFinance, onGoals, onWork, onReady, showWorkFeatures = true }: { onHome: () => void; onFinance: (period?: { year: number; month: number }) => void; onGoals: () => void; onWork: () => void; onReady?: () => void; showWorkFeatures?: boolean }) {
  const now = new Date();
  const startYear = now.getFullYear();
  const startMonth = now.getMonth() + 1;
  const [forecast, setForecast] = useState<ApiForecast | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [expandedMonth, setExpandedMonth] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    financeApi.forecast(startYear, startMonth, 12)
      .then((response) => {
        if (active) setForecast(response);
      })
      .catch((requestError) => {
        if (active) setError(messageFromError(requestError));
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });
    return () => { active = false; };
  }, [startMonth, startYear]);
  useEffect(() => { if (!isLoading) onReady?.(); }, [isLoading, onReady]);

  const timeline = forecast?.timeline ?? [];
  const current = timeline[0];
  const totalPending = timeline.reduce((total, period) => total + Number(period.pendingExpenses), 0);
  const lowestBalance = timeline.reduce<(typeof timeline)[number] | undefined>((lowest, period) => (
    !lowest || Number(period.freeBalance) < Number(lowest.freeBalance) ? period : lowest
  ), undefined);
  const monthName = (year: number, month: number, style: "long" | "short" = "long") => style === "long"
    ? formatMonthYear(new Date(year, month - 1, 1))
    : new Intl.DateTimeFormat("pt-BR", { month: style, year: "numeric" }).format(new Date(year, month - 1, 1));
  const signedCurrency = (value: number) => `${value < 0 ? "- " : "+ "}${formatCurrency(value)}`;
  const balanceStatus = (value: number) => value < 0 ? "Atenção" : value === 0 ? "No limite" : "Saldo positivo";

  return (
    <main className="dashboard-screen radar-screen">
      <AppHeader className="radar-header">
        <span className="radar-header-label">PROJEÇÃO</span>
      </AppHeader>

      <div className="radar-content">
        {error && <p className="auth-error" role="alert">{error}</p>}
        {isLoading ? null : (
          <>
            <div className="radar-summary" aria-label="Resumo da previsão">
              <PrimaryKpiCard className="radar-balance-card">
                <KpiLabel>SALDO PREVISTO NESTE MÊS</KpiLabel>
                <KpiValue className={Number(current?.freeBalance ?? 0) < 0 ? "negative" : ""}>
                  {Number(current?.freeBalance ?? 0) < 0 ? "- " : ""}{formatCurrency(Number(current?.freeBalance ?? 0))}
                </KpiValue>
              </PrimaryKpiCard>
              <SecondaryKpiGroup className="radar-mini-cards">
                <SecondaryKpiItem><KpiLabel>Gastos nos 12 meses</KpiLabel><KpiValue>{formatCurrency(totalPending)}</KpiValue></SecondaryKpiItem>
                <SecondaryKpiItem><KpiLabel>Mês mais apertado</KpiLabel><KpiValue className={Number(lowestBalance?.freeBalance ?? 0) < 0 ? "negative" : ""}>{Number(lowestBalance?.freeBalance ?? 0) < 0 ? "- " : ""}{formatCurrency(Number(lowestBalance?.freeBalance ?? 0))}</KpiValue></SecondaryKpiItem>
              </SecondaryKpiGroup>
            </div>

            <section className="radar-timeline" aria-label="Previsão mensal">
              <div className="radar-timeline-title"><span>PREVISÃO MÊS A MÊS</span></div>
              {timeline.map((period) => {
                const freeBalance = Number(period.freeBalance);
                const expenseChange = Number(period.expenseChange);
                const income = Number(period.income);
                const pending = Number(period.pendingExpenses);
                const barWidth = income > 0 ? Math.min(100, Math.round((pending / income) * 100)) : pending > 0 ? 100 : 0;
                const monthKey = `${period.year}-${period.month}`;
                const isExpanded = expandedMonth === monthKey;
                const startingEvents = period.events.filter((event) => event.type === "STARTING");
                const endingEvents = period.events.filter((event) => event.type === "ENDING");
                return (
                  <article className={`forecast-month${isExpanded ? " expanded" : ""}${freeBalance < 0 ? " deficit" : ""}`} key={monthKey}>
                    <button className="forecast-month-toggle" type="button" aria-expanded={isExpanded} onClick={() => setExpandedMonth(isExpanded ? null : monthKey)}>
                      <span className="forecast-month-name"><strong>{monthName(period.year, period.month)}</strong><small>{balanceStatus(freeBalance)}</small></span>
                      <span className="forecast-free"><small>Saldo previsto</small><strong className={freeBalance < 0 ? "negative" : ""}>{freeBalance < 0 ? "- " : ""}{formatCurrency(freeBalance)}</strong></span>
                      <span className="forecast-chevron" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="m8 10 4 4 4-4" /></svg></span>
                    </button>
                    <div className="forecast-core-values">
                      <div><span>Receitas previstas</span><strong>{formatCurrency(income)}</strong></div>
                      <span className="forecast-operation">−</span>
                      <div><span>Gastos previstos</span><strong>{formatCurrency(pending)}</strong></div>
                    </div>
                    <div className="forecast-commitment"><div className="forecast-bar" aria-label={`${barWidth}% das receitas comprometidas`}><span style={{ width: `${barWidth}%` }} /></div><small>{income > 0 ? `${barWidth}% da receita comprometida` : pending > 0 ? "Sem receita prevista" : "Sem gastos previstos"}</small></div>
                    {isExpanded && <div className="forecast-details">
                      {Number(period.paidExpenses) > 0 && <div className="forecast-paid"><span>Já pago neste mês</span><strong>{formatCurrency(Number(period.paidExpenses))}</strong></div>}
                      <p className={`forecast-comparison${expenseChange > 0 ? " worse" : expenseChange < 0 ? " better" : ""}`}>
                        {expenseChange === 0 ? "Gastos estáveis em relação ao mês anterior." : expenseChange > 0
                          ? `${signedCurrency(expenseChange)} em gastos comparado ao mês anterior.`
                          : `${formatCurrency(expenseChange)} a menos em gastos que no mês anterior.`}
                      </p>
                      {period.events.length > 0 && <div className="forecast-events">
                        {startingEvents.length > 0 && <section className="forecast-event-group">
                          <header><strong>O que começa neste mês</strong><span>{startingEvents.length} {startingEvents.length === 1 ? "item" : "itens"}</span></header>
                          {startingEvents.map((event) => (
                            <div className="forecast-event starting" key={`${event.type}-${event.source}-${event.name}-${event.date}`}>
                              <span>Começa</span><div><strong>{event.name}</strong><small>Primeira parcela</small></div><b>{formatCurrency(Number(event.amount))}</b>
                            </div>
                          ))}
                        </section>}
                        {endingEvents.length > 0 && <section className="forecast-event-group ending">
                          <header><strong>O que termina neste mês</strong><span>{endingEvents.length} {endingEvents.length === 1 ? "item" : "itens"}</span></header>
                          {endingEvents.map((event) => (
                            <div className="forecast-event ending" key={`${event.type}-${event.source}-${event.name}-${event.date}`}>
                              <span>Termina</span><div><strong>{event.name}</strong><small>Última parcela</small></div><b>{formatCurrency(Number(event.amount))}</b>
                            </div>
                          ))}
                        </section>}
                        <button type="button" className="forecast-events-link" onClick={() => onFinance({ year: period.year, month: period.month })}>Ver todas as despesas do mês <Icon name="forward" /></button>
                      </div>}
                    </div>}
                  </article>
                );
              })}
            </section>
          </>
        )}
      </div>

      <BottomNavigation active="radar" loading={isLoading} onHome={onHome} onFinance={() => onFinance()} onRadar={() => {}} onGoals={onGoals} onWork={onWork} showWorkFeatures={showWorkFeatures} />
    </main>
  );
}

const goalCalendarWeekdays = ["SEG", "TER", "QUA", "QUI", "SEX", "SÁB", "DOM"];

function localDateFromInput(dateValue: string) {
  const [year, month, day] = dateValue.split("-").map(Number);
  return new Date(year, month - 1, day);
}

function goalCalendarCells(year: number, month: number, referenceDate: string) {
  const firstDate = new Date(year, month - 1, 1);
  const offsetFromMonday = (firstDate.getDay() + 6) % 7;
  const daysInMonth = new Date(year, month, 0).getDate();
  const totalCells = Math.ceil((offsetFromMonday + daysInMonth) / 7) * 7;
  const reference = localDateFromInput(referenceDate);

  return Array.from({ length: totalCells }, (_, index) => {
    const date = new Date(year, month - 1, index - offsetFromMonday + 1);
    const inMonth = date.getFullYear() === year && date.getMonth() === month - 1;
    return {
      key: dateToInputValue(date),
      day: date.getDate(),
      inMonth,
      passed: inMonth && date < reference,
    };
  });
}

function remainingGoalDays(year: number, month: number, referenceDate: string) {
  return goalCalendarCells(year, month, referenceDate)
    .filter((cell) => cell.inMonth && !cell.passed)
    .map((cell) => cell.day);
}

function GoalsScreen({ onHome, onFinance, onRadar, onWork, onReady }: { onHome: () => void; onFinance: () => void; onRadar: () => void; onWork: () => void; onReady?: () => void }) {
  const [referenceDate, setReferenceDate] = useState(() => dateToInputValue(new Date()));
  const [goal, setGoal] = useState<ApiGoal | null>(null);
  const [nextGoal, setNextGoal] = useState<ApiGoal | null>(null);
  const [isNextGoalExpanded, setIsNextGoalExpanded] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingNextGoal, setIsLoadingNextGoal] = useState(false);
  const [isSavingDays, setIsSavingDays] = useState(false);
  const [error, setError] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);
  useEffect(() => { if (!isLoading) onReady?.(); }, [isLoading, onReady]);

  useEffect(() => {
    const updateDate = () => {
      const currentDate = dateToInputValue(new Date());
      setReferenceDate((savedDate) => savedDate === currentDate ? savedDate : currentDate);
    };
    const refreshOnFocus = () => {
      if (document.visibilityState !== "visible") return;
      updateDate();
      setRefreshKey((current) => current + 1);
    };
    window.addEventListener("focus", refreshOnFocus);
    document.addEventListener("visibilitychange", refreshOnFocus);
    const timer = window.setInterval(updateDate, 60_000);
    return () => {
      window.removeEventListener("focus", refreshOnFocus);
      document.removeEventListener("visibilitychange", refreshOnFocus);
      window.clearInterval(timer);
    };
  }, []);

  useEffect(() => {
    let active = true;
    financeApi.goal(referenceDate)
      .then(async (response) => {
        if (!active) return;
        setGoal(response);
        setNextGoal(null);
        setIsNextGoalExpanded(false);
        setIsLoading(false);
        if (!response.covered || Number(response.targetAmount) === 0
          || (Number(response.income) === 0 && Number(response.totalExpenses) === 0)) return;

        const nextMonthDate = new Date(response.year, response.month, 1);
        setIsLoadingNextGoal(true);
        const followingGoal = await financeApi.goal(
          referenceDate,
          nextMonthDate.getFullYear(),
          nextMonthDate.getMonth() + 1,
        );
        if (active) setNextGoal(followingGoal);
      })
      .catch((requestError) => {
        if (active) setError(messageFromError(requestError));
      })
      .finally(() => {
        if (active) {
          setIsLoading(false);
          setIsLoadingNextGoal(false);
        }
      });
    return () => { active = false; };
  }, [referenceDate, refreshKey]);

  async function saveGoalDays(targetGoal: ApiGoal, selectedDays: number[]) {
    if (isSavingDays) return;
    setIsSavingDays(true);
    setError("");
    try {
      const updatedGoal = await financeApi.updateGoalPreferences(
        referenceDate,
        [...selectedDays].sort((first, second) => first - second),
        targetGoal.year,
        targetGoal.month,
      );
      if (goal?.year === targetGoal.year && goal.month === targetGoal.month) setGoal(updatedGoal);
      else setNextGoal(updatedGoal);
    } catch (requestError) {
      setError(messageFromError(requestError));
    } finally {
      setIsSavingDays(false);
    }
  }

  function toggleGoalDay(targetGoal: ApiGoal, day: number) {
    const selectableDays = remainingGoalDays(targetGoal.year, targetGoal.month, referenceDate);
    if (!selectableDays.includes(day)) return;
    const selectedDays = targetGoal.selectedDays.includes(day)
      ? targetGoal.selectedDays.filter((selectedDay) => selectedDay !== day)
      : [...targetGoal.selectedDays, day];
    void saveGoalDays(targetGoal, selectedDays);
  }

  function selectRemainingGoalDays(targetGoal: ApiGoal) {
    const remainingDays = remainingGoalDays(targetGoal.year, targetGoal.month, referenceDate);
    void saveGoalDays(targetGoal, [...new Set([...targetGoal.selectedDays, ...remainingDays])]);
  }

  function clearRemainingGoalDays(targetGoal: ApiGoal) {
    const remainingDays = new Set(remainingGoalDays(targetGoal.year, targetGoal.month, referenceDate));
    void saveGoalDays(targetGoal, targetGoal.selectedDays.filter((day) => !remainingDays.has(day)));
  }

  const goalMonthLabel = (targetGoal: ApiGoal) => formatMonthYear(new Date(targetGoal.year, targetGoal.month - 1, 1));
  const progress = Math.max(0, Math.min(100, Number(goal?.progressPercentage ?? 0)));
  function renderGoalDetails(targetGoal: ApiGoal) {
    const calendarCells = goalCalendarCells(targetGoal.year, targetGoal.month, referenceDate);
    const remainingDays = remainingGoalDays(targetGoal.year, targetGoal.month, referenceDate);
    const remainingDaysSet = new Set(remainingDays);
    return (
      <>
        <SecondaryKpiGroup className="goal-targets" aria-label={`Objetivos restantes de ${goalMonthLabel(targetGoal)}`}>
          <SecondaryKpiItem><KpiLabel>OBJETIVO SEMANAL</KpiLabel><KpiValue>{formatCurrency(Number(targetGoal.weeklyTarget))}</KpiValue></SecondaryKpiItem>
          <SecondaryKpiItem><KpiLabel>OBJETIVO DIÁRIO</KpiLabel><KpiValue>{formatCurrency(Number(targetGoal.dailyTarget))}</KpiValue></SecondaryKpiItem>
        </SecondaryKpiGroup>

        <section className="goal-days-card">
          <div className="goal-days-heading">
            <div><span>DIAS DISPONÍVEIS</span><h2>Selecione os dias em que pretende gerar renda</h2></div>
            <strong>{targetGoal.availableDays} {targetGoal.availableDays === 1 ? "dia selecionado" : "dias selecionados"}</strong>
          </div>
          <div className="goal-calendar-weekdays" aria-hidden="true">
            {goalCalendarWeekdays.map((weekday) => <span key={weekday}>{weekday}</span>)}
          </div>
          <div className="goal-calendar" aria-label={`Calendário de ${goalMonthLabel(targetGoal)}`}>
            {calendarCells.map((cell) => {
              const selected = cell.inMonth && !cell.passed && targetGoal.selectedDays.includes(cell.day);
              const selectable = cell.inMonth && remainingDaysSet.has(cell.day);
              return (
                <button
                  type="button"
                  key={cell.key}
                  className={`${!cell.inMonth ? "outside" : cell.passed ? "passed" : selected ? "selected" : "available"}`}
                  disabled={isSavingDays || !selectable}
                  onClick={() => toggleGoalDay(targetGoal, cell.day)}
                  aria-pressed={selected}
                  aria-label={!cell.inMonth
                    ? `${cell.day}, fora do mês`
                    : cell.passed
                      ? `Dia ${cell.day}, já passou`
                      : `Dia ${cell.day}${selected ? ", selecionado" : ", não selecionado"}`}
                >
                  <span>{String(cell.day).padStart(2, "0")}</span>
                  {selected && <i aria-hidden="true"><Icon name="check" /></i>}
                </button>
              );
            })}
          </div>
          <div className="goal-calendar-actions">
            <button type="button" disabled={isSavingDays || remainingDays.length === 0} onClick={() => selectRemainingGoalDays(targetGoal)}><Icon name="check" />Selecionar restantes</button>
            <button type="button" className="clear" disabled={isSavingDays || targetGoal.availableDays === 0} onClick={() => clearRemainingGoalDays(targetGoal)}><Icon name="trash" />Limpar restantes</button>
          </div>
          <div className="goal-calendar-legend" aria-hidden="true">
            <span><i className="passed" />Já passou</span>
            <span><i className="selected" />Selecionado</span>
            <span><i />Não selecionado</span>
          </div>
        </section>
      </>
    );
  }

  return (
    <main className="dashboard-screen goals-screen">
      <AppHeader className="goals-header">
        <span className="goals-header-label">OBJETIVOS</span>
      </AppHeader>

      <div className="goals-content">
        {error && <p className="auth-error" role="alert">{error}</p>}

        {isLoading ? null : goal && (
          <>
            <PrimaryKpiCard className={`goal-main-card${goal.covered ? " monthly-goal-completed" : ""}`}>
              {goal.covered ? (
                <>
                  <span>OBJETIVO MENSAL</span>
                  <strong>Meta alcançada!</strong>
                  <p>Seus ganhos já cobrem todas as despesas do mês.</p>
                </>
              ) : (
                <>
                  <KpiLabel>{Number(goal.targetAmount) > 0 ? "FALTA PARA O OBJETIVO DO MÊS" : "OBJETIVO MENSAL"}</KpiLabel>
                  <KpiValue>{formatCurrency(Number(goal.remainingAmount))}</KpiValue>
                  <div className="goal-progress-heading"><span>Progresso</span><b>{progress.toLocaleString("pt-BR", { maximumFractionDigits: 0 })}%</b></div>
                  <div className="goal-progress" aria-label={`${progress}% do objetivo coberto`}><span style={{ width: `${progress}%` }} /></div>
                </>
              )}
            </PrimaryKpiCard>

            {renderGoalDetails(goal)}

            {goal.covered && Number(goal.targetAmount) > 0 && isLoadingNextGoal && (
              <div className="goal-next-loading" role="status">
                <div className="loader" aria-hidden="true" />
                <span className="goal-next-loading-copy"><strong>Próximo mês</strong><small>Calculando seu objetivo...</small></span>
              </div>
            )}

            {goal.covered && Number(goal.targetAmount) > 0 && nextGoal && (
              <section className={`goal-next-month${isNextGoalExpanded ? " expanded" : ""}`}>
                <button
                  type="button"
                  className={`goal-next-summary${nextGoal.covered ? " covered" : ""}`}
                  onClick={() => setIsNextGoalExpanded((expanded) => !expanded)}
                  aria-expanded={isNextGoalExpanded}
                >
                  <span className="goal-next-copy">
                    <small>PRÓXIMO MÊS · {goalMonthLabel(nextGoal)}</small>
                    <strong>{nextGoal.covered ? "Mês coberto" : `Falta ${formatCurrency(Number(nextGoal.remainingAmount))}`}</strong>
                    <span>{isNextGoalExpanded ? "Ocultar objetivo" : "Ver objetivo do próximo mês"}</span>
                  </span>
                  <span className="goal-next-toggle" aria-hidden="true">{isNextGoalExpanded ? "−" : "+"}</span>
                </button>

                {isNextGoalExpanded && (
                  <div className="goal-next-details">
                    {nextGoal.covered && Number(nextGoal.targetAmount) > 0 ? (
                      <p className="goal-next-covered-message">O objetivo mensal previsto já está coberto pelo saldo financeiro.</p>
                    ) : renderGoalDetails(nextGoal)}
                  </div>
                )}
              </section>
            )}
          </>
        )}

      </div>

      <BottomNavigation active="goals" loading={isLoading} onHome={onHome} onFinance={onFinance} onRadar={onRadar} onGoals={() => {}} onWork={onWork} />
    </main>
  );
}

type HomeCostDraft = {
  key: string;
  id?: string;
  category: ApiWorkCostCategory;
  amount: string;
  description: string;
};

type HomeEarningDraft = {
  key: string;
  sourceId?: string;
  source: string;
  originalSource: string;
  amount: string;
  persistForFuture: boolean;
};

const HOME_AUTOSAVE_DELAY = 800;
const HOME_UNDO_TIMEOUT = 6000;

type HomeSaveStatus = "idle" | "saved" | "error" | "offline";

type HomeRemovedItem =
  | { kind: "cost"; item: HomeCostDraft; index: number }
  | { kind: "earning"; item: HomeEarningDraft; index: number };

function homeWorkSignature({
  uber,
  ninetyNine,
  kilometers,
  additionalEarnings,
  costs,
}: {
  uber: string;
  ninetyNine: string;
  kilometers: string;
  additionalEarnings: HomeEarningDraft[];
  costs: HomeCostDraft[];
}) {
  return JSON.stringify({
    uber,
    ninetyNine,
    kilometers,
    additionalEarnings: additionalEarnings.map(({ sourceId, source, amount, persistForFuture }) => ({ sourceId, source, amount, persistForFuture })),
    costs: costs
      .filter((item) => item.id || (parseCurrencyInput(item.amount) || 0) > 0)
      .map(({ id, category, amount, description }) => ({ id: id ?? null, category, amount, description })),
  });
}

const usageProfileLabels: Record<ApiUsageProfile, string> = {
  PERSONAL: "Uso pessoal",
  RIDE_DRIVER: "Motorista de aplicativo",
  DELIVERY: "Entregador",
};

function AccountReactivationScreen({ scheduledAt, onReactivate, onLogout }: { scheduledAt: string; onReactivate: () => Promise<void>; onLogout: () => void }) {
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [now] = useState(() => Date.now());
  const remainingDays = Math.max(0, Math.ceil((new Date(scheduledAt).getTime() - now) / 86400000));
  const deletionDate = formatShortDate(new Date(scheduledAt));

  async function reactivate() {
    setSaving(true);
    setError("");
    try {
      await onReactivate();
    } catch (requestError) {
      setError(messageFromError(requestError));
      setSaving(false);
    }
  }

  return <main className="usage-profile-screen">
    <AppHeader />
    <section className="home-empty-state account-reactivation" aria-label="Reativar conta">
      <div className="home-empty-icon"><Icon name="clock" /></div>
      <span>CONTA DESATIVADA</span>
      <h2>{remainingDays === 1 ? "Sua conta será excluída em 1 dia" : `Sua conta será excluída em ${remainingDays} dias`}</h2>
      <p>A exclusão definitiva está agendada para {deletionDate}. Reative agora para continuar usando o AMCash com todos os seus dados.</p>
      {error && <p className="home-odometer-feedback error" role="alert">{error}</p>}
      <div className="home-empty-actions">
        <Button type="button" variant="secondary" disabled={saving} onClick={onLogout}>Sair</Button>
        <Button type="button" className="primary" loading={saving} loadingLabel="Reativando..." onClick={() => void reactivate()}>Reativar conta</Button>
      </div>
    </section>
  </main>;
}

function UsageProfileScreen({ onSelect, onCancel }: { onSelect: (profile: ApiUsageProfile) => Promise<void>; onCancel?: () => void }) {
  const [savingProfile, setSavingProfile] = useState<ApiUsageProfile | null>(null);
  const [profileError, setProfileError] = useState("");
  const options: { profile: ApiUsageProfile; title: string; description: string; icon: IconName }[] = [
    { profile: "PERSONAL", title: "Uso pessoal", description: "Organize suas receitas, despesas e contas a pagar.", icon: "wallet" },
    { profile: "RIDE_DRIVER", title: "Motorista de aplicativo", description: "Acompanhe seus ganhos, custos, quilometragem e metas.", icon: "work" },
    { profile: "DELIVERY", title: "Entregador", description: "Controle seus ganhos por entrega, despesas e metas.", icon: "radar" },
  ];

  async function choose(profile: ApiUsageProfile) {
    if (savingProfile) return;
    setSavingProfile(profile);
    setProfileError("");
    try {
      await onSelect(profile);
    } catch (requestError) {
      setProfileError(messageFromError(requestError));
      setSavingProfile(null);
    }
  }

  return <main className="usage-profile-screen">
    <AppHeader>{onCancel && <button type="button" className="usage-profile-close" onClick={onCancel} aria-label="Fechar">×</button>}</AppHeader>
    <section className="usage-profile-content">
      <span>PERSONALIZE SUA EXPERIÊNCIA</span>
      <h1>Como você usa o AMCash?</h1>
      <p>Escolha seu perfil para personalizar sua experiência. Você poderá alterá-lo quando quiser, sem perder seus dados.</p>
      <div className="usage-profile-options">
        {options.map((option) => <button type="button" key={option.profile} disabled={savingProfile !== null} onClick={() => void choose(option.profile)}>
          <span className="usage-profile-icon"><Icon name={option.icon} /></span>
          <span><strong>{option.title}</strong><small>{option.description}</small></span>
          <Icon name="forward" />
        </button>)}
      </div>
      {profileError && <p className="usage-profile-error" role="alert">{profileError}</p>}
      {savingProfile && <small className="usage-profile-saving">Salvando {usageProfileLabels[savingProfile]}...</small>}
    </section>
  </main>;
}

function notificationIndicator(notifications: ApiFinancialNotification[]) {
  const unread = notifications.filter((item) => item.readAt === null);
  if (unread.some((item) => item.type === "OVERDUE")) return "overdue";
  return unread.length > 0 ? "upcoming" : null;
}

function AccountMenu({ onLogout, usageProfile, onChangeProfile, notifications, onNotifications }: { onLogout: () => void; usageProfile: ApiUsageProfile; onChangeProfile: () => void; notifications: ApiFinancialNotification[]; onNotifications: () => void }) {
  const [open, setOpen] = useState(false);
  const [user, setUser] = useState<AuthenticatedUser | null>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [deleteAcknowledged, setDeleteAcknowledged] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState("");
  const canDelete = deleteAcknowledged;

  function closeDeleteDialog() {
    if (deleting) return;
    setConfirmDelete(false);
    setDeleteAcknowledged(false);
    setDeleteError("");
  }

  async function deleteAccount() {
    if (!canDelete || deleting) return;
    setDeleting(true);
    setDeleteError("");
    try {
      await financeApi.deleteAccount();
      onLogout();
    } catch (error) {
      setDeleteError(messageFromError(error));
      setDeleting(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => {
      try {
        const saved = window.localStorage.getItem("amcash.user");
        setUser(saved ? JSON.parse(saved) as AuthenticatedUser : null);
      } catch {
        setUser(null);
      }
    }, 0);
    return () => window.clearTimeout(timer);
  }, []);

  const initial = user?.name?.trim().charAt(0).toLocaleUpperCase("pt-BR") || "U";
  const indicator = notificationIndicator(notifications);
  return <div className="account-menu">
    <button type="button" className="account-menu-trigger" aria-label="Abrir menu da conta" aria-haspopup="menu" aria-expanded={open} onClick={() => setOpen((current) => !current)}>{initial}{indicator && <i className={`notification-dot ${indicator}`} aria-label="Novos alertas financeiros" />}</button>
    {open && <>
      <button type="button" className="account-menu-dismiss" aria-label="Fechar menu da conta" onClick={() => setOpen(false)} />
      <section className="account-menu-popover" role="menu">
        <div className="account-menu-user"><span>{initial}</span><div><strong>{user?.name || "Sua conta"}</strong>{user?.email && <small>{user.email}</small>}</div></div>
        <button type="button" className="account-menu-profile account-menu-notifications" role="menuitem" onClick={() => { setOpen(false); onNotifications(); }}><Icon name="bell" /><span><strong>Notificações</strong><small>Consulte seus alertas.</small></span>{indicator && <i className={`notification-dot ${indicator}`} aria-hidden="true" />}</button>
        <button type="button" className="account-menu-profile" role="menuitem" onClick={() => { setOpen(false); onChangeProfile(); }}><Icon name="target" /><span><strong>Modo de uso</strong><small>{usageProfileLabels[usageProfile]}</small></span></button>
        <button type="button" className="account-menu-logout" role="menuitem" onClick={onLogout}><Icon name="logout" /><span><strong>Sair da conta</strong><small>Encerrar sessão</small></span></button>
        <button type="button" className="account-menu-logout account-menu-delete" role="menuitem" onClick={() => { setOpen(false); setConfirmDelete(true); }}><Icon name="trash" /><span><strong>Excluir conta</strong><small>Desativar e excluir em 30 dias</small></span></button>
      </section>
    </>}
    {confirmDelete && <div className="sheet-backdrop confirm-backdrop" role="presentation" onMouseDown={closeDeleteDialog}>
      <section className="confirm-dialog account-delete-dialog" role="alertdialog" aria-modal="true" aria-labelledby="account-delete-title" aria-describedby="account-delete-description" onMouseDown={(event) => event.stopPropagation()}>
        <div className="confirm-icon"><Icon name="trash" /></div>
        <h2 id="account-delete-title">Deseja excluir sua conta?</h2>
        <p id="account-delete-description">Sua conta será desativada agora e excluída definitivamente em 30 dias. Até lá, você pode reativá-la fazendo login.</p>
        <div className="account-delete-list">
          <strong>Após 30 dias, serão excluídos:</strong>
          <ul>
            <li>Receitas, despesas e lançamentos</li>
            <li>Metas e histórico de trabalho</li>
            <li>Veículos, quilometragem e manutenções</li>
            <li>Preferências e informações da conta</li>
          </ul>
        </div>
        <label className="account-delete-check"><input type="checkbox" checked={deleteAcknowledged} disabled={deleting} onChange={(event) => { setDeleteAcknowledged(event.target.checked); setDeleteError(""); }} /><span>Entendo que, após 30 dias, meus dados não poderão ser recuperados.</span></label>
        {deleteError && <p className="home-odometer-feedback error" role="alert">{deleteError}</p>}
        <div><button type="button" disabled={deleting} onClick={closeDeleteDialog}>Cancelar</button><button type="button" disabled={!canDelete || deleting} onClick={() => void deleteAccount()}>{deleting ? "Excluindo..." : "Excluir minha conta"}</button></div>
      </section>
    </div>}
  </div>;
}

function HomeDashboard({ onFinance, onCreateExpense, onCreateIncome, onRadar, onGoals, onWork, onVehicles, onLogout, onReady, usageProfile, onChangeProfile, notifications, onNotifications, onMileageUpdated }: {
  onFinance: () => void;
  onCreateExpense: () => void;
  onCreateIncome: () => void;
  onRadar: () => void;
  onGoals: () => void;
  onWork: () => void;
  onVehicles: () => void;
  onLogout: () => void;
  onReady?: () => void;
  usageProfile: ApiUsageProfile;
  onChangeProfile: () => void;
  notifications: ApiFinancialNotification[];
  onNotifications: () => void;
  onMileageUpdated: () => Promise<void>;
}) {
  const [summary, setSummary] = useState<ApiHome | null>(null);
  const [workOverview, setWorkOverview] = useState<ApiWorkOverview | null>(null);
  const [uber, setUber] = useState("");
  const [ninetyNine, setNinetyNine] = useState("");
  const [additionalEarnings, setAdditionalEarnings] = useState<HomeEarningDraft[]>([]);
  const [kilometers, setKilometers] = useState("");
  const [homeCosts, setHomeCosts] = useState<HomeCostDraft[]>([]);
  const [homeVehicles, setHomeVehicles] = useState<ApiVehicle[]>([]);
  const [selectedVehicleId, setSelectedVehicleId] = useState("");
  const [odometerValue, setOdometerValue] = useState("");
  const [odometerSaving, setOdometerSaving] = useState(false);
  const [odometerMessage, setOdometerMessage] = useState("");
  const [odometerError, setOdometerError] = useState("");
  const [initialWorkSignature, setInitialWorkSignature] = useState("");
  const [openCostKey, setOpenCostKey] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [saveStatus, setSaveStatus] = useState<HomeSaveStatus>("idle");
  const [failedWorkSignature, setFailedWorkSignature] = useState("");
  const [removedItem, setRemovedItem] = useState<HomeRemovedItem | null>(null);
  const [error, setError] = useState("");
  const [workError, setWorkError] = useState("");
  const [sourceModalOpen, setSourceModalOpen] = useState(false);
  const [costDialogOpen, setCostDialogOpen] = useState(false);
  const [pendingFixedSourceDelete, setPendingFixedSourceDelete] = useState<HomeEarningDraft | null>(null);
  const [deletingFixedSource, setDeletingFixedSource] = useState(false);
  const nextCostKey = useRef(1);
  const savingLockRef = useRef(false);
  const saveWorkRef = useRef<() => Promise<void>>(async () => {});
  const unsavedWorkRef = useRef(false);
  const flushWorkRef = useRef(false);
  const today = dateToInputValue(new Date());
  const homeCostPrefix = "HOME: ";
  const legacyHomeCostDescription = "Custo do dia — Home";

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    setWorkError("");
    try {
      const [home, work, vehicles, earningSources] = await Promise.all([
        financeApi.home(today),
        usageProfile === "PERSONAL" ? Promise.resolve(null) : financeApi.workOverview(today, "DAY"),
        usageProfile === "PERSONAL" ? Promise.resolve([] as ApiVehicle[]) : financeApi.vehicles().catch(() => [] as ApiVehicle[]),
        usageProfile === "PERSONAL" ? Promise.resolve([] as ApiWorkEarningSource[]) : financeApi.workEarningSources(),
      ]);
      setSummary(home);
      setWorkOverview(work);
      setHomeVehicles(vehicles);
      setSelectedVehicleId(vehicles.find((vehicle) => vehicle.inUse)?.id ?? vehicles[0]?.id ?? "");
      if (!work) return;
      const day = work.days.find((item) => item.date === today);
      const [primaryPlatform, secondaryPlatform] = profileWorkPlatforms(usageProfile);
      const uberEarning = day?.earnings.find((item) => isWorkPlatformSource(primaryPlatform, item.source));
      const ninetyNineEarning = day?.earnings.find((item) => isWorkPlatformSource(secondaryPlatform, item.source));
      const savedHomeCosts = work.costs.filter((item) => item.description === legacyHomeCostDescription || item.description?.startsWith(homeCostPrefix));
      const nextUber = maskCurrencyInput(String(Math.round(Number(uberEarning?.amount ?? 0) * 100)));
      const nextNinetyNine = maskCurrencyInput(String(Math.round(Number(ninetyNineEarning?.amount ?? 0) * 100)));
      const usedEarningIds = new Set<string>();
      if (uberEarning) usedEarningIds.add(uberEarning.id);
      if (ninetyNineEarning) usedEarningIds.add(ninetyNineEarning.id);
      const nextAdditionalEarnings: HomeEarningDraft[] = earningSources.filter((source) => !source.systemKey).map((source) => {
        const earning = day?.earnings.find((item) => !usedEarningIds.has(item.id) && item.source.trim().toLocaleLowerCase("pt-BR") === source.name.trim().toLocaleLowerCase("pt-BR"));
        if (earning) usedEarningIds.add(earning.id);
        return { key: source.id, sourceId: source.id, source: source.name, originalSource: source.name,
          amount: maskCurrencyInput(String(Math.round(Number(earning?.amount ?? 0) * 100))), persistForFuture: true };
      });
      day?.earnings.filter((item) => !usedEarningIds.has(item.id) && Number(item.amount) > 0).forEach((item) => nextAdditionalEarnings.push({
        key: `earning-${item.id}`, source: item.source, originalSource: item.source,
        amount: maskCurrencyInput(String(Math.round(Number(item.amount) * 100))), persistForFuture: false,
      }));
      const nextKilometers = Number(day?.kilometers ?? 0) > 0 ? String(Number(day?.kilometers)).replace(".", ",") : "";
      const nextHomeCosts = savedHomeCosts.length ? savedHomeCosts.map((item) => {
        const storedDescription = item.description?.startsWith(homeCostPrefix) ? item.description.slice(homeCostPrefix.length) : "";
        return {
          key: item.id,
          id: item.id,
          category: item.category,
          amount: maskCurrencyInput(String(Math.round(Number(item.amount) * 100))),
          description: storedDescription === workCostCategoryLabels[item.category] ? "" : storedDescription,
        };
      }) : [];
      setUber(nextUber);
      setNinetyNine(nextNinetyNine);
      setAdditionalEarnings(nextAdditionalEarnings);
      setKilometers(nextKilometers);
      setHomeCosts(nextHomeCosts);
      setInitialWorkSignature(homeWorkSignature({ uber: nextUber, ninetyNine: nextNinetyNine, kilometers: nextKilometers, additionalEarnings: nextAdditionalEarnings, costs: nextHomeCosts }));
      setOpenCostKey(null);
    } catch (requestError) {
      setError(messageFromError(requestError));
    } finally {
      setLoading(false);
    }
  }, [today, usageProfile]);

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load]);
  useEffect(() => { if (!loading) onReady?.(); }, [loading, onReady]);

  const rawDateLabel = new Intl.DateTimeFormat("pt-BR", { weekday: "short", day: "2-digit", month: "short" })
    .format(new Date(`${today}T12:00:00`))
    .replace(/\./g, "");
  const dateLabel = rawDateLabel.charAt(0).toLocaleUpperCase("pt-BR") + rawDateLabel.slice(1);
  const uberValue = parseCurrencyInput(uber) || 0;
  const ninetyNineValue = parseCurrencyInput(ninetyNine) || 0;
  const additionalEarningsValue = additionalEarnings.reduce((total, item) => total + (parseCurrencyInput(item.amount) || 0), 0);
  const kilometersValue = parseDecimalInput(kilometers);
  const costValue = homeCosts.reduce((total, item) => total + (parseCurrencyInput(item.amount) || 0), 0);
  const projectedGrossEarnings = uberValue + ninetyNineValue + additionalEarningsValue;
  const netValue = projectedGrossEarnings - costValue;
  const savedDay = workOverview?.days.find((item) => item.date === today);
  const savedEditableEarnings = savedDay?.earnings.reduce((total, item) => total + Number(item.amount), 0) ?? 0;
  const earningsPerKilometer = kilometersValue > 0 ? projectedGrossEarnings / kilometersValue : 0;
  const savedHomeCosts = workOverview?.costs.filter((item) => item.description === legacyHomeCostDescription || item.description?.startsWith(homeCostPrefix)) ?? [];
  const savedHomeCostTotal = savedHomeCosts.reduce((total, item) => total + Number(item.amount), 0);
  const resultOutsideHomeFields = Number(workOverview?.summary.netEarnings ?? 0) - savedEditableEarnings + savedHomeCostTotal;
  const projectedTodayNet = resultOutsideHomeFields + netValue;
  const dailyTarget = Number(summary?.goal.dailyTarget ?? 0);
  const dailyRemaining = Number(summary?.goal.todayRemaining ?? 0);
  const dailyProgress = Number(summary?.goal.progressPercentage ?? 0);
  const dailyGoalCompleted = dailyTarget > 0 && Boolean(summary?.goal.achieved);
  const todayGross = Number(summary?.goal.todayGross ?? 0);
  const showEmptyState = summary?.hasFinancialEntries === false && Number(summary?.goal.targetAmount ?? 0) === 0;
  const additionalEarningsAreValid = additionalEarnings.every((item) => item.source.trim().length > 0
    && ((parseCurrencyInput(item.amount) || 0) > 0 || item.persistForFuture || Boolean(item.sourceId)));
  const [firstPlatform, secondPlatform] = profileWorkPlatforms(usageProfile);
  const earningSourceCounts = [firstPlatform.name.toLocaleLowerCase("pt-BR"), secondPlatform.name.toLocaleLowerCase("pt-BR"), ...additionalEarnings.map((item) => item.source.trim().toLocaleLowerCase("pt-BR")).filter(Boolean)]
    .reduce<Map<string, number>>((counts, source) => counts.set(source, (counts.get(source) ?? 0) + 1), new Map());
  const duplicateEarningSources = new Set([...earningSourceCounts.entries()].filter(([, count]) => count > 1).map(([source]) => source));
  const duplicateEarningError = duplicateEarningSources.size > 0 ? "Cada ganho deve aparecer apenas uma vez." : "";
  const workFormError = duplicateEarningError || workError;
  const currentWorkSignature = homeWorkSignature({ uber, ninetyNine, kilometers, additionalEarnings, costs: homeCosts });
  const hasWorkChanges = initialWorkSignature !== "" && initialWorkSignature !== currentWorkSignature;
  const workIsValid = additionalEarningsAreValid && !duplicateEarningError;
  const workSaveFailed = hasWorkChanges && currentWorkSignature === failedWorkSignature;
  const autoSaveReady = Boolean(summary) && hasWorkChanges && workIsValid && !workSaveFailed;
  const displayedSaveStatus = saving || autoSaveReady ? "saving"
    : hasWorkChanges && !workIsValid ? "invalid"
      : workSaveFailed ? saveStatus === "offline" ? "offline" : "error"
        : saveStatus === "saved" ? "saved" : "idle";
  const selectedHomeVehicle = homeVehicles.find((vehicle) => vehicle.id === selectedVehicleId) ?? null;
  const parsedOdometer = parseDecimalInput(odometerValue);

  function updateHomeCost(key: string, changes: Partial<Pick<HomeCostDraft, "category" | "amount" | "description">>) {
    setHomeCosts((current) => current.map((item) => item.key === key ? { ...item, ...changes } : item));
  }

  function addHomeCost(value: CostDialogValue) {
    const key = `new-${nextCostKey.current++}`;
    setHomeCosts((current) => [...current, { key, ...value }]);
    setOpenCostKey(null);
    setCostDialogOpen(false);
  }

  function removeHomeCost(key: string) {
    const index = homeCosts.findIndex((item) => item.key === key);
    if (index < 0) return;
    setOpenCostKey((current) => current === key ? null : current);
    setHomeCosts((current) => current.filter((item) => item.key !== key));
    setRemovedItem({ kind: "cost", item: homeCosts[index], index });
  }

  function undoRemoval() {
    if (!removedItem) return;
    if (removedItem.kind === "cost") {
      // O registro original pode já ter sido excluído no servidor: recria como novo custo.
      const restored: HomeCostDraft = { ...removedItem.item, id: undefined, key: `new-${nextCostKey.current++}` };
      setHomeCosts((current) => [...current.slice(0, removedItem.index), restored, ...current.slice(removedItem.index)]);
    } else {
      const restored = removedItem.item;
      setAdditionalEarnings((current) => [...current.slice(0, removedItem.index), restored, ...current.slice(removedItem.index)]);
    }
    setRemovedItem(null);
  }

  function updateAdditionalEarning(index: number, changes: Partial<HomeEarningDraft>) {
    setWorkError("");
    setAdditionalEarnings((current) => current.map((item, itemIndex) => itemIndex === index ? { ...item, ...changes } : item));
  }

  function openSourceModal() {
    setSourceModalOpen(true);
  }

  function removeAdditionalEarning(item: HomeEarningDraft) {
    setWorkError("");
    if (item.sourceId) {
      setPendingFixedSourceDelete(item);
      return;
    }
    const index = additionalEarnings.findIndex((earning) => earning.key === item.key);
    setAdditionalEarnings((current) => current.filter((earning) => earning.key !== item.key));
    if (index >= 0) setRemovedItem({ kind: "earning", item, index });
  }

  async function confirmFixedSourceDelete() {
    const deleted = pendingFixedSourceDelete;
    const sourceId = deleted?.sourceId;
    if (!deleted || !sourceId || deletingFixedSource) return;
    setDeletingFixedSource(true);
    setWorkError("");
    try {
      await financeApi.deactivateWorkEarningSource(sourceId, today);
      setAdditionalEarnings((current) => current.filter((item) => item.key !== deleted.key));
      setInitialWorkSignature((signature) => {
        if (!signature) return signature;
        const parsed = JSON.parse(signature) as { additionalEarnings: Array<Record<string, unknown>> };
        parsed.additionalEarnings = parsed.additionalEarnings.filter((item) => item.sourceId !== sourceId);
        return JSON.stringify(parsed);
      });
      setPendingFixedSourceDelete(null);
    } catch (requestError) {
      setWorkError(messageFromError(requestError));
      setPendingFixedSourceDelete(null);
    } finally {
      setDeletingFixedSource(false);
    }
  }

  async function saveDailyResult() {
    if (!summary || savingLockRef.current || !hasWorkChanges || !workIsValid) return;
    const savedSignature = currentWorkSignature;
    if (!navigator.onLine) {
      setFailedWorkSignature(savedSignature);
      setSaveStatus("offline");
      return;
    }
    const snapshotEarnings = additionalEarnings;
    const snapshotCosts = homeCosts;
    let additional = snapshotEarnings.filter((item) => item.sourceId || item.source.trim() || item.amount);
    const createdCostIds = new Map<string, string>();
    savingLockRef.current = true;
    setSaving(true);
    setWorkError("");
    try {
      for (const earning of additional) {
        let resolved: HomeEarningDraft | null = null;
        if (earning.sourceId && earning.source.trim() !== earning.originalSource) {
          const updated = await financeApi.renameWorkEarningSource(earning.sourceId, earning.source.trim());
          resolved = { ...earning, originalSource: updated.name };
        } else if (!earning.sourceId && earning.persistForFuture && earning.source.trim()) {
          const created = await financeApi.createWorkEarningSource(earning.source.trim());
          resolved = { ...earning, sourceId: created.id, originalSource: created.name };
        }
        if (!resolved) continue;
        const { sourceId, originalSource } = resolved;
        additional = additional.map((item) => item.key === earning.key ? resolved : item);
        setAdditionalEarnings((current) => current.map((item) => item.key === earning.key ? { ...item, sourceId, originalSource } : item));
      }

      const hasDayValues = kilometersValue > 0 || uberValue > 0 || ninetyNineValue > 0
        || additional.some((item) => (parseCurrencyInput(item.amount) || 0) > 0);
      if (savedDay || hasDayValues) {
        await financeApi.saveWorkDay(today, {
          kilometers: kilometersValue,
          vehicleId: selectedHomeVehicle?.id ?? null,
          earnings: [
            { source: firstPlatform.name, amount: uberValue },
            { source: secondPlatform.name, amount: ninetyNineValue },
            ...additional.map((item) => ({ source: item.source.trim(), amount: parseCurrencyInput(item.amount) || 0 })),
          ],
        });
      }

      const validCosts = snapshotCosts
        .map((item) => ({ ...item, parsedAmount: parseCurrencyInput(item.amount) || 0 }))
        .filter((item) => item.parsedAmount > 0);
      const retainedCostIds = new Set(validCosts.flatMap((item) => item.id ? [item.id] : []));
      await Promise.all([
        ...validCosts.map(async (item) => {
          const payload = { date: today, category: item.category, description: `${homeCostPrefix}${item.description.trim() || workCostCategoryLabels[item.category]}`, amount: item.parsedAmount };
          if (item.id) {
            await financeApi.updateWorkCost(item.id, payload);
            return;
          }
          const created = await financeApi.createWorkCost(payload);
          createdCostIds.set(item.key, created.id);
          setHomeCosts((current) => current.map((cost) => cost.key === item.key ? { ...cost, id: created.id } : cost));
        }),
        ...savedHomeCosts.filter((item) => !retainedCostIds.has(item.id)).map((item) => financeApi.deleteWorkCost(item.id)),
      ]);

      const [home, work] = await Promise.all([financeApi.home(today), financeApi.workOverview(today, "DAY")]);
      setSummary(home);
      setWorkOverview(work);
      setInitialWorkSignature(homeWorkSignature({
        uber,
        ninetyNine,
        kilometers,
        additionalEarnings: snapshotEarnings.map((item) => additional.find((resolved) => resolved.key === item.key) ?? item),
        costs: snapshotCosts.map((item) => createdCostIds.has(item.key) ? { ...item, id: createdCostIds.get(item.key) } : item),
      }));
      setFailedWorkSignature("");
      setSaveStatus("saved");
    } catch {
      setFailedWorkSignature(savedSignature);
      setSaveStatus(navigator.onLine ? "error" : "offline");
    } finally {
      savingLockRef.current = false;
      setSaving(false);
    }
  }

  useEffect(() => {
    saveWorkRef.current = saveDailyResult;
    unsavedWorkRef.current = hasWorkChanges || saving;
    flushWorkRef.current = autoSaveReady;
  });

  useEffect(() => {
    if (!autoSaveReady || saving) return;
    const timer = window.setTimeout(() => void saveWorkRef.current(), HOME_AUTOSAVE_DELAY);
    return () => window.clearTimeout(timer);
  }, [autoSaveReady, saving, currentWorkSignature]);

  useEffect(() => {
    if (saveStatus !== "saved") return;
    const timer = window.setTimeout(() => setSaveStatus("idle"), 3000);
    return () => window.clearTimeout(timer);
  }, [saveStatus]);

  useEffect(() => {
    if (!removedItem) return;
    const timer = window.setTimeout(() => setRemovedItem(null), HOME_UNDO_TIMEOUT);
    return () => window.clearTimeout(timer);
  }, [removedItem]);

  useEffect(() => {
    const flush = () => { if (flushWorkRef.current) void saveWorkRef.current(); };
    const handleVisibility = () => { if (document.visibilityState === "hidden") flush(); };
    const handleBeforeUnload = (event: BeforeUnloadEvent) => { if (unsavedWorkRef.current) event.preventDefault(); };
    const handleOnline = () => { setFailedWorkSignature(""); setSaveStatus("idle"); };
    document.addEventListener("visibilitychange", handleVisibility);
    window.addEventListener("beforeunload", handleBeforeUnload);
    window.addEventListener("online", handleOnline);
    return () => {
      document.removeEventListener("visibilitychange", handleVisibility);
      window.removeEventListener("beforeunload", handleBeforeUnload);
      window.removeEventListener("online", handleOnline);
      flush();
    };
  }, []);

  async function updateHomeOdometer() {
    if (!selectedHomeVehicle || !odometerValue.trim() || odometerSaving) return;
    if (parsedOdometer <= Number(selectedHomeVehicle.currentMileage)) {
      setOdometerError("A nova quilometragem deve ser maior que a última leitura.");
      setOdometerMessage("");
      return;
    }
    setOdometerSaving(true);
    setOdometerError("");
    setOdometerMessage("");
    try {
      const reading = await financeApi.updateOdometer(selectedHomeVehicle.id, { mileage: parsedOdometer, readingDate: today, correction: false });
      setHomeVehicles((current) => current.map((vehicle) => vehicle.id === selectedHomeVehicle.id
        ? { ...vehicle, currentMileage: reading.mileage, mileageReadingDate: reading.readingDate }
        : vehicle));
      setOdometerValue("");
      setOdometerMessage("Quilometragem atualizada.");
      await onMileageUpdated();
    } catch (requestError) {
      setOdometerError(messageFromError(requestError));
    } finally {
      setOdometerSaving(false);
    }
  }

  return <main className="daily-home-screen">
    <AppHeader className="daily-home-header">
      <div className="daily-home-account"><time className="daily-home-date" dateTime={today}>{dateLabel}</time><AccountMenu onLogout={onLogout} usageProfile={usageProfile} onChangeProfile={onChangeProfile} notifications={notifications} onNotifications={onNotifications} /></div>
    </AppHeader>
    <div className="daily-home-content">
      {loading ? null : error ?
        <div className="daily-home-error"><p>{error}</p><button type="button" onClick={() => void load()}>Tentar novamente</button></div> : summary && <>
          {showEmptyState ? <section className="home-empty-state" aria-label="Começar a usar o AMCash">
            <div className="home-empty-icon"><Icon name="wallet" /></div>
            <span>PRIMEIROS PASSOS</span>
            <h2>Organize seu mês</h2>
            <p>Registre suas receitas e despesas para acompanhar seu saldo e saber quanto sobra no fim do mês.</p>
            <div className="home-empty-actions">
              <Button type="button" variant="secondary" onClick={onCreateIncome}><Icon name="income" /><span>Adicionar receita</span></Button>
              <Button type="button" className="primary" onClick={onCreateExpense}><Icon name="expense" /><span>Adicionar despesa</span></Button>
            </div>
          </section> : <>
          {usageProfile === "PERSONAL" && <>
          <PrimaryKpiCard className="radar-balance-card personal-balance-card">
            <KpiLabel>SALDO DO MÊS</KpiLabel>
            <KpiValue className={summary.financial.balance < 0 ? "negative" : ""} title={`${summary.financial.balance < 0 ? "- " : ""}${formatCurrency(summary.financial.balance)}`}>{summary.financial.balance < 0 ? "- " : ""}{formatKpiCurrency(summary.financial.balance)}</KpiValue>
            <small>{summary.financial.pendingExpenseCount > 0
              ? `Faltam ${formatCurrency(summary.financial.pendingExpenses)} em ${summary.financial.pendingExpenseCount} ${summary.financial.pendingExpenseCount === 1 ? "conta" : "contas"} a pagar`
              : "Nenhuma conta pendente neste mês"}</small>
          </PrimaryKpiCard>
          <SecondaryKpiGroup className="personal-month-summary" aria-label="Resumo financeiro do mês">
            <SecondaryKpiItem><KpiLabel>RECEITAS DO MÊS</KpiLabel><KpiValue title={formatCurrency(summary.financial.income)}>{formatKpiCurrency(summary.financial.income)}</KpiValue></SecondaryKpiItem>
            <SecondaryKpiItem><KpiLabel>A PAGAR</KpiLabel><KpiValue className={summary.financial.pendingExpenses > 0 ? "cost" : ""} title={formatCurrency(summary.financial.pendingExpenses)}>{formatKpiCurrency(summary.financial.pendingExpenses)}</KpiValue></SecondaryKpiItem>
          </SecondaryKpiGroup>
          {summary.nextExpense ? <button type="button" className="home-next-expense" onClick={onFinance} aria-label={`Próxima conta: ${summary.nextExpense.name}, vence em ${formatShortDate(summary.nextExpense.dueDate)}, ${formatCurrency(summary.nextExpense.amount)}`}>
            <span className="home-next-expense-icon"><Icon name="calendar" /></span>
            <span className="home-next-expense-main"><small>Próxima conta</small><strong>{summary.nextExpense.name}</strong><span>Vence em {formatShortDate(summary.nextExpense.dueDate)}</span></span>
            <b className="home-next-expense-value expense">{formatCurrency(summary.nextExpense.amount)}</b>
            <span className="work-day-chevron" aria-hidden="true">›</span>
          </button> : <button type="button" className="home-next-expense" onClick={onFinance}>
            <span className="home-next-expense-icon"><Icon name="check" /></span>
            <span className="home-next-expense-main"><small>Próxima conta</small><strong>Nenhuma conta pendente</strong><span>Suas contas do mês estão em dia</span></span>
            <b className="home-next-expense-value" />
            <span className="work-day-chevron" aria-hidden="true">›</span>
          </button>}
          <div className="home-empty-actions personal-quick-actions">
            <Button type="button" variant="secondary" onClick={onCreateIncome}><Icon name="income" /><span>Receita</span></Button>
            <Button type="button" variant="secondary" onClick={onCreateExpense}><Icon name="expense" /><span>Despesa</span></Button>
          </div>
          </>}
          {usageProfile !== "PERSONAL" && <>
          <PrimaryKpiCard className={`daily-goal-card${!summary.goal.todaySelected ? " day-off" : dailyGoalCompleted ? " daily-completed" : summary.goal.covered && dailyTarget === 0 ? " covered" : ""}`}>
            {!summary.goal.todaySelected ? <div className="daily-goal-day-off">
              <span>DIA DE FOLGA</span>
              <strong>Hoje você não planejou trabalhar</strong>
              <p>Aproveite o descanso. Se os planos mudarem, inclua o dia de hoje no seu planejamento.</p>
              <button type="button" onClick={onGoals}>Alterar dias em Objetivos <Icon name="forward" /></button>
            </div> : dailyGoalCompleted ? <div className="daily-goal-completed-content">
              <i className="daily-goal-confetti confetti-one" aria-hidden="true" />
              <i className="daily-goal-confetti confetti-two" aria-hidden="true" />
              <i className="daily-goal-confetti confetti-three" aria-hidden="true" />
              <i className="daily-goal-confetti confetti-four" aria-hidden="true" />
              <div className="daily-goal-title"><span><Icon name="target" />OBJETIVO DE HOJE</span></div>
              <div className="daily-goal-completed-values">
                <span><strong title={formatCurrency(dailyTarget)}>{formatKpiCurrency(dailyTarget)}</strong><small><i><Icon name="check" /></i>Meta concluída!</small></span>
                <div><i><Icon name="check" /></i><span><small>Concluído</small><b title={formatCurrency(todayGross)}>{formatKpiCurrency(todayGross)}</b></span></div>
              </div>
            </div> : summary.goal.covered && dailyTarget === 0 ? <>
              <span>OBJETIVO DE HOJE</span>
              <strong>Meta concluída</strong>
            </> : <>
              <div className="daily-goal-title"><span><Icon name="target" />OBJETIVO DE HOJE</span></div>
              <div className="daily-goal-values"><strong title={formatCurrency(dailyTarget)}>{formatKpiCurrency(dailyTarget)}</strong><div><Icon name="clock" /><span><small>Faltam</small><b title={formatCurrency(dailyRemaining)}>{formatKpiCurrency(dailyRemaining)}</b></span></div></div>
              <div className="daily-goal-progress-label"><span>Progresso</span><strong>{dailyProgress}%</strong></div>
              <div className="daily-progress-track" aria-label={`${dailyProgress}% do objetivo de hoje`}><i style={{ width: `${dailyProgress}%` }} /></div>
            </>}
          </PrimaryKpiCard>

          {summary.goal.todaySelected && <>
          <SecondaryKpiGroup columns={3} className="home-today-summary" aria-label="Resumo do trabalho de hoje">
            <SecondaryKpiItem><KpiLabel>FATURAMENTO HOJE</KpiLabel><KpiValue title={formatCurrency(projectedGrossEarnings)}>{formatKpiCurrency(projectedGrossEarnings)}</KpiValue></SecondaryKpiItem>
            <SecondaryKpiItem><KpiLabel>RESULTADO LÍQUIDO</KpiLabel><KpiValue className={projectedTodayNet < 0 ? "negative" : ""} title={`${projectedTodayNet < 0 ? "- " : ""}${formatCurrency(projectedTodayNet)}`}>{projectedTodayNet < 0 ? "- " : ""}{formatKpiCurrency(projectedTodayNet)}</KpiValue></SecondaryKpiItem>
            <SecondaryKpiItem><KpiLabel>FATURAMENTO POR KM</KpiLabel><KpiValue title={kilometersValue > 0 ? formatCurrency(earningsPerKilometer) : undefined}>{kilometersValue > 0 ? formatKpiCurrency(earningsPerKilometer) : "—"}</KpiValue></SecondaryKpiItem>
          </SecondaryKpiGroup>
          <section className="home-quick-entry" aria-label="Trabalho de hoje">
            <div className="home-work-card-heading"><div><span>TRABALHO DE HOJE</span></div><small className={`home-work-save-status ${displayedSaveStatus}`} role="status" aria-live="polite">
              {displayedSaveStatus === "saving" ? "Salvando..."
                : displayedSaveStatus === "saved" ? <><Icon name="check" />Alterações salvas</>
                  : displayedSaveStatus === "invalid" ? "Corrija os campos para salvar"
                    : displayedSaveStatus === "offline" ? "Sem conexão. Salvaremos ao reconectar."
                      : displayedSaveStatus === "error" ? <>Não foi possível salvar.<button type="button" onClick={() => { setFailedWorkSignature(""); setSaveStatus("idle"); }}>Tentar novamente</button></>
                        : "Edite os valores diretamente"}
            </small></div>
            <div className="home-work-fields">
              <label><span className="home-work-source logo metric-logo"><Image src="/road-icon.png" alt="" width={1254} height={1254} /></span><span>Quilômetros rodados</span><div><b>km</b><input aria-label="Quilômetros rodados" value={kilometers} onChange={(event) => setKilometers(maskDecimalInput(event.target.value))} placeholder="0" inputMode="decimal" /></div></label>
              <label><span className={`home-work-source ${firstPlatform.className} logo`}><Image src={firstPlatform.logo} alt="" width={512} height={512} /></span><span>{firstPlatform.name}</span><div><b>R$</b><input aria-label={`Faturamento ${firstPlatform.name}`} className={uber.length > 8 ? "long-value" : undefined} value={uber} onChange={(event) => { setUber(maskLimitedCurrencyInput(event.target.value)); setWorkError(""); }} placeholder="0,00" inputMode="numeric" /></div></label>
              <label><span className={`home-work-source ${secondPlatform.className} logo`}><Image src={secondPlatform.logo} alt="" width={512} height={512} /></span><span>{secondPlatform.name}</span><div><b>R$</b><input aria-label={`Faturamento ${secondPlatform.name}`} className={ninetyNine.length > 8 ? "long-value" : undefined} value={ninetyNine} onChange={(event) => { setNinetyNine(maskLimitedCurrencyInput(event.target.value)); setWorkError(""); }} placeholder="0,00" inputMode="numeric" /></div></label>
              {additionalEarnings.map((item, index) => {
                const duplicateSource = duplicateEarningSources.has(item.source.trim().toLocaleLowerCase("pt-BR"));
                return <div className="home-additional-source-entry" key={item.key}><div className={`home-additional-source-row${duplicateSource ? " invalid" : ""}`}><span className="home-work-source additional"><Icon name="wallet" /></span><span className="home-additional-source-copy"><input className="home-additional-source-name" maxLength={30} aria-label={`Nome do ganho adicional ${index + 1}`} aria-invalid={duplicateSource || undefined} value={item.source} onChange={(event) => updateAdditionalEarning(index, { source: event.target.value })} placeholder="Ganho adicional" /><button type="button" aria-label={item.sourceId ? `Excluir ganho fixo ${item.source}` : `Remover ganho adicional ${index + 1}`} onClick={() => removeAdditionalEarning(item)}><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6M10 10v6M14 10v6" /></svg></button></span><div><b>R$</b><input aria-label={`Valor do ganho adicional ${index + 1}`} className={item.amount.length > 8 ? "long-value" : undefined} value={item.amount} onChange={(event) => updateAdditionalEarning(index, { amount: maskLimitedCurrencyInput(event.target.value) })} placeholder="0,00" inputMode="numeric" /></div></div></div>;
              })}
              {workFormError && <p className="home-work-inline-error" role="alert"><Icon name="warning" /><span>{workFormError}</span></p>}
              <button type="button" className="home-add-source" onClick={openSourceModal}><Icon name="plus" />Adicionar outro ganho</button>
            </div>
            <div className="home-costs-block">
              <div className="home-costs-heading"><div><span>CUSTOS DE HOJE</span></div></div>
              <div className="home-costs-list">
                {homeCosts.map((item, index) => <div className={`home-cost-row${openCostKey === item.key ? " expanded" : ""}`} key={item.key}>
                  <button type="button" className="home-cost-summary" aria-expanded={openCostKey === item.key} onClick={() => setOpenCostKey((current) => current === item.key ? null : item.key)}>
                    <span className="home-cost-summary-icon"><CostCategoryIcon category={item.category} /></span><span className="home-cost-summary-copy"><strong>{workCostCategoryLabels[item.category]}</strong>{item.description.trim() && <small>{item.description.trim()}</small>}</span><b>{formatCurrency(parseCurrencyInput(item.amount) || 0)}</b><svg className="home-cost-summary-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true"><path d="m8 10 4 4 4-4" /></svg>
                  </button>
                  {openCostKey === item.key && <div className="home-cost-editor">
                    <span className="home-cost-category-label">Categoria</span><CostCategoryGrid value={item.category} ariaLabel={`Categoria do custo ${index + 1}`} onChange={(category) => updateHomeCost(item.key, { category })} />
                    <label><span>Valor</span><div><b>R$</b><input value={item.amount} onChange={(event) => updateHomeCost(item.key, { amount: maskCurrencyInput(event.target.value) })} placeholder="0,00" inputMode="numeric" /></div></label>
                    <label><span>Descrição (opcional)</span><div><input maxLength={60} value={item.description} onChange={(event) => updateHomeCost(item.key, { description: event.target.value })} placeholder="Ex.: Abastecimento no posto" /></div></label>
                    <button type="button" className="home-cost-remove" onClick={() => removeHomeCost(item.key)}><Icon name="trash" />Remover custo</button>
                  </div>}
                </div>)}
              </div>
              <button type="button" className="home-add-cost" onClick={() => setCostDialogOpen(true)}><Icon name="plus" />Adicionar custo</button>
            </div>
          </section>
          {homeVehicles.length > 0 ? <section className="home-odometer-update" aria-labelledby="home-odometer-title">
            <header className="home-odometer-heading">
              <h3 id="home-odometer-title">QUILOMETRAGEM DO VEÍCULO</h3>
              {homeVehicles.length === 1 && <small>{selectedHomeVehicle?.nickname || `${selectedHomeVehicle?.brand ?? ""} ${selectedHomeVehicle?.model ?? ""}`.trim()}</small>}
            </header>
            {homeVehicles.length > 1 && selectedHomeVehicle && <p className="home-odometer-active-vehicle">Veículo em uso: <strong>{selectedHomeVehicle.nickname||`${selectedHomeVehicle.brand} ${selectedHomeVehicle.model}`}</strong></p>}
            <div className="home-odometer-entry">
              {selectedHomeVehicle && <div className="home-odometer-last"><span>Última leitura</span><strong>{new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 1 }).format(selectedHomeVehicle.currentMileage)} km</strong><time dateTime={selectedHomeVehicle.mileageReadingDate}>{formatDate(selectedHomeVehicle.mileageReadingDate)}</time></div>}
              <InputField fieldClassName="home-odometer-new" label="Nova leitura" prefix="km" inputMode="decimal" placeholder="0" value={odometerValue} onChange={(event) => { setOdometerValue(maskDecimalInput(event.target.value)); setOdometerError(""); setOdometerMessage(""); }} />
            </div>
            <Button type="button" fullWidth loading={odometerSaving} loadingLabel="Atualizando..." disabled={!odometerValue.trim() || parsedOdometer <= Number(selectedHomeVehicle?.currentMileage ?? 0)} onClick={() => void updateHomeOdometer()}>Atualizar quilometragem</Button>
            {odometerError && <p className="home-odometer-feedback error" role="alert">{odometerError}</p>}
            {odometerMessage && <p className="home-odometer-feedback success" role="status">{odometerMessage}</p>}
          </section> : <section className="home-odometer-empty"><h3>QUILOMETRAGEM DO VEÍCULO</h3><p>Cadastre um veículo para registrar o hodômetro e acompanhar quanto você roda.</p><button type="button" onClick={onVehicles}>Cadastrar veículo</button></section>}
          </>}
          </>}

          </>}
        </>}
    </div>
    {sourceModalOpen && <EarningSourceDialog existingSources={["Uber", "99", ...additionalEarnings.map((item) => item.source)]} onClose={() => setSourceModalOpen(false)} onAdd={(value) => { setAdditionalEarnings((current) => [...current, { key: crypto.randomUUID(), source: value.source, originalSource: "", amount: value.amount, persistForFuture: value.persistForFuture }]); setWorkError(""); setSourceModalOpen(false); }} />}
    {costDialogOpen && <CostDialog onClose={() => setCostDialogOpen(false)} onAdd={addHomeCost} />}
    {pendingFixedSourceDelete && <div className="sheet-backdrop confirm-backdrop" role="presentation" onMouseDown={() => !deletingFixedSource && setPendingFixedSourceDelete(null)}><section className="confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="delete-fixed-source-title" onMouseDown={(event) => event.stopPropagation()}><span className="confirm-icon"><Icon name="trash" /></span><h2 id="delete-fixed-source-title">Excluir ganho fixo?</h2><p>O ganho “{pendingFixedSourceDelete.source}” e o valor registrado hoje serão excluídos. Os registros anteriores serão mantidos.</p><div><button type="button" disabled={deletingFixedSource} onClick={() => setPendingFixedSourceDelete(null)}>Cancelar</button><button type="button" disabled={deletingFixedSource} onClick={() => void confirmFixedSourceDelete()}>{deletingFixedSource ? "Excluindo..." : "Excluir ganho"}</button></div></section></div>}
    <div className="home-undo-anchor" role="status" aria-live="polite">{removedItem && <div className="home-undo-bar" key={`${removedItem.kind}-${removedItem.item.key}`}><span>{removedItem.kind === "cost" ? "Custo removido" : "Ganho removido"}</span><button type="button" onClick={undoRemoval}>Desfazer</button></div>}</div>
    <BottomNavigation active="home" loading={loading} onHome={() => {}} onFinance={onFinance} onRadar={onRadar} onGoals={onGoals} onWork={onWork} showWorkFeatures={usageProfile !== "PERSONAL"} />
  </main>;
}

function NotificationsScreen({ notifications, onBack, onOpen, error }: { notifications: ApiFinancialNotification[]; onBack: () => void; onOpen: (item: ApiFinancialNotification) => void; error: string }) {
  const today = dateToInputValue(new Date());
  const dayDifference = (date: string) => Math.round((Date.parse(`${date}T00:00:00Z`) - Date.parse(`${today}T00:00:00Z`)) / 86400000);
  const groups = [
    { key: "today", label: "Hoje", items: notifications.filter((item) => dayDifference(item.dueDate) === 0) },
    { key: "upcoming", label: "Próximos dias", items: notifications.filter((item) => dayDifference(item.dueDate) > 0) },
    { key: "overdue", label: "Vencidas", items: notifications.filter((item) => item.type === "OVERDUE") },
  ].filter((group) => group.items.length > 0);
  return <main className="notifications-screen">
    <AppHeader className="notifications-header"><button type="button" className="notifications-back" onClick={onBack} aria-label="Voltar"><Icon name="back" /></button><h1>Notificações</h1></AppHeader>
    <section className={`notifications-content${notifications.length === 0 ? " empty" : ""}`} aria-label="Alertas">
      {error && <p className="auth-error" role="alert">{error}</p>}
      {!error && notifications.length === 0 ? <div className="notifications-empty"><span><Icon name="check" /></span><h2>Tudo em dia!</h2><p>Você não possui alertas no momento.</p></div> : groups.map((group) => <section className="notification-group" key={group.key} aria-labelledby={`notification-group-${group.key}`}>
        <h2 id={`notification-group-${group.key}`}>{group.label}</h2>
        <div className="notification-group-list">{group.items.map((item) => {
          const days = dayDifference(item.dueDate);
          const overdue = item.type === "OVERDUE";
          const mileageReminder = item.type === "MILEAGE_UPDATE";
          const operationalSuggestion = item.type === "MILEAGE_SUGGESTION";
          const tone = overdue ? "overdue" : days === 0 ? "today" : "upcoming";
          const title = overdue ? "Conta vencida" : days === 0 ? "Sua conta vence hoje" : `Conta vence em ${days} ${days === 1 ? "dia" : "dias"}`;
          return <button type="button" className={`notification-row ${tone}`} key={item.id} onClick={() => onOpen(item)}>
            <i className={`notification-dot ${tone}`} aria-hidden="true" />
            <span className="notification-icon">{operationalSuggestion?<Image src="/vehicle-menu-icon.svg" width={20} height={20} alt=""/>:<Icon name={overdue ? "warning" : "calendar"} />}</span>
            <span className="notification-copy"><strong>{operationalSuggestion?"Revise a quilometragem mensal":item.source === "VEHICLE" ? mileageReminder ? "Atualize a quilometragem" : overdue ? "Manutenção vencida" : "Próxima manutenção" : title}</strong><b>{item.expenseName}</b><time dateTime={item.dueDate}>{item.source !== "FINANCIAL" ? item.message : `Vencimento ${formatDate(item.dueDate)}`}</time></span>
            <span className="notification-value">{item.amount !== null && <strong>{formatCurrency(item.amount)}</strong>}<Icon name="forward" /></span>
          </button>;
        })}</div>
      </section>)}
    </section>
  </main>;
}

function MileageSuggestionDialog({vehicleName,data,preferredSource,loading,error,onClose,onUpdate,onReview,onDismiss}:{vehicleName:string;data:ApiMileageSuggestions;preferredSource:ApiMileageSource|null;loading:boolean;error:string;onClose:()=>void;onUpdate:(mileage:number,source:ApiMileageSource)=>void;onReview:()=>void;onDismiss:(source:Exclude<ApiMileageSource,"MANUAL">)=>void}){
  const[selectedSource,setSelectedSource]=useState<ApiMileageSource>(preferredSource??data.options.find(option=>option.available)?.source??"MANUAL");
  const selected=data.options.find(item=>item.source===selectedSource);
  return <div className="sheet-backdrop mileage-suggestion-backdrop" role="presentation" onMouseDown={()=>!loading&&onClose()}><section className="mileage-suggestion-dialog" role="dialog" aria-modal="true" aria-labelledby="mileage-suggestion-title" onMouseDown={event=>event.stopPropagation()}>
    <div className="mileage-suggestion-icon"><Image src="/vehicle-menu-icon.svg" width={28} height={28} alt=""/></div>
    <span>QUILOMETRAGEM MENSAL</span><h2 id="mileage-suggestion-title">Atualizar {vehicleName}?</h2><p>Configurado atualmente: <strong>{new Intl.NumberFormat("pt-BR",{maximumFractionDigits:0}).format(data.configuredMileage)} km/mês</strong></p>
    <div className="mileage-suggestion-options">{data.options.map(option=><button type="button" disabled={!option.available} className={selectedSource===option.source?"selected":""} aria-pressed={selectedSource===option.source} key={option.source} onClick={()=>setSelectedSource(option.source)}><span><strong>{option.source==="ODOMETER"?"Uso total do veículo":"Somente trabalho"}</strong><small>{option.available&&option.periodStart&&option.periodEnd?formatDateRange(option.periodStart,option.periodEnd):option.unavailableReason}</small></span><b>{option.monthlyMileage==null?"Indisponível":`${new Intl.NumberFormat("pt-BR",{maximumFractionDigits:0}).format(option.monthlyMileage)} km`}</b></button>)}</div>
    {error&&<p className="work-error" role="alert">{error}</p>}
    <Button type="button" fullWidth loading={loading} disabled={!selected?.available||selected.monthlyMileage==null} onClick={()=>selected?.monthlyMileage!=null&&onUpdate(selected.monthlyMileage,selected.source)}>Atualizar quilometragem</Button>
    <button type="button" className="mileage-review" disabled={loading} onClick={onReview}>Revisar configuração</button>
    <button type="button" className="mileage-dismiss" disabled={loading||preferredSource==null||preferredSource==="MANUAL"} onClick={()=>preferredSource&&preferredSource!=="MANUAL"&&onDismiss(preferredSource)}>Desconsiderar sugestão</button>
  </section></div>;
}

export default function Home() {
  const [screen, setScreen] = useState<"checking" | "auth" | "loading" | "refreshing" | "app">("checking");
  const [isLaunchSplashVisible, setIsLaunchSplashVisible] = useState(true);
  const [activeTab, setActiveTab] = useState<NavigationTab>("home");
  const [pendingTab, setPendingTab] = useState<NavigationTab | null>(null);
  const [usageProfile, setUsageProfile] = useState<ApiUsageProfile | null | undefined>(undefined);
  const [profilePickerOpen, setProfilePickerOpen] = useState(false);
  const [deletionScheduledAt, setDeletionScheduledAt] = useState<string | null>(null);
  const [financeCreateRequested, setFinanceCreateRequested] = useState<"expense" | "income" | null>(null);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [notifications, setNotifications] = useState<ApiFinancialNotification[]>([]);
  const [notificationsError, setNotificationsError] = useState("");
  const [mileageSuggestion, setMileageSuggestion] = useState<{ notification: ApiFinancialNotification; data: ApiMileageSuggestions } | null>(null);
  const [mileageSuggestionLoading, setMileageSuggestionLoading] = useState(false);
  const [mileageSuggestionError, setMileageSuggestionError] = useState("");
  const [operationalEditVehicleId, setOperationalEditVehicleId] = useState<string | null>(null);
  const [items, setItems] = useState<Transaction[]>([]);
  const [selected, setSelected] = useState<Transaction | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const [period, setPeriod] = useState(() => {
    const today = new Date();
    return { year: today.getFullYear(), month: today.getMonth() + 1 };
  });
  const lastLoadedPeriod = useRef("");
  const requestedPeriod = useRef("");
  const requestId = useRef(0);

  const refreshNotifications = useCallback(async () => {
    try {
      setNotifications(await financeApi.notifications());
      setNotificationsError("");
    } catch (requestError) {
      if (!(requestError instanceof ApiError && requestError.status === 401)) setNotificationsError(messageFromError(requestError));
    }
  }, []);

  const logout = useCallback(() => {
    window.localStorage.removeItem("amcash.accessToken");
    window.localStorage.removeItem("amcash.user");
    setDeletionScheduledAt(null);
    setItems([]);
    setSelected(null);
    setError("");
    setActiveTab("home");
    setPendingTab(null);
    setUsageProfile(undefined);
    setProfilePickerOpen(false);
    setFinanceCreateRequested(null);
    setNotificationsOpen(false);
    setNotifications([]);
    lastLoadedPeriod.current = "";
    requestedPeriod.current = "";
    setScreen("auth");
  }, []);

  const loadMonth = useCallback(async (year: number, month: number) => {
    const periodKey = `${year}-${month}`;
    requestedPeriod.current = periodKey;
    const currentRequest = ++requestId.current;
    setIsLoading(true);
    setError("");
    try {
      const response = await financeApi.listMonth(year, month);
      if (currentRequest !== requestId.current) return false;
      const mappedEntries = mapEntries(response.entries);
      setItems(mappedEntries);
      setSelected((current) => current
        ? mappedEntries.find((entry) => entry.id === current.id) ?? current
        : null);
      lastLoadedPeriod.current = periodKey;
      return true;
    } catch (requestError) {
      if (currentRequest !== requestId.current) return false;
      if (requestError instanceof ApiError && requestError.status === 401) {
        logout();
        return false;
      }
      setItems([]);
      setError(messageFromError(requestError));
      return false;
    } finally {
      if (currentRequest === requestId.current) setIsLoading(false);
    }
  }, [logout]);

  useEffect(() => {
    const savedToken = window.localStorage.getItem("amcash.accessToken");
    let active = true;
    const sessionCheck = window.setTimeout(() => {
      if (!savedToken) {
        setScreen("auth");
        return;
      }
      let savedUser: AuthenticatedUser | null = null;
      try {
        const stored = window.localStorage.getItem("amcash.user");
        savedUser = stored ? JSON.parse(stored) as AuthenticatedUser : null;
      } catch {
        savedUser = null;
      }
      setUsageProfile(savedUser?.usageProfile ?? null);
      setDeletionScheduledAt(savedUser?.deletionScheduledAt ?? null);
      financeApi.userProfile().then((profile) => {
        if (!active) return;
        const refreshedUser: AuthenticatedUser = profile;
        window.localStorage.setItem("amcash.user", JSON.stringify(refreshedUser));
        setUsageProfile(profile.usageProfile);
        setDeletionScheduledAt(profile.deletionScheduledAt);
        setScreen("refreshing");
      }).catch((requestError) => {
        if (!active || requestError instanceof ApiError && requestError.status === 401) return;
        setScreen("refreshing");
      });
    }, savedToken ? 0 : 1000);
    return () => { active = false; window.clearTimeout(sessionCheck); };
  }, []);

  useEffect(() => {
    const splashTimer = window.setTimeout(() => setIsLaunchSplashVisible(false), 1000);
    return () => window.clearTimeout(splashTimer);
  }, []);

  useEffect(() => {
    const showingSplash = isLaunchSplashVisible || screen === "checking" || screen === "refreshing" || screen === "loading";
    const backgroundColor = screen === "auth" || showingSplash ? "#f8faff" : "#ffffff";
    document.documentElement.style.backgroundColor = backgroundColor;
    document.body.style.backgroundColor = backgroundColor;
    document.querySelector<HTMLMetaElement>('meta[name="theme-color"]')?.setAttribute("content", backgroundColor);
  }, [isLaunchSplashVisible, screen]);

  useEffect(() => {
    window.addEventListener("amcash:unauthorized", logout);
    return () => window.removeEventListener("amcash:unauthorized", logout);
  }, [logout]);

  useEffect(() => {
    if (screen !== "loading" && screen !== "refreshing") return;
    let active = true;
    const minimumLoading = screen === "loading" ? 1400 : 1000;
    const startLoading = window.setTimeout(() => {
      new Promise((resolve) => window.setTimeout(resolve, minimumLoading)).then(() => {
        if (active) setScreen((current) => current === "auth" ? "auth" : "app");
      });
    }, 0);
    return () => {
      active = false;
      window.clearTimeout(startLoading);
    };
  }, [screen]);

  useEffect(() => {
    if (screen !== "app" || !usageProfile || deletionScheduledAt) return;
    const timer = window.setTimeout(() => void refreshNotifications(), 0);
    return () => window.clearTimeout(timer);
  }, [screen, usageProfile, deletionScheduledAt, refreshNotifications]);

  const changeMonth = useCallback((year: number, month: number) => {
    const periodKey = `${year}-${month}`;
    setPeriod({ year, month });
    if (requestedPeriod.current !== periodKey || lastLoadedPeriod.current !== periodKey) {
      void loadMonth(year, month);
    }
  }, [loadMonth]);

  async function addTransaction(transaction: NewTransaction) {
    await financeApi.create({
      name: transaction.name,
      category: transaction.category,
      type: transaction.income ? "INCOME" : "EXPENSE",
      amount: transaction.value,
      dueDate: transaction.date,
      recurrenceFrequency: recurrenceToApi[transaction.recurrenceFrequency],
      recurrenceCount: transaction.recurrenceCount,
      hasSubexpenses: transaction.hasSubexpenses,
      paymentMethod: transaction.paymentMethod,
      notes: transaction.notes || null,
      subexpenses: transaction.items.map((item) => ({
        name: item.name,
        amount: item.value,
        installmentDescription: null,
        paid: false,
        recurrenceFrequency: recurrenceToApi[item.recurrenceFrequency],
        recurrenceCount: item.recurrenceCount,
      })),
    });
    await loadMonth(period.year, period.month);
    await refreshNotifications();
  }

  async function deleteTransaction(entryId: string, scope: ApiSeriesScope) {
    await financeApi.delete(entryId, scope);
    setSelected(null);
    await loadMonth(period.year, period.month);
    await refreshNotifications();
  }

  async function deleteDashboardTransaction(transaction: Transaction, scope: ApiSeriesScope) {
    await deleteTransaction(transaction.id, scope);
  }

  async function markTransactionPaid(transaction: Transaction) {
    await financeApi.setPaid(transaction.id, !transaction.paid);
    await loadMonth(period.year, period.month);
    await refreshNotifications();
  }

  async function refreshCurrentMonth() {
    await loadMonth(period.year, period.month);
    await refreshNotifications();
  }

  async function reactivateAccount() {
    const profile = await financeApi.reactivateAccount();
    window.localStorage.setItem("amcash.user", JSON.stringify(profile));
    setUsageProfile(profile.usageProfile);
    setDeletionScheduledAt(null);
  }

  async function chooseUsageProfile(profile: ApiUsageProfile) {
    const updatedUser = await financeApi.updateUsageProfile(profile);
    window.localStorage.setItem("amcash.user", JSON.stringify(updatedUser));
    setUsageProfile(updatedUser.usageProfile);
    setProfilePickerOpen(false);
    setActiveTab("home");
  }

  if (isLaunchSplashVisible || screen === "checking" || screen === "refreshing" || screen === "loading") return <LoadingScreen />;
  if (screen === "auth") return <AuthScreen onAuthenticated={(user) => { setUsageProfile(user.usageProfile); setDeletionScheduledAt(user.deletionScheduledAt ?? null); setScreen("loading"); }} />;
  if (deletionScheduledAt) return <AccountReactivationScreen scheduledAt={deletionScheduledAt} onLogout={logout} onReactivate={reactivateAccount} />;
  if (usageProfile === undefined) return <LoadingScreen />;
  if (usageProfile === null || profilePickerOpen) return <UsageProfileScreen onSelect={chooseUsageProfile} onCancel={usageProfile ? () => setProfilePickerOpen(false) : undefined} />;
  const showWorkFeatures = usageProfile !== "PERSONAL";
  const navigateTo = (tab: NavigationTab) => {
    if (tab === activeTab) return;
    setPendingTab(tab);
  };
  const openWork = () => navigateTo("work");
  const openGoals = () => navigateTo("goals");
  const openFinance = (targetPeriod?: { year: number; month: number }) => {
    const today = new Date();
    const nextPeriod = targetPeriod ?? { year: today.getFullYear(), month: today.getMonth() + 1 };
    changeMonth(nextPeriod.year, nextPeriod.month);
    navigateTo("finance");
  };
  const openNotifications = async () => {
    setNotificationsOpen(true);
    setNotifications((current) => current.map((item) => ({ ...item, readAt: item.readAt ?? new Date().toISOString() })));
    try {
      setNotifications(await financeApi.markNotificationsRead());
      setNotificationsError("");
    } catch (requestError) {
      setNotificationsError(messageFromError(requestError));
    }
  };
  const openNotificationEntry = async (notification: ApiFinancialNotification) => {
    if (notification.source === "OPERATIONAL_COST" && notification.vehicleId) {
      setMileageSuggestionLoading(true);
      setMileageSuggestionError("");
      try {
        const data = await financeApi.operationalMileageSuggestions(notification.vehicleId);
        setMileageSuggestion({ notification, data });
      } catch (requestError) {
        setNotificationsError(messageFromError(requestError));
      } finally {
        setMileageSuggestionLoading(false);
      }
      return;
    }
    if (notification.source === "VEHICLE") {
      setNotificationsOpen(false);
      setPendingTab("vehicles");
      return;
    }
    if (!notification.entryId) return;
    try {
      const entry = await financeApi.get(notification.entryId);
      setNotificationsOpen(false);
      setSelected(mapEntries([entry])[0]);
    } catch (requestError) {
      setNotificationsError(messageFromError(requestError));
    }
  };
  const updateSuggestedMileage = async (mileage: number, source: ApiMileageSource) => {
    if (!mileageSuggestion?.notification.vehicleId) return;
    setMileageSuggestionLoading(true);setMileageSuggestionError("");
    try { await financeApi.updateOperationalMileage(mileageSuggestion.notification.vehicleId,mileage,source);setMileageSuggestion(null);await refreshNotifications(); }
    catch(requestError){setMileageSuggestionError(messageFromError(requestError));}
    finally{setMileageSuggestionLoading(false);}
  };
  const dismissSuggestedMileage = async (source:Exclude<ApiMileageSource,"MANUAL">) => {
    if (!mileageSuggestion?.notification.vehicleId) return;
    setMileageSuggestionLoading(true);setMileageSuggestionError("");
    try { await financeApi.dismissOperationalMileageSuggestion(mileageSuggestion.notification.vehicleId,source);setMileageSuggestion(null);await refreshNotifications(); }
    catch(requestError){setMileageSuggestionError(messageFromError(requestError));}
    finally{setMileageSuggestionLoading(false);}
  };
  const reviewSuggestedMileage = () => {
    const vehicleId=mileageSuggestion?.notification.vehicleId;if(!vehicleId)return;setOperationalEditVehicleId(vehicleId);setMileageSuggestion(null);setNotificationsOpen(false);navigateTo("operational-cost");
  };
  if (notificationsOpen) return <><NotificationsScreen notifications={notifications} error={notificationsError} onBack={() => setNotificationsOpen(false)} onOpen={(item) => void openNotificationEntry(item)} />{mileageSuggestion&&<MileageSuggestionDialog vehicleName={mileageSuggestion.notification.expenseName} data={mileageSuggestion.data} preferredSource={mileageSuggestion.notification.suggestionSource} loading={mileageSuggestionLoading} error={mileageSuggestionError} onClose={()=>setMileageSuggestion(null)} onUpdate={(mileage,source)=>void updateSuggestedMileage(mileage,source)} onReview={reviewSuggestedMileage} onDismiss={(source)=>void dismissSuggestedMileage(source)}/>}</>;
  if (selected) {
    return (
      <DetailScreen
        transaction={selected}
        onBack={() => setSelected(null)}
        onDelete={deleteTransaction}
        onChanged={refreshCurrentMonth}
      />
    );
  }

  const renderTab = (tab: NavigationTab, isPending: boolean) => {
    const onReady = isPending ? () => { setActiveTab(tab); setPendingTab(null); } : undefined;
    if (tab === "radar") return <RadarScreen onReady={onReady} onHome={() => navigateTo("home")} onFinance={openFinance} onGoals={() => openGoals()} onWork={openWork} showWorkFeatures={showWorkFeatures} />;
    if (tab === "goals") return <GoalsScreen onReady={onReady} onHome={() => navigateTo("home")} onFinance={() => openFinance()} onRadar={() => navigateTo("radar")} onWork={openWork} />;
    if (tab === "work") return <WorkScreen onReady={onReady} onOperationalCost={() => navigateTo("operational-cost")} navigation={(loading) => <BottomNavigation active="work" loading={loading} onHome={() => navigateTo("home")} onFinance={() => openFinance()} onRadar={() => navigateTo("radar")} onGoals={() => openGoals()} onWork={() => {}} />} />;
    if (tab === "vehicles") return <VehicleScreen onReady={onReady} navigation={(loading) => <BottomNavigation active="vehicles" loading={loading} onHome={() => navigateTo("home")} onFinance={() => openFinance()} onRadar={() => navigateTo("radar")} onGoals={() => openGoals()} onWork={openWork} />} />;
    if (tab === "operational-cost") return <OperationalCostScreen onReady={onReady} initialEditVehicleId={operationalEditVehicleId} onInitialEditHandled={()=>setOperationalEditVehicleId(null)} onVehicles={() => navigateTo("vehicles")} navigation={(loading) => <BottomNavigation active="operational-cost" loading={loading} onHome={() => navigateTo("home")} onFinance={() => openFinance()} onRadar={() => navigateTo("radar")} onGoals={() => openGoals()} onWork={openWork} />} />;
    if (tab === "finance") return <Dashboard onReady={onReady} items={items} onOpen={setSelected} onCreate={addTransaction} onDelete={deleteDashboardTransaction} onPaid={markTransactionPaid} onHome={() => navigateTo("home")} onRadar={() => navigateTo("radar")} onGoals={() => openGoals()} onWork={openWork} showWorkFeatures={showWorkFeatures} onMonthChange={changeMonth} initialPeriod={period} initialCreateType={financeCreateRequested} onInitialCreateHandled={() => setFinanceCreateRequested(null)} error={error} isLoading={isLoading} />;
    return <HomeDashboard onReady={onReady} onFinance={() => openFinance()} onCreateExpense={() => { setFinanceCreateRequested("expense"); openFinance(); }} onCreateIncome={() => { setFinanceCreateRequested("income"); openFinance(); }} onRadar={() => navigateTo("radar")} onGoals={openGoals} onWork={openWork} onVehicles={() => navigateTo("vehicles")} onLogout={logout} usageProfile={usageProfile} onChangeProfile={() => setProfilePickerOpen(true)} notifications={notifications} onNotifications={() => void openNotifications()} onMileageUpdated={refreshNotifications} />;
  };
  const renderedTabs = pendingTab && pendingTab !== activeTab ? [activeTab, pendingTab] : [activeTab];
  return <VehicleNavigationContext.Provider value={() => navigateTo("vehicles")}><OperationalCostNavigationContext.Provider value={() => navigateTo("operational-cost")}><NavigationLoadingContext.Provider value={pendingTab}>
    {renderedTabs.map((tab) => <div className={`app-tab-stage${tab === activeTab ? " active" : " pending"}`} key={tab} aria-hidden={tab !== activeTab}>{renderTab(tab, tab === pendingTab)}</div>)}
  </NavigationLoadingContext.Provider></OperationalCostNavigationContext.Provider></VehicleNavigationContext.Provider>;
}
