"use client";

import Image from "next/image";
import { useEffect, useState } from "react";

type Transaction = {
  id: number;
  name: string;
  date: string;
  amount: string;
  balance: string;
  kind: "food" | "card" | "home" | "shield" | "phone" | "wifi" | "wallet";
  income?: boolean;
};

type Subexpense = {
  id: number;
  name: string;
  amount: string;
  installment: string;
  paid: boolean;
};

const transactions: Transaction[] = [
  { id: 1, name: "Flávio", date: "01/10/2026", amount: "- R$ 1.097,00", balance: "- R$ 1.097,00", kind: "food" },
  { id: 2, name: "Supermais - 5/12", date: "08/10/2026", amount: "- R$ 277,90", balance: "- R$ 1.374,90", kind: "food" },
  { id: 3, name: "Nubank", date: "08/10/2026", amount: "- R$ 332,00", balance: "- R$ 1.706,90", kind: "card" },
  { id: 4, name: "Mãe", date: "10/10/2026", amount: "- R$ 3.650,00", balance: "- R$ 5.356,90", kind: "home" },
  { id: 5, name: "Seguro", date: "11/10/2026", amount: "- R$ 306,22", balance: "- R$ 5.663,12", kind: "shield" },
  { id: 6, name: "Neon - 1/3", date: "11/10/2026", amount: "- R$ 227,78", balance: "- R$ 5.890,90", kind: "card" },
  { id: 7, name: "Neon", date: "15/10/2026", amount: "- R$ 676,00", balance: "- R$ 6.566,90", kind: "card" },
  { id: 8, name: "Renner", date: "15/10/2026", amount: "- R$ 511,00", balance: "- R$ 7.077,90", kind: "food" },
  { id: 9, name: "Claro", date: "17/10/2026", amount: "- R$ 100,00", balance: "- R$ 7.177,90", kind: "phone" },
  { id: 10, name: "Nubank - 2/10", date: "23/10/2026", amount: "- R$ 229,00", balance: "- R$ 7.406,90", kind: "card" },
  { id: 11, name: "Zaffari - 2/3", date: "24/10/2026", amount: "- R$ 100,00", balance: "- R$ 7.506,90", kind: "food" },
  { id: 12, name: "Net", date: "25/10/2026", amount: "- R$ 100,00", balance: "- R$ 7.606,90", kind: "wifi" },
  { id: 13, name: "Salário", date: "31/10/2026", amount: "+ R$ 8.500,00", balance: "+ R$ 893,10", kind: "wallet", income: true },
];

const initialSubexpenses: Subexpense[] = [
  { id: 1, name: "Notebook", amount: "189,90", installment: "5 de 12", paid: true },
  { id: 2, name: "Farmácia", amount: "86,40", installment: "2 de 3", paid: true },
  { id: 3, name: "Spotify", amount: "21,90", installment: "Mensal", paid: true },
  { id: 4, name: "iCloud", amount: "33,80", installment: "Mensal", paid: false },
];

type IconName = Transaction["kind"] | "search" | "more" | "back" | "home" | "edit" | "trash" | "plus" | "check";

function Icon({ name }: { name: IconName }) {
  if (name === "more") {
    return <svg viewBox="0 0 24 24"><circle cx="12" cy="5" r="1.6" /><circle cx="12" cy="12" r="1.6" /><circle cx="12" cy="19" r="1.6" /></svg>;
  }

  const paths = {
    search: <><circle cx="10.8" cy="10.8" r="6.2" /><path d="m15.4 15.4 4.2 4.2" /></>,
    back: <path d="m15 18-6-6 6-6" />,
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

function Dashboard({
  items,
  onOpen,
}: {
  items: Transaction[];
  onOpen: (transaction: Transaction) => void;
}) {
  return (
    <main className="dashboard-screen">
      <header className="dashboard-header">
        <div className="header-brand" aria-label="AMCash">
          <Image src="/amcash-logo.png" alt="" width={1254} height={1254} priority />
          <span><b>AM</b>Cash</span>
        </div>
        <div className="header-actions">
          <button type="button" aria-label="Buscar"><Icon name="search" /></button>
          <button type="button" aria-label="Mais opções"><Icon name="more" /></button>
        </div>
      </header>

      <section className="month-selector" aria-label="Período selecionado">
        <button type="button" aria-label="Mês anterior"><Icon name="back" /></button>
        <h1>outubro de 2026</h1>
        <span aria-hidden="true" />
      </section>

      <section className="transactions" aria-label="Movimentações de outubro">
        {items.map((transaction) => (
          <button
            type="button"
            className="transaction"
            key={transaction.id}
            onClick={() => onOpen(transaction)}
            aria-label={`Abrir detalhes de ${transaction.name}`}
          >
            <div className={`transaction-icon${transaction.income ? " transaction-icon--income" : ""}`}>
              <Icon name={transaction.kind} />
            </div>
            <div className="transaction-main">
              <strong>{transaction.name}</strong>
              <time dateTime={transaction.date.split("/").reverse().join("-")}>{transaction.date}</time>
            </div>
            <div className="transaction-values">
              <strong className={transaction.income ? "value-income" : "value-expense"}>{transaction.amount}</strong>
              <small>Saldo: {transaction.balance}</small>
            </div>
          </button>
        ))}
      </section>

      <section className="monthly-summary" aria-label="Resumo do mês">
        <div><span>Receitas</span><strong>R$ 8.500,00</strong></div>
        <div><span>Despesas</span><strong>- R$ 7.606,90</strong></div>
        <div><span>Saldo do mês</span><strong>R$ 893,10</strong></div>
      </section>

      <nav className="bottom-nav" aria-label="Navegação principal">
        <a href="#inicio" className="nav-home" aria-current="page">
          <Icon name="home" />
          <span>Início</span>
        </a>
      </nav>
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
  onSave: (item: Subexpense) => void;
  onDelete: (id: number) => void;
}) {
  const [draft, setDraft] = useState(item);

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
            <span>Subdespesa</span>
            <h2 id="subexpense-title">Editar lançamento</h2>
          </div>
          <button type="button" className="sheet-close" onClick={onClose} aria-label="Fechar">×</button>
        </div>

        <label className="field full-field">
          <span>Descrição</span>
          <input value={draft.name} onChange={(event) => setDraft({ ...draft, name: event.target.value })} />
        </label>
        <div className="field-grid">
          <label className="field">
            <span>Valor</span>
            <div className="currency-input"><b>R$</b><input value={draft.amount} onChange={(event) => setDraft({ ...draft, amount: event.target.value })} /></div>
          </label>
          <label className="field">
            <span>Parcela</span>
            <input value={draft.installment} onChange={(event) => setDraft({ ...draft, installment: event.target.value })} />
          </label>
        </div>
        <label className="paid-toggle">
          <input type="checkbox" checked={draft.paid} onChange={(event) => setDraft({ ...draft, paid: event.target.checked })} />
          <span>Marcar como paga</span>
        </label>

        <div className="sheet-actions">
          <button type="button" className="sub-delete" onClick={() => onDelete(item.id)}><Icon name="trash" /> Excluir</button>
          <button type="button" className="primary-button" onClick={() => onSave(draft)}>Salvar alteração</button>
        </div>
      </section>
    </div>
  );
}

function DetailScreen({
  transaction,
  onBack,
  onDelete,
}: {
  transaction: Transaction;
  onBack: () => void;
  onDelete: (id: number) => void;
}) {
  const [type, setType] = useState<"expense" | "income">(transaction.income ? "income" : "expense");
  const [name, setName] = useState(transaction.name);
  const [amount, setAmount] = useState(transaction.amount.replace(/[+-]\s?R\$\s?/, ""));
  const [dueDate, setDueDate] = useState(transaction.date.split("/").reverse().join("-"));
  const [repeat, setRepeat] = useState("Mensal · 10 parcelas");
  const [subexpenses, setSubexpenses] = useState(initialSubexpenses);
  const [editing, setEditing] = useState<Subexpense | null>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [saved, setSaved] = useState(false);
  const completed = subexpenses.filter((item) => item.paid).length;
  const progress = subexpenses.length ? Math.round((completed / subexpenses.length) * 100) : 0;

  function saveDetails() {
    setSaved(true);
    window.setTimeout(() => setSaved(false), 1800);
  }

  function addSubexpense() {
    const newItem: Subexpense = {
      id: Date.now(),
      name: "Nova subdespesa",
      amount: "0,00",
      installment: "1 de 1",
      paid: false,
    };
    setSubexpenses((current) => [...current, newItem]);
    setEditing(newItem);
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

        <section className="detail-card progress-card">
          <div className="progress-heading"><div><span>PROGRESSO</span><strong>{completed} de {subexpenses.length} concluídas</strong></div><b>{progress}%</b></div>
          <div className="progress-track"><span style={{ width: `${progress}%` }} /></div>
          <p>As subdespesas pagas completam a despesa principal.</p>
        </section>

        <section className="detail-card edit-card">
          <div className="section-title"><div><span>DADOS PRINCIPAIS</span><h2>Editar lançamento</h2></div><Icon name="edit" /></div>

          <div className="type-switch" aria-label="Tipo do lançamento">
            <button type="button" className={type === "expense" ? "active expense" : ""} onClick={() => setType("expense")}>Despesa</button>
            <button type="button" className={type === "income" ? "active income" : ""} onClick={() => setType("income")}>Receita</button>
          </div>

          <label className="field full-field"><span>Descrição</span><input value={name} onChange={(event) => setName(event.target.value)} /></label>
          <div className="field-grid">
            <label className="field"><span>Valor</span><div className="currency-input"><b>R$</b><input value={amount} onChange={(event) => setAmount(event.target.value)} inputMode="decimal" /></div></label>
            <label className="field"><span>Vencimento</span><input type="date" value={dueDate} onChange={(event) => setDueDate(event.target.value)} /></label>
          </div>
          <label className="field full-field"><span>Repetição</span><select value={repeat} onChange={(event) => setRepeat(event.target.value)}><option>Não repetir</option><option>Mensal</option><option>Mensal · 3 parcelas</option><option>Mensal · 10 parcelas</option><option>Mensal · 12 parcelas</option></select></label>
        </section>

        <section className="detail-card subexpenses-card">
          <div className="section-title subexpense-title">
            <div><span>COMPOSIÇÃO</span><h2>Subdespesas <b>{subexpenses.length}</b></h2></div>
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

        <button type="button" className="primary-button save-details" onClick={saveDetails}>{saved ? <><Icon name="check" /> Alterações salvas</> : "Salvar alterações"}</button>
        <button type="button" className="danger-button" onClick={() => setConfirmDelete(true)}><Icon name="trash" /> Excluir lançamento</button>
      </div>

      {editing && (
        <SubexpenseEditor
          item={editing}
          onClose={() => setEditing(null)}
          onSave={(updated) => { setSubexpenses((current) => current.map((item) => item.id === updated.id ? updated : item)); setEditing(null); }}
          onDelete={(id) => { setSubexpenses((current) => current.filter((item) => item.id !== id)); setEditing(null); }}
        />
      )}

      {confirmDelete && (
        <div className="sheet-backdrop confirm-backdrop" role="presentation" onMouseDown={() => setConfirmDelete(false)}>
          <section className="confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="delete-title" onMouseDown={(event) => event.stopPropagation()}>
            <div className="confirm-icon"><Icon name="trash" /></div>
            <h2 id="delete-title">Excluir lançamento?</h2>
            <p>A despesa e todas as suas subdespesas serão removidas.</p>
            <div><button type="button" onClick={() => setConfirmDelete(false)}>Cancelar</button><button type="button" onClick={() => onDelete(transaction.id)}>Excluir</button></div>
          </section>
        </div>
      )}
    </main>
  );
}

export default function Home() {
  const [isLoading, setIsLoading] = useState(true);
  const [items, setItems] = useState(transactions);
  const [selected, setSelected] = useState<Transaction | null>(null);

  useEffect(() => {
    const timer = window.setTimeout(() => setIsLoading(false), 1800);
    return () => window.clearTimeout(timer);
  }, []);

  if (isLoading) return <LoadingScreen />;
  if (selected) {
    return (
      <DetailScreen
        transaction={selected}
        onBack={() => setSelected(null)}
        onDelete={(id) => { setItems((current) => current.filter((item) => item.id !== id)); setSelected(null); }}
      />
    );
  }

  return <Dashboard items={items} onOpen={setSelected} />;
}
