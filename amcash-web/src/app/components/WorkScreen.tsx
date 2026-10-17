"use client";

import Image from "next/image";
import { ReactNode, useCallback, useEffect, useRef, useState } from "react";
import { ApiWorkDay, ApiWorkOverview, ApiWorkPeriod, financeApi } from "@/lib/api";

type EarningDraft = { source: string; amount: string };

type WorkDraft = {
  date: string;
  kilometers: string;
  fuelCost: string;
  otherCosts: string;
  uber: string;
  ninetyNine: string;
  additional: EarningDraft[];
};

const periodLabels: Record<ApiWorkPeriod, string> = {
  DAY: "Dia",
  WEEK: "Semana",
  MONTH: "Mês",
};

function isoDate(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function parseDate(value: string) {
  return new Date(`${value}T12:00:00`);
}

function currency(value: number) {
  return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(value);
}

function decimal(value: number) {
  return new Intl.NumberFormat("pt-BR", { minimumFractionDigits: 0, maximumFractionDigits: 2 }).format(value);
}

function inputMoney(value: string) {
  const digits = value.replace(/\D/g, "");
  if (!digits) return "";
  const padded = digits.padStart(3, "0");
  const integer = (padded.slice(0, -2).replace(/^0+(?=\d)/, "") || "0")
    .replace(/\B(?=(\d{3})+(?!\d))/g, ".");
  return `${integer},${padded.slice(-2)}`;
}

function numberFromInput(value: string) {
  if (!value.trim()) return 0;
  return Number(value.replace(/\./g, "").replace(",", ".")) || 0;
}

function numberInput(value: string) {
  const cleaned = value.replace(/[^\d,.]/g, "").replace(".", ",");
  const [integer = "", ...decimals] = cleaned.split(",");
  return decimals.length ? `${integer},${decimals.join("").slice(0, 2)}` : integer;
}

function emptyDraft(date = isoDate(new Date())): WorkDraft {
  return { date, kilometers: "", fuelCost: "", otherCosts: "", uber: "", ninetyNine: "", additional: [] };
}

function moneyForInput(value: number) {
  return value ? inputMoney(String(Math.round(value * 100))) : "";
}

function messageFrom(error: unknown) {
  return error instanceof Error ? error.message : "Não foi possível concluir a operação.";
}

function periodTitle(overview: ApiWorkOverview | null, period: ApiWorkPeriod, referenceDate: string) {
  if (!overview) return "";
  const formatter = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "short", year: "numeric" });
  if (period === "DAY") return formatter.format(parseDate(referenceDate));
  if (period === "WEEK") {
    return `${formatter.format(parseDate(overview.rangeStart))} – ${formatter.format(parseDate(overview.rangeEnd))}`;
  }
  return new Intl.DateTimeFormat("pt-BR", { month: "long", year: "numeric" }).format(parseDate(referenceDate));
}

export default function WorkScreen({ navigation }: { navigation: ReactNode }) {
  const [period, setPeriod] = useState<ApiWorkPeriod>("MONTH");
  const [referenceDate, setReferenceDate] = useState(() => isoDate(new Date()));
  const [overview, setOverview] = useState<ApiWorkOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [draft, setDraft] = useState<WorkDraft | null>(null);
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const requestId = useRef(0);

  const load = useCallback(async () => {
    const currentRequest = ++requestId.current;
    setLoading(true);
    setError("");
    try {
      const response = await financeApi.workOverview(referenceDate, period);
      if (currentRequest === requestId.current) setOverview(response);
    } catch (requestError) {
      if (currentRequest === requestId.current) setError(messageFrom(requestError));
    } finally {
      if (currentRequest === requestId.current) setLoading(false);
    }
  }, [period, referenceDate]);

  useEffect(() => {
    const loadTimer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(loadTimer);
  }, [load]);

  function movePeriod(direction: number) {
    const date = parseDate(referenceDate);
    if (period === "DAY") date.setDate(date.getDate() + direction);
    if (period === "WEEK") date.setDate(date.getDate() + (7 * direction));
    if (period === "MONTH") date.setMonth(date.getMonth() + direction, 1);
    setReferenceDate(isoDate(date));
  }

  function openNew() {
    setEditing(false);
    setDraft(emptyDraft(referenceDate));
  }

  function openDay(day: ApiWorkDay) {
    const uber = day.earnings.find((earning) => earning.source.toLowerCase() === "uber");
    const ninetyNine = day.earnings.find((earning) => earning.source === "99");
    const additional = day.earnings
      .filter((earning) => earning !== uber && earning !== ninetyNine)
      .map((earning) => ({ source: earning.source, amount: moneyForInput(Number(earning.amount)) }));
    setEditing(true);
    setDraft({
      date: day.date,
      kilometers: day.kilometers ? String(day.kilometers).replace(".", ",") : "",
      fuelCost: moneyForInput(Number(day.fuelCost)),
      otherCosts: moneyForInput(Number(day.otherCosts)),
      uber: moneyForInput(Number(uber?.amount ?? 0)),
      ninetyNine: moneyForInput(Number(ninetyNine?.amount ?? 0)),
      additional,
    });
  }

  async function saveDay() {
    if (!draft || saving) return;
    const additional = draft.additional.filter((item) => item.source.trim() || item.amount);
    if (additional.some((item) => !item.source.trim())) {
      setError("Informe o nome de cada fonte adicional.");
      return;
    }
    setSaving(true);
    setError("");
    try {
      await financeApi.saveWorkDay(draft.date, {
        kilometers: numberFromInput(draft.kilometers),
        fuelCost: numberFromInput(draft.fuelCost),
        otherCosts: numberFromInput(draft.otherCosts),
        earnings: [
          { source: "Uber", amount: numberFromInput(draft.uber) },
          { source: "99", amount: numberFromInput(draft.ninetyNine) },
          ...additional.map((item) => ({ source: item.source.trim(), amount: numberFromInput(item.amount) })),
        ],
      });
      setReferenceDate(draft.date);
      setDraft(null);
      await load();
    } catch (requestError) {
      setError(messageFrom(requestError));
    } finally {
      setSaving(false);
    }
  }

  async function deleteDay() {
    if (!draft || saving || !window.confirm("Excluir o registro deste dia?")) return;
    setSaving(true);
    try {
      await financeApi.deleteWorkDay(draft.date);
      setDraft(null);
      await load();
    } catch (requestError) {
      setError(messageFrom(requestError));
    } finally {
      setSaving(false);
    }
  }

  const summary = overview?.summary;

  return (
    <main className="work-screen">
      <header className="dashboard-header work-header">
        <div className="header-brand" aria-label="AMCash">
          <Image src="/amcash-logo.png" alt="" width={1254} height={1254} priority />
          <span><b>AM</b>Cash</span>
        </div>
        <span className="work-header-label">TRABALHO</span>
      </header>

      <div className="work-content">
        <section className="work-heading">
          <div><span>SEU RESULTADO</span><h1>Quanto seu trabalho rendeu?</h1></div>
          <button type="button" onClick={openNew}><span>+</span> Registrar dia</button>
        </section>

        <div className="work-period-tabs" role="group" aria-label="Período do resumo">
          {(Object.keys(periodLabels) as ApiWorkPeriod[]).map((option) => (
            <button key={option} type="button" className={period === option ? "selected" : ""} onClick={() => setPeriod(option)}>{periodLabels[option]}</button>
          ))}
        </div>

        <div className="work-period-selector">
          <button type="button" onClick={() => movePeriod(-1)} aria-label="Período anterior">‹</button>
          <strong>{periodTitle(overview, period, referenceDate)}</strong>
          <button type="button" onClick={() => movePeriod(1)} aria-label="Próximo período">›</button>
        </div>

        {error && <p className="work-error" role="alert">{error}</p>}

        {loading ? (
          <div className="work-loading"><div className="loader" /><span>Calculando seus resultados</span></div>
        ) : (
          <>
            <section className="work-net-card">
              <span>LÍQUIDO NO PERÍODO</span>
              <strong className={(summary?.netEarnings ?? 0) < 0 ? "negative" : ""}>{currency(Number(summary?.netEarnings ?? 0))}</strong>
              <small>Faturamento menos combustível e outros custos</small>
            </section>

            <section className="work-metrics">
              <article><span>Faturamento</span><strong>{currency(Number(summary?.grossEarnings ?? 0))}</strong></article>
              <article><span>Custos</span><strong className="cost">{currency(Number(summary?.totalCosts ?? 0))}</strong></article>
              <article><span>Quilômetros</span><strong>{decimal(Number(summary?.kilometers ?? 0))} km</strong></article>
              <article><span>Por km</span><strong>{currency(Number(summary?.earningsPerKilometer ?? 0))}</strong></article>
            </section>

            <section className="work-days-section">
              <div className="work-section-title"><div><span>HISTÓRICO</span><h2>Dias trabalhados</h2></div><b>{summary?.workedDays ?? 0}</b></div>
              {!overview?.days.length ? (
                <div className="work-empty"><strong>Nenhum dia registrado</strong><span>Registre seu faturamento e seus custos para acompanhar o resultado.</span></div>
              ) : overview.days.map((day) => (
                <button type="button" className="work-day-row" key={day.id} onClick={() => openDay(day)}>
                  <span className="work-day-date"><b>{String(parseDate(day.date).getDate()).padStart(2, "0")}</b><small>{new Intl.DateTimeFormat("pt-BR", { weekday: "short" }).format(parseDate(day.date)).replace(".", "")}</small></span>
                  <span className="work-day-main"><strong>{currency(Number(day.netEarnings))} líquidos</strong><small>{decimal(Number(day.kilometers))} km · {currency(Number(day.earningsPerKilometer))}/km</small></span>
                  <span className="work-day-values"><b>{currency(Number(day.grossEarnings))}</b><small>- {currency(Number(day.totalCosts))}</small></span>
                  <span aria-hidden="true">›</span>
                </button>
              ))}
            </section>
          </>
        )}
      </div>

      {navigation}

      {draft && (
        <div className="work-sheet-backdrop" role="presentation" onMouseDown={() => !saving && setDraft(null)}>
          <section className="work-sheet" role="dialog" aria-modal="true" aria-labelledby="work-sheet-title" onMouseDown={(event) => event.stopPropagation()}>
            <div className="work-sheet-handle" />
            <header><div><span>TRABALHO DO DIA</span><h2 id="work-sheet-title">{editing ? "Editar resultado" : "Registrar resultado"}</h2></div><button type="button" onClick={() => setDraft(null)} aria-label="Fechar">×</button></header>

            <div className="work-form-scroll">
              <label className="work-field"><span>Data</span><input type="date" value={draft.date} disabled={editing} onChange={(event) => setDraft({ ...draft, date: event.target.value })} /></label>
              <label className="work-field"><span>Quilômetros rodados</span><div className="work-input-suffix"><input inputMode="decimal" placeholder="0" value={draft.kilometers} onChange={(event) => setDraft({ ...draft, kilometers: numberInput(event.target.value) })} /><b>km</b></div></label>

              <div className="work-form-section"><span>FATURAMENTO</span><h3>Quanto você faturou?</h3></div>
              <div className="work-form-grid">
                <label className="work-field"><span>Uber</span><div className="work-money-input"><b>R$</b><input inputMode="numeric" placeholder="0,00" value={draft.uber} onChange={(event) => setDraft({ ...draft, uber: inputMoney(event.target.value) })} /></div></label>
                <label className="work-field"><span>99</span><div className="work-money-input"><b>R$</b><input inputMode="numeric" placeholder="0,00" value={draft.ninetyNine} onChange={(event) => setDraft({ ...draft, ninetyNine: inputMoney(event.target.value) })} /></div></label>
              </div>

              {draft.additional.map((item, index) => (
                <div className="work-additional-row" key={index}>
                  <label className="work-field"><span>Fonte adicional</span><input placeholder="Ex.: particular" value={item.source} onChange={(event) => setDraft({ ...draft, additional: draft.additional.map((current, currentIndex) => currentIndex === index ? { ...current, source: event.target.value } : current) })} /></label>
                  <label className="work-field"><span>Valor</span><div className="work-money-input"><b>R$</b><input inputMode="numeric" placeholder="0,00" value={item.amount} onChange={(event) => setDraft({ ...draft, additional: draft.additional.map((current, currentIndex) => currentIndex === index ? { ...current, amount: inputMoney(event.target.value) } : current) })} /></div></label>
                  <button type="button" onClick={() => setDraft({ ...draft, additional: draft.additional.filter((_, currentIndex) => currentIndex !== index) })} aria-label="Remover fonte">×</button>
                </div>
              ))}
              <button type="button" className="work-add-source" onClick={() => setDraft({ ...draft, additional: [...draft.additional, { source: "", amount: "" }] })}>+ Adicionar outra fonte</button>

              <div className="work-form-section"><span>CUSTOS</span><h3>Quanto você gastou?</h3></div>
              <div className="work-form-grid">
                <label className="work-field"><span>Combustível</span><div className="work-money-input"><b>R$</b><input inputMode="numeric" placeholder="0,00" value={draft.fuelCost} onChange={(event) => setDraft({ ...draft, fuelCost: inputMoney(event.target.value) })} /></div></label>
                <label className="work-field"><span>Outros custos</span><div className="work-money-input"><b>R$</b><input inputMode="numeric" placeholder="0,00" value={draft.otherCosts} onChange={(event) => setDraft({ ...draft, otherCosts: inputMoney(event.target.value) })} /></div></label>
              </div>
            </div>

            <footer>
              {editing && <button type="button" className="work-delete" disabled={saving} onClick={deleteDay}>Excluir</button>}
              <button type="button" className="work-save" disabled={saving || !draft.date} onClick={saveDay}>{saving ? "Salvando..." : "Salvar resultado"}</button>
            </footer>
          </section>
        </div>
      )}
    </main>
  );
}
