"use client";

import Image from "next/image";
import Script from "next/script";
import { useCallback, useEffect, useRef, useState } from "react";
import {
  ApiEntry,
  ApiEntryCategory,
  ApiError,
  ApiRecurrenceFrequency,
  ApiSubexpense,
  financeApi,
} from "@/lib/api";

type GoogleCredentialResponse = {
  credential?: string;
};

type AuthenticatedUser = {
  id: string;
  name: string;
  email: string;
  pictureUrl: string | null;
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
};

type Subexpense = {
  id: string;
  name: string;
  amount: string;
  installment: string;
  paid: boolean;
  recurrenceFrequency: RecurrenceFrequency;
  recurrenceCount: number;
};

type IconName = Transaction["kind"] | "search" | "more" | "back" | "forward" | "home" | "edit" | "trash" | "plus" | "check";

function Icon({ name }: { name: IconName }) {
  if (name === "more") {
    return <svg viewBox="0 0 24 24"><circle cx="12" cy="5" r="1.6" /><circle cx="12" cy="12" r="1.6" /><circle cx="12" cy="19" r="1.6" /></svg>;
  }

  const paths = {
    search: <><circle cx="10.8" cy="10.8" r="6.2" /><path d="m15.4 15.4 4.2 4.2" /></>,
    back: <path d="m15 18-6-6 6-6" />,
    forward: <path d="m9 18 6-6-6-6" />,
    home: <><path d="m3.5 10 8.5-7 8.5 7" /><path d="M5.5 9v11h13V9M9.5 20v-6h5v6" /></>,
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
  };

  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">{paths[name]}</svg>;
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(Math.abs(value));
}

function formatDate(date: string) {
  return new Intl.DateTimeFormat("pt-BR").format(new Date(`${date}T12:00:00`));
}

function maskCurrencyInput(value: string) {
  const digits = value.replace(/\D/g, "");
  if (!digits) return "";

  const normalized = digits.replace(/^0+(?=\d)/, "");
  const padded = normalized.padStart(3, "0");
  const rawInteger = padded.slice(0, -2);
  const integer = (rawInteger.replace(/^0+(?=\d)/, "") || "0")
    .replace(/\B(?=(\d{3})+(?!\d))/g, ".");
  const decimal = padded.slice(-2);

  return `${integer},${decimal}`;
}

function completeCurrencyInput(value: string) {
  return maskCurrencyInput(value);
}

function parseCurrencyInput(value: string) {
  return Number(value.replace(/\./g, "").replace(",", "."));
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
  };
}

function mapEntries(entries: ApiEntry[]) {
  let balance = 0;
  return entries.map((entry): Transaction => {
    const income = entry.type === "INCOME";
    balance += income ? Number(entry.amount) : -Number(entry.amount);
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

function dateToInputValue(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
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
  const selectedLabel = options.find((option) => option.value === value)?.label;

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
        <span>{selectedLabel ?? "Selecione uma categoria"}</span>
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true"><path d="m8 10 4 4 4-4" /></svg>
      </button>

      {isOpen && (
        <div className="category-picker-backdrop" role="presentation" onMouseDown={() => setIsOpen(false)}>
          <section className="category-picker-dialog" role="dialog" aria-modal="true" aria-labelledby="category-picker-title" onMouseDown={(event) => event.stopPropagation()}>
            <div className="category-picker-heading">
              <div><span>CATEGORIA</span><h2 id="category-picker-title">Selecionar categoria</h2></div>
              <button type="button" onClick={() => setIsOpen(false)} aria-label="Fechar">×</button>
            </div>
            <div className="category-picker-options">
              {options.map((option) => (
                <button
                  type="button"
                  key={option.value}
                  className={value === option.value ? "selected" : ""}
                  onClick={() => { onChange(option.value); setIsOpen(false); }}
                  aria-pressed={value === option.value}
                >
                  {option.label}
                  {value === option.value && <Icon name="check" />}
                </button>
              ))}
            </div>
          </section>
        </div>
      )}
    </div>
  );
}

function DatePickerInput({
  label,
  value,
  onChange,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
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
    onChange(dateToInputValue(date));
    setIsOpen(false);
  }

  return (
    <div className="field date-picker-field">
      <span>{label}</span>
      <button type="button" className="date-input-button" onClick={openCalendar} aria-haspopup="dialog" aria-expanded={isOpen}>
        <span>{value ? formatDate(value) : "Selecionar data"}</span>
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
          <rect x="3.5" y="5" width="17" height="15" rx="2" />
          <path d="M8 3v4M16 3v4M3.5 9.5h17" />
        </svg>
      </button>

      {isOpen && (
        <div className="date-picker-backdrop" role="presentation" onMouseDown={() => setIsOpen(false)}>
          <section className="date-picker-dialog" role="dialog" aria-modal="true" aria-label="Selecionar data" onMouseDown={(event) => event.stopPropagation()}>
            <div className="date-picker-header">
              <button type="button" onClick={() => setVisibleMonth((current) => new Date(current.getFullYear(), current.getMonth() - 1, 1))} aria-label="Mês anterior"><Icon name="back" /></button>
              <strong>{monthLabel}</strong>
              <button type="button" onClick={() => setVisibleMonth((current) => new Date(current.getFullYear(), current.getMonth() + 1, 1))} aria-label="Próximo mês"><Icon name="forward" /></button>
            </div>

            <div className="date-picker-weekdays" aria-hidden="true">
              {["D", "S", "T", "Q", "Q", "S", "S"].map((weekday, index) => <span key={`${weekday}-${index}`}>{weekday}</span>)}
            </div>
            <div className="date-picker-days">
              {days.map((date) => {
                const dateValue = dateToInputValue(date);
                const outside = date.getMonth() !== visibleMonth.getMonth();
                return (
                  <button
                    type="button"
                    key={dateValue}
                    className={`${outside ? "outside " : ""}${dateValue === value ? "selected " : ""}${dateValue === todayValue ? "today" : ""}`}
                    onClick={() => chooseDate(date)}
                    aria-pressed={dateValue === value}
                  >
                    {date.getDate()}
                  </button>
                );
              })}
            </div>
            <div className="date-picker-actions">
              <button type="button" onClick={() => setIsOpen(false)}>Cancelar</button>
              <button type="button" onClick={() => chooseDate(new Date())}>Hoje</button>
            </div>
          </section>
        </div>
      )}
    </div>
  );
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
  onClose,
  onCreate,
}: {
  defaultDate: string;
  onClose: () => void;
  onCreate: (transaction: NewTransaction) => Promise<void>;
}) {
  type DraftExpenseItem = { id: string; name: string; amount: string; recurrenceFrequency: RecurrenceFrequency; recurrenceCount: string };

  const [type, setType] = useState<"expense" | "income">("expense");
  const [name, setName] = useState("");
  const [category, setCategory] = useState<ApiEntryCategory | "">("");
  const [amount, setAmount] = useState("");
  const [date, setDate] = useState(defaultDate);
  const [recurrenceFrequency, setRecurrenceFrequency] = useState<RecurrenceFrequency>("none");
  const [recurrenceCount, setRecurrenceCount] = useState("");
  const [hasSubexpenses, setHasSubexpenses] = useState(false);
  const [expenseItems, setExpenseItems] = useState<DraftExpenseItem[]>([]);
  const [editingItemId, setEditingItemId] = useState<string | null>(null);
  const [discardAction, setDiscardAction] = useState<"disable" | "income" | null>(null);
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

  function toggleDetailedExpense(checked: boolean) {
    if (checked) {
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

  function selectType(nextType: "expense" | "income") {
    if (nextType === type) return;
    if (nextType === "income" && hasSubexpenses && hasFilledItems) {
      setDiscardAction("income");
      return;
    }
    if (nextType === "income") clearDetailedExpense();
    setType(nextType);
    setCategory("");
  }

  function confirmDiscardItems() {
    const action = discardAction;
    clearDetailedExpense();
    setDiscardAction(null);
    if (action === "income") {
      setType("income");
      setCategory("");
    }
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
      });
    } catch (requestError) {
      setError(messageFromError(requestError));
      setIsSubmitting(false);
    }
  }

  return (
    <div className="sheet-backdrop create-transaction-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="subexpense-sheet create-transaction-sheet" role="dialog" aria-modal="true" aria-labelledby="create-transaction-title" onMouseDown={(event) => event.stopPropagation()}>
        <div className="sheet-handle" aria-hidden="true" />
        <div className="sheet-heading create-sheet-heading">
          <h2 id="create-transaction-title">Novo lançamento</h2>
          <button type="button" className="sheet-close" onClick={onClose} aria-label="Fechar">×</button>
        </div>

        <form onSubmit={submit}>
          <div className="type-switch" aria-label="Tipo do lançamento">
            <button type="button" className={type === "expense" ? "active expense" : ""} onClick={() => selectType("expense")}><Icon name="card" /> Despesa</button>
            <button type="button" className={type === "income" ? "active income" : ""} onClick={() => selectType("income")}><Icon name="wallet" /> Receita</button>
          </div>

          <label className="field full-field"><span>Descrição</span><input value={name} onChange={(event) => setName(event.target.value)} placeholder={type === "expense" ? "Ex.: Supermercado" : "Ex.: Salário"} /></label>
          <CategoryPickerInput value={category} options={type === "expense" ? expenseCategories : incomeCategories} onChange={setCategory} />

          {type === "expense" && (
            <label className="paid-toggle subexpense-toggle detail-expense-toggle">
              <input type="checkbox" checked={hasSubexpenses} onChange={(event) => toggleDetailedExpense(event.target.checked)} />
              <span><b>Quero detalhar esta despesa</b><small>Adicione os itens que compõem o valor total.</small></span>
            </label>
          )}

          {hasSubexpenses ? (
            <>
              <section className="expense-items-editor" aria-labelledby="expense-items-title">
                <h3 id="expense-items-title">Itens da despesa</h3>
                <div className="expense-items-list">
                  {expenseItems.map((item, index) => (
                    <div className="expense-item-card" key={item.id}>
                      <div className="expense-item-row">
                        <input
                          className="expense-item-name"
                          value={item.name}
                          onChange={(event) => updateExpenseItem(item.id, "name", event.target.value)}
                          placeholder="Descrição do item"
                          aria-label={`Descrição do item ${index + 1}`}
                          maxLength={160}
                        />
                        <div className="currency-input expense-item-amount">
                          <b>R$</b>
                          <input
                            value={item.amount}
                            onChange={(event) => updateExpenseItem(item.id, "amount", maskCurrencyInput(event.target.value))}
                            onBlur={() => updateExpenseItem(item.id, "amount", completeCurrencyInput(item.amount))}
                            placeholder="0,00"
                            inputMode="numeric"
                            aria-label={`Valor do item ${index + 1}`}
                          />
                        </div>
                        <button type="button" className="expense-item-remove" onClick={() => removeExpenseItem(item.id)} aria-label={`Remover item ${index + 1}`}><Icon name="trash" /></button>
                      </div>
                      <button type="button" className="expense-item-recurrence" onClick={() => setEditingItemId((current) => current === item.id ? null : item.id)} aria-expanded={editingItemId === item.id}>
                        {itemRecurrenceSummary(item)}
                      </button>
                      {editingItemId === item.id && (
                        <div className="item-recurrence-editor">
                          <fieldset className="recurrence-choice">
                            <legend>Repetição do item</legend>
                            <div>
                              {([["none", "Não repetir"], ["daily", "Diária"], ["weekly", "Semanal"], ["monthly", "Mensal"]] as [RecurrenceFrequency, string][]).map(([frequency, label]) => (
                                <button type="button" key={frequency} className={item.recurrenceFrequency === frequency ? "active" : ""} onClick={() => updateExpenseItem(item.id, "recurrenceFrequency", frequency)}>{label}</button>
                              ))}
                            </div>
                          </fieldset>
                          {item.recurrenceFrequency !== "none" && (
                            <label className="field recurrence-count"><span>Total de repetições (incluindo esta)</span><input type="number" inputMode="numeric" min="2" max="120" value={item.recurrenceCount} placeholder="0" onChange={(event) => updateExpenseItem(item.id, "recurrenceCount", event.target.value)} /></label>
                          )}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
                <button type="button" className="add-expense-item" onClick={() => setExpenseItems((current) => [...current, createEmptyItem()])}><Icon name="plus" /> Adicionar item</button>
                <div className="expense-items-total"><span>Total da despesa</span><strong>{formatCurrency(calculatedTotal)}</strong></div>
              </section>
              <div className="field-grid field-grid--single">
                <DatePickerInput label="Data" value={date} onChange={setDate} />
              </div>
            </>
          ) : (
            <div className="field-grid">
              <label className="field"><span>Valor</span><div className="currency-input"><b>R$</b><input value={amount} onChange={(event) => setAmount(maskCurrencyInput(event.target.value))} onBlur={() => setAmount(completeCurrencyInput(amount))} placeholder="0,00" inputMode="numeric" /></div></label>
              <DatePickerInput label="Data" value={date} onChange={setDate} />
            </div>
          )}

          {!hasSubexpenses && (
            <>
              <div className="recurrence-fields">
                <fieldset className="recurrence-choice">
                  <legend>Repetição</legend>
                  <div>
                    {([["none", "Não repetir"], ["daily", "Diária"], ["weekly", "Semanal"], ["monthly", "Mensal"]] as [RecurrenceFrequency, string][]).map(([frequency, label]) => (
                      <button type="button" key={frequency} className={recurrenceFrequency === frequency ? "active" : ""} onClick={() => setRecurrenceFrequency(frequency)}>{label}</button>
                    ))}
                  </div>
                </fieldset>
              </div>
              {recurrenceFrequency !== "none" && (
                <label className="field recurrence-count"><span>Total de parcelas (incluindo esta)</span><input type="number" inputMode="numeric" min="2" max="120" value={recurrenceCount} placeholder="0" onChange={(event) => setRecurrenceCount(event.target.value)} /></label>
              )}
            </>
          )}

          {error && <p className="auth-error" role="alert">{error}</p>}
          <button type="submit" className={`primary-button create-transaction-submit${type === "income" ? " create-transaction-submit--income" : ""}`} disabled={!canSave || isSubmitting}><Icon name="plus" /> {isSubmitting ? "Adicionando..." : `Adicionar ${type === "expense" ? "despesa" : "receita"}`}</button>
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
          <time dateTime={transaction.date}>{formatDate(transaction.date)}{transaction.paid && <span> · Pago</span>}</time>
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
  onMonthChange,
  initialPeriod,
  error = "",
  isLoading = false,
}: {
  items: Transaction[];
  onOpen: (transaction: Transaction) => void;
  onCreate: (transaction: NewTransaction) => Promise<void>;
  onDelete: (transaction: Transaction) => Promise<void>;
  onPaid: (transaction: Transaction) => Promise<void>;
  onMonthChange: (year: number, month: number) => void;
  initialPeriod: { year: number; month: number };
  error?: string;
  isLoading?: boolean;
}) {
  const [selectedMonth, setSelectedMonth] = useState(() => {
    return new Date(initialPeriod.year, initialPeriod.month - 1, 1);
  });
  const [isMonthPickerOpen, setIsMonthPickerOpen] = useState(false);
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [pickerYear, setPickerYear] = useState(selectedMonth.getFullYear());
  const [pendingDelete, setPendingDelete] = useState<Transaction | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const [actionError, setActionError] = useState("");
  const periodFormatter = new Intl.DateTimeFormat("pt-BR", {
    month: "long",
    year: "numeric",
  });
  const monthFormatter = new Intl.DateTimeFormat("pt-BR", { month: "long" });
  const monthNames = Array.from({ length: 12 }, (_, month) => (
    new Intl.DateTimeFormat("pt-BR", { month: "long" }).format(new Date(2024, month, 1))
  ));
  const monthLabel = `${monthFormatter.format(selectedMonth)} / ${selectedMonth.getFullYear()}`;
  const previousMonth = new Date(selectedMonth.getFullYear(), selectedMonth.getMonth() - 1, 1);
  const nextMonth = new Date(selectedMonth.getFullYear(), selectedMonth.getMonth() + 1, 1);
  const today = new Date();
  const visibleItems = items.filter((item) => {
    const [year, month] = item.date.split("-").map(Number);
    return year === selectedMonth.getFullYear() && month === selectedMonth.getMonth() + 1;
  });
  const totalIncome = visibleItems.filter((item) => item.income).reduce((total, item) => total + item.value, 0);
  const totalExpenses = visibleItems.filter((item) => !item.income).reduce((total, item) => total + item.value, 0);
  const monthBalance = totalIncome - totalExpenses;
  const defaultDay = selectedMonth.getFullYear() === today.getFullYear() && selectedMonth.getMonth() === today.getMonth() ? today.getDate() : 1;
  const defaultDate = `${selectedMonth.getFullYear()}-${String(selectedMonth.getMonth() + 1).padStart(2, "0")}-${String(defaultDay).padStart(2, "0")}`;

  useEffect(() => {
    onMonthChange(selectedMonth.getFullYear(), selectedMonth.getMonth() + 1);
  }, [onMonthChange, selectedMonth]);

  useEffect(() => {
    if (!isMonthPickerOpen) return;

    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === "Escape") setIsMonthPickerOpen(false);
    }

    window.addEventListener("keydown", closeOnEscape);
    return () => window.removeEventListener("keydown", closeOnEscape);
  }, [isMonthPickerOpen]);

  function changeMonth(offset: number) {
    setSelectedMonth((current) => new Date(current.getFullYear(), current.getMonth() + offset, 1));
  }

  function openMonthPicker() {
    setPickerYear(selectedMonth.getFullYear());
    setIsMonthPickerOpen(true);
  }

  function selectMonth(month: number, year = pickerYear) {
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

  async function confirmDeleteTransaction() {
    if (!pendingDelete || isDeleting) return;
    setIsDeleting(true);
    setActionError("");
    try {
      await onDelete(pendingDelete);
      setPendingDelete(null);
    } catch (requestError) {
      setActionError(messageFromError(requestError));
    } finally {
      setIsDeleting(false);
    }
  }

  return (
    <main className="dashboard-screen">
      <header className="dashboard-header">
        <div className="header-brand" aria-label="AMCash">
          <Image src="/amcash-logo.png" alt="" width={1254} height={1254} priority />
          <span><b>AM</b>Cash</span>
        </div>
        <button type="button" className="header-add-button" disabled={isLoading} onClick={() => setIsCreateOpen(true)} aria-label="Adicionar receita ou despesa"><Icon name="plus" /></button>
      </header>

      <section className="month-selector" aria-label="Período selecionado">
        <button type="button" className="month-arrow" onClick={() => changeMonth(-1)} aria-label={`Ir para ${periodFormatter.format(previousMonth)}`}><Icon name="back" /></button>
        <button type="button" className="month-current" onClick={openMonthPicker} aria-haspopup="dialog" aria-expanded={isMonthPickerOpen}>
          {monthLabel}
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true"><path d="m8 10 4 4 4-4" /></svg>
        </button>
        <button type="button" className="month-arrow" onClick={() => changeMonth(1)} aria-label={`Ir para ${periodFormatter.format(nextMonth)}`}><Icon name="forward" /></button>
      </section>

      {(error || actionError) && <p className="auth-error" role="alert">{error || actionError}</p>}

      <section className={`transactions${isLoading || visibleItems.length === 0 ? " transactions--empty" : ""}`} aria-label={`Movimentações de ${monthLabel}`} aria-busy={isLoading}>
        {isLoading ? (
          <div className="dashboard-loading" role="status">
            <div className="loader" aria-hidden="true" />
            <span>Carregando seus lançamentos</span>
          </div>
        ) : visibleItems.length === 0 ? (
          <div className="empty-transactions">
            <div className="empty-transactions-icon" aria-hidden="true"><Icon name="wallet" /></div>
            <h2>Nenhum lançamento neste mês</h2>
            <p>Suas receitas e despesas aparecerão aqui quando forem adicionadas.</p>
          </div>
        ) : visibleItems.map((transaction) => (
          <TransactionRow
            key={transaction.id}
            transaction={transaction}
            onOpen={onOpen}
            onDelete={setPendingDelete}
            onPaid={markPaid}
          />
        ))}      </section>

      <section className="monthly-summary" aria-label="Resumo do mês">
        <div><span>Receitas</span><strong>{formatCurrency(totalIncome)}</strong></div>
        <div><span>Despesas</span><strong>{formatCurrency(totalExpenses)}</strong></div>
        <div><span>Saldo do mês</span><strong>{monthBalance < 0 ? "- " : ""}{formatCurrency(monthBalance)}</strong></div>
      </section>

      <nav className="bottom-nav" aria-label="Navegação principal">
        <a href="#inicio" className="nav-home" aria-current="page">
          <Icon name="home" />
          <span>Início</span>
        </a>
      </nav>

      {isMonthPickerOpen && (
        <div className="month-picker-backdrop" role="presentation" onMouseDown={() => setIsMonthPickerOpen(false)}>
          <section className="month-picker" role="dialog" aria-modal="true" aria-labelledby="month-picker-title" onMouseDown={(event) => event.stopPropagation()}>
            <div className="month-picker-heading">
              <div>
                <span>PERÍODO</span>
                <h2 id="month-picker-title">Selecionar mês</h2>
              </div>
              <button type="button" className="month-picker-close" onClick={() => setIsMonthPickerOpen(false)} aria-label="Fechar seletor">×</button>
            </div>

            <div className="year-selector" aria-label="Selecionar ano">
              <button type="button" onClick={() => setPickerYear((year) => year - 1)} aria-label={`Ir para ${pickerYear - 1}`}><Icon name="back" /></button>
              <strong aria-live="polite">{pickerYear}</strong>
              <button type="button" onClick={() => setPickerYear((year) => year + 1)} aria-label={`Ir para ${pickerYear + 1}`}><Icon name="forward" /></button>
            </div>

            <div className="month-grid">
              {monthNames.map((monthName, month) => {
                const isSelected = selectedMonth.getFullYear() === pickerYear && selectedMonth.getMonth() === month;
                const isCurrent = today.getFullYear() === pickerYear && today.getMonth() === month;
                return (
                  <button
                    type="button"
                    key={monthName}
                    className={`${isSelected ? "selected" : ""}${isCurrent ? " current" : ""}`}
                    onClick={() => selectMonth(month)}
                    aria-pressed={isSelected}
                  >
                    {monthName}
                    {isCurrent && <span>Hoje</span>}
                  </button>
                );
              })}
            </div>

            <button type="button" className="go-to-current-month" onClick={() => selectMonth(today.getMonth(), today.getFullYear())}>
              Voltar para o mês atual
            </button>
          </section>
        </div>
      )}

      {isCreateOpen && (
        <CreateTransactionSheet
          defaultDate={defaultDate}
          onClose={() => setIsCreateOpen(false)}
          onCreate={async (transaction) => { await onCreate(transaction); setIsCreateOpen(false); }}
        />
      )}

      {pendingDelete && (
        <div className="sheet-backdrop confirm-backdrop" role="presentation" onMouseDown={() => !isDeleting && setPendingDelete(null)}>
          <section className="confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="swipe-delete-title" onMouseDown={(event) => event.stopPropagation()}>
            <div className="confirm-icon"><Icon name="trash" /></div>
            <h2 id="swipe-delete-title">Excluir despesa?</h2>
            <p>{pendingDelete.subexpenses.length > 0
              ? `Esta despesa possui ${pendingDelete.subexpenses.length} ${pendingDelete.subexpenses.length === 1 ? "item" : "itens"}. Todos serão excluídos junto com ela.`
              : "Esta despesa será excluída permanentemente."}</p>
            <div><button type="button" disabled={isDeleting} onClick={() => setPendingDelete(null)}>Cancelar</button><button type="button" disabled={isDeleting} onClick={confirmDeleteTransaction}>{isDeleting ? "Excluindo..." : "Excluir"}</button></div>
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
  onSave: (item: Subexpense) => Promise<void>;
  onDelete: (id: string) => Promise<void>;
}) {
  const [draft, setDraft] = useState(item);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState("");
  const amountValue = parseCurrencyInput(draft.amount);
  const itemRecurrenceIsValid = draft.recurrenceFrequency === "none" || (Number.isInteger(draft.recurrenceCount) && draft.recurrenceCount >= 2 && draft.recurrenceCount <= 120);
  const canSave = draft.name.trim().length > 0 && Number.isFinite(amountValue) && amountValue > 0 && itemRecurrenceIsValid;

  async function save() {
    if (!canSave || isSubmitting) return;
    setIsSubmitting(true);
    setError("");
    try {
      await onSave({ ...draft, name: draft.name.trim(), amount: completeCurrencyInput(draft.amount) });
    } catch (requestError) {
      setError(messageFromError(requestError));
      setIsSubmitting(false);
    }
  }

  async function remove() {
    if (!item.id || isSubmitting) {
      onClose();
      return;
    }
    setIsSubmitting(true);
    setError("");
    try {
      await onDelete(item.id);
    } catch (requestError) {
      setError(messageFromError(requestError));
      setIsSubmitting(false);
    }
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

        <label className="field full-field">
          <span>Descrição</span>
          <input value={draft.name} onChange={(event) => setDraft({ ...draft, name: event.target.value })} placeholder="Descrição do item" />
        </label>
        <div className="field-grid field-grid--single">
          <label className="field">
            <span>Valor</span>
            <div className="currency-input"><b>R$</b><input value={draft.amount} onChange={(event) => setDraft({ ...draft, amount: maskCurrencyInput(event.target.value) })} onBlur={() => setDraft({ ...draft, amount: completeCurrencyInput(draft.amount) })} inputMode="numeric" /></div>
          </label>
        </div>
        <div className="recurrence-fields item-editor-recurrence">
          <fieldset className="recurrence-choice">
            <legend>Repetição do item</legend>
            <div>
              {([["none", "Não repetir"], ["daily", "Diária"], ["weekly", "Semanal"], ["monthly", "Mensal"]] as [RecurrenceFrequency, string][]).map(([frequency, label]) => (
                <button type="button" key={frequency} className={draft.recurrenceFrequency === frequency ? "active" : ""} onClick={() => setDraft({ ...draft, recurrenceFrequency: frequency })}>{label}</button>
              ))}
            </div>
          </fieldset>
        </div>
        {draft.recurrenceFrequency !== "none" && (
          <label className="field recurrence-count"><span>Total de repetições (incluindo esta)</span><input type="number" inputMode="numeric" min="2" max="120" value={draft.recurrenceCount || ""} placeholder="0" onChange={(event) => setDraft({ ...draft, recurrenceCount: Number(event.target.value) })} /></label>
        )}
        <label className="paid-toggle">
          <input type="checkbox" checked={draft.paid} onChange={(event) => setDraft({ ...draft, paid: event.target.checked })} />
          <span>Marcar como paga</span>
        </label>

        {error && <p className="auth-error" role="alert">{error}</p>}
        <div className="sheet-actions">
          <button type="button" className="sub-delete" disabled={isSubmitting} onClick={remove}><Icon name="trash" /> {item.id ? "Excluir" : "Cancelar"}</button>
          <button type="button" className="primary-button" disabled={!canSave || isSubmitting} onClick={save}>{isSubmitting ? "Salvando..." : "Salvar alteração"}</button>
        </div>
      </section>
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
  onDelete: (id: string) => Promise<void>;
  onChanged: () => Promise<void>;
}) {
  const [type, setType] = useState<"expense" | "income">(transaction.income ? "income" : "expense");
  const [name, setName] = useState(transaction.name);
  const [category, setCategory] = useState<ApiEntryCategory | "">(transaction.category);
  const [amount, setAmount] = useState(transaction.value.toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 }));
  const [dueDate, setDueDate] = useState(transaction.date);
  const [recurrenceFrequency, setRecurrenceFrequency] = useState<RecurrenceFrequency>(transaction.recurrenceFrequency);
  const [recurrenceCount, setRecurrenceCount] = useState(String(transaction.recurrenceCount || 1));
  const [subexpenses, setSubexpenses] = useState<Subexpense[]>(transaction.subexpenses);
  const [editing, setEditing] = useState<Subexpense | null>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState("");
  const numericRecurrenceCount = Number(recurrenceCount);
  const recurrenceIsValid = transaction.hasSubexpenses || recurrenceFrequency === "none" || (recurrenceCount !== "" && Number.isInteger(numericRecurrenceCount) && numericRecurrenceCount >= 2 && numericRecurrenceCount <= 120);
  const progress = transaction.totalOccurrences
    ? Math.round((transaction.paidOccurrences / transaction.totalOccurrences) * 100)
    : 0;

  useEffect(() => {
    setAmount(transaction.value.toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 }));
    setSubexpenses(transaction.subexpenses);
  }, [transaction.subexpenses, transaction.value]);

  async function saveDetails() {
    const numericAmount = parseCurrencyInput(amount);
    if (!name.trim() || !category || !dueDate || !Number.isFinite(numericAmount) || numericAmount <= 0 || !recurrenceIsValid || isSaving) return;
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
      });
      await onChanged();
      onBack();
    } catch (requestError) {
      setError(messageFromError(requestError));
    } finally {
      setIsSaving(false);
    }
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
    };
    setEditing(newItem);
  }

  async function saveSubexpense(item: Subexpense) {
    const payload = {
      name: item.name,
      amount: parseCurrencyInput(item.amount),
      installmentDescription: item.installment.trim() || null,
      paid: item.paid,
      recurrenceFrequency: recurrenceToApi[item.recurrenceFrequency],
      recurrenceCount: item.recurrenceFrequency === "none" ? 0 : item.recurrenceCount,
    };
    if (item.id) {
      const updated = mapSubexpense(await financeApi.updateSubexpense(transaction.id, item.id, payload));
      setSubexpenses((current) => current.map((currentItem) => currentItem.id === updated.id ? updated : currentItem));
    } else {
      const created = mapSubexpense(await financeApi.addSubexpense(transaction.id, payload));
      setSubexpenses((current) => [...current, created]);
    }
    setEditing(null);
    await onChanged();
  }

  async function deleteSubexpense(subexpenseId: string) {
    await financeApi.deleteSubexpense(transaction.id, subexpenseId);
    setSubexpenses((current) => current.filter((item) => item.id !== subexpenseId));
    setEditing(null);
    await onChanged();
  }

  async function deleteEntry() {
    if (isSaving) return;
    setIsSaving(true);
    setError("");
    try {
      await onDelete(transaction.id);
    } catch (requestError) {
      setConfirmDelete(false);
      setError(messageFromError(requestError));
      setIsSaving(false);
    }
  }

  return (
    <main className="detail-screen">
      <header className="detail-header">
        <button type="button" onClick={onBack} aria-label="Voltar"><Icon name="back" /></button>
        <div><span>LANÇAMENTO</span><h1>Detalhes</h1></div>
        <button type="button" aria-label="Mais opções"><Icon name="more" /></button>
      </header>

      <div className="detail-content">
        <section className="detail-hero">
          <div className={`detail-icon${type === "income" ? " detail-icon--income" : ""}`}><Icon name={transaction.kind} /></div>
          <div className="detail-hero-copy"><span>{type === "expense" ? "Despesa total" : "Receita total"}</span><strong>{type === "expense" ? "- " : "+ "}R$ {amount}</strong></div>
          <span className={`status-pill ${type === "income" ? "status-pill--income" : ""}`}>{type === "expense" ? "Despesa" : "Receita"}</span>
        </section>

        {transaction.recurrenceFrequency !== "none" && (
          <section className="detail-card progress-card">
            <div className="progress-heading"><div><span>PROGRESSO</span><strong>{transaction.paidOccurrences} de {transaction.totalOccurrences} parcelas pagas</strong></div><b>{progress}%</b></div>
            <div className="progress-track"><span style={{ width: `${progress}%` }} /></div>
            <p>O progresso considera as parcelas desta série marcadas como pagas.</p>
          </section>
        )}

        <section className="detail-card edit-card">
          <div className="section-title"><div><span>DADOS PRINCIPAIS</span><h2>Editar lançamento</h2></div><Icon name="edit" /></div>

          <div className="type-switch" aria-label="Tipo do lançamento">
            <button type="button" className={type === "expense" ? "active expense" : ""} onClick={() => { setType("expense"); if (type !== "expense") setCategory(""); }}>Despesa</button>
            <button type="button" className={type === "income" ? "active income" : ""} onClick={() => { setType("income"); if (type !== "income") setCategory(""); }}>Receita</button>
          </div>

          <label className="field full-field"><span>Descrição</span><input value={name} onChange={(event) => setName(event.target.value)} /></label>
          <CategoryPickerInput value={category} options={type === "expense" ? expenseCategories : incomeCategories} onChange={setCategory} />
          <div className="field-grid">
            <label className="field"><span>Valor</span><div className="currency-input"><b>R$</b><input value={amount} onChange={(event) => setAmount(maskCurrencyInput(event.target.value))} onBlur={() => setAmount(completeCurrencyInput(amount))} inputMode="numeric" /></div></label>
            <DatePickerInput label="Vencimento" value={dueDate} onChange={setDueDate} />
          </div>
          {!transaction.hasSubexpenses && (
            <>
              <div className="recurrence-fields">
                <fieldset className="recurrence-choice">
                  <legend>Repetição</legend>
                  <div>
                    {([['none', 'Não repetir'], ['daily', 'Diária'], ['weekly', 'Semanal'], ['monthly', 'Mensal']] as [RecurrenceFrequency, string][]).map(([frequency, label]) => (
                      <button type="button" key={frequency} className={recurrenceFrequency === frequency ? "active" : ""} onClick={() => setRecurrenceFrequency(frequency)}>{label}</button>
                    ))}
                  </div>
                </fieldset>
              </div>
              {recurrenceFrequency !== "none" && <label className="field recurrence-count"><span>Total de parcelas (incluindo esta)</span><input type="number" inputMode="numeric" min="2" max="120" value={recurrenceCount} placeholder="0" onChange={(event) => setRecurrenceCount(event.target.value)} /></label>}
            </>
          )}
        </section>

        {transaction.hasSubexpenses && type === "expense" && (
          <section className="detail-card subexpenses-card">
            <div className="section-title subexpense-title">
              <div><span>COMPOSIÇÃO</span><h2>Itens da despesa <b>{subexpenses.length}</b></h2></div>
              <button type="button" onClick={addSubexpense}><Icon name="plus" /> Adicionar</button>
            </div>
            <div className="subexpense-list">
              {subexpenses.map((item) => (
                <button type="button" className="subexpense-row" key={item.id} onClick={() => setEditing(item)}>
                  <span className={`sub-check${item.paid ? " checked" : ""}`}>{item.paid && <Icon name="check" />}</span>
                  <span className="sub-copy"><strong>{item.name}</strong><small>{item.installment}</small></span>
                  <span className="sub-value">R$ {item.amount}</span>
                  <span className="sub-edit"><Icon name="edit" /></span>
                </button>
              ))}
            </div>
          </section>
        )}

        {error && <p className="auth-error" role="alert">{error}</p>}
        <button type="button" className="primary-button save-details" disabled={isSaving} onClick={saveDetails}>{isSaving ? "Salvando..." : "Salvar alterações"}</button>
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

      {confirmDelete && (
        <div className="sheet-backdrop confirm-backdrop" role="presentation" onMouseDown={() => setConfirmDelete(false)}>
          <section className="confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="delete-title" onMouseDown={(event) => event.stopPropagation()}>
            <div className="confirm-icon"><Icon name="trash" /></div>
            <h2 id="delete-title">Excluir lançamento?</h2>
            <p>{subexpenses.length > 0 ? `Esta despesa possui ${subexpenses.length} ${subexpenses.length === 1 ? "item" : "itens"}. Todos serão excluídos junto com ela.` : "Esta despesa será excluída permanentemente."}</p>
            <div><button type="button" disabled={isSaving} onClick={() => setConfirmDelete(false)}>Cancelar</button><button type="button" disabled={isSaving} onClick={deleteEntry}>{isSaving ? "Excluindo..." : "Excluir"}</button></div>
          </section>
        </div>
      )}
    </main>
  );
}

export default function Home() {
  const [screen, setScreen] = useState<"checking" | "auth" | "loading" | "refreshing" | "app">("checking");
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

  const logout = useCallback(() => {
    window.localStorage.removeItem("amcash.accessToken");
    window.localStorage.removeItem("amcash.user");
    setItems([]);
    setSelected(null);
    setError("");
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
    const sessionCheck = window.setTimeout(() => {
      const savedToken = window.localStorage.getItem("amcash.accessToken");
      setScreen(savedToken ? "refreshing" : "auth");
    }, 0);
    return () => window.clearTimeout(sessionCheck);
  }, []);

  useEffect(() => {
    const backgroundColor = screen === "auth" ? "#f8faff" : "#ffffff";
    document.documentElement.style.backgroundColor = backgroundColor;
    document.body.style.backgroundColor = backgroundColor;
    document.querySelector<HTMLMetaElement>('meta[name="theme-color"]')?.setAttribute("content", backgroundColor);
  }, [screen]);

  useEffect(() => {
    window.addEventListener("amcash:unauthorized", logout);
    return () => window.removeEventListener("amcash:unauthorized", logout);
  }, [logout]);

  useEffect(() => {
    if (screen !== "loading" && screen !== "refreshing") return;
    let active = true;
    const minimumLoading = screen === "loading" ? 1400 : 0;
    const startLoading = window.setTimeout(() => {
      Promise.all([
        loadMonth(period.year, period.month),
        new Promise((resolve) => window.setTimeout(resolve, minimumLoading)),
      ]).then(() => {
        if (active) setScreen((current) => current === "auth" ? "auth" : "app");
      });
    }, 0);
    return () => {
      active = false;
      window.clearTimeout(startLoading);
    };
  }, [loadMonth, period.month, period.year, screen]);

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
  }

  async function deleteTransaction(entryId: string) {
    await financeApi.delete(entryId);
    setSelected(null);
    await loadMonth(period.year, period.month);
  }

  async function deleteDashboardTransaction(transaction: Transaction) {
    await deleteTransaction(transaction.id);
  }

  async function markTransactionPaid(transaction: Transaction) {
    await financeApi.setPaid(transaction.id, !transaction.paid);
    await loadMonth(period.year, period.month);
  }

  async function refreshCurrentMonth() {
    await loadMonth(period.year, period.month);
  }

  const idleCreate = async () => {};
  const idleTransactionAction = async (_transaction: Transaction) => {};
  const idleMonthChange = () => {};

  if (screen === "checking" || screen === "refreshing") return <Dashboard items={items} onOpen={setSelected} onCreate={idleCreate} onDelete={idleTransactionAction} onPaid={idleTransactionAction} onMonthChange={idleMonthChange} initialPeriod={period} isLoading />;
  if (screen === "auth") return <AuthScreen onAuthenticated={() => setScreen("loading")} />;
  if (screen === "loading") return <LoadingScreen />;
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

  return <Dashboard items={items} onOpen={setSelected} onCreate={addTransaction} onDelete={deleteDashboardTransaction} onPaid={markTransactionPaid} onMonthChange={changeMonth} initialPeriod={period} error={error} isLoading={isLoading} />;
}
