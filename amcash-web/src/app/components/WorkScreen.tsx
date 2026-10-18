"use client";

import Image from "next/image";
import { ReactNode, useCallback, useEffect, useRef, useState } from "react";
import { ApiWorkCost, ApiWorkCostCategory, ApiWorkDay, ApiWorkOverview, ApiWorkPeriod, financeApi } from "@/lib/api";

type EarningDraft = { source: string; amount: string };
type WorkDraft = { date: string; kilometers: string; uber: string; ninetyNine: string; additional: EarningDraft[] };
type CostDraft = { id?: string; date: string; category: ApiWorkCostCategory; description: string; amount: string };

const periodLabels: Record<ApiWorkPeriod, string> = { DAY: "Dia", WEEK: "Semana", MONTH: "Mês" };
const costLabels: Record<ApiWorkCostCategory, string> = {
  FUEL: "Combustível", MAINTENANCE: "Manutenção", PARKING: "Estacionamento",
  TOLL: "Pedágio", WASH: "Lavagem", OTHER: "Outro",
};

function isoDate(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}
function parseDate(value: string) { return new Date(`${value}T12:00:00`); }
function currency(value: number) { return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(value); }
function decimal(value: number) { return new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 2 }).format(value); }
function inputMoney(value: string) {
  const digits = value.replace(/\D/g, "");
  if (!digits) return "";
  const padded = digits.padStart(3, "0");
  const integer = (padded.slice(0, -2).replace(/^0+(?=\d)/, "") || "0").replace(/\B(?=(\d{3})+(?!\d))/g, ".");
  return `${integer},${padded.slice(-2)}`;
}
function numberFromInput(value: string) { return value.trim() ? Number(value.replace(/\./g, "").replace(",", ".")) || 0 : 0; }
function numberInput(value: string) {
  const cleaned = value.replace(/[^\d,.]/g, "").replace(".", ",");
  const [integer = "", ...decimals] = cleaned.split(",");
  return decimals.length ? `${integer},${decimals.join("").slice(0, 2)}` : integer;
}
function moneyForInput(value: number) { return value ? inputMoney(String(Math.round(value * 100))) : ""; }
function emptyDraft(date: string): WorkDraft { return { date, kilometers: "", uber: "", ninetyNine: "", additional: [] }; }
function emptyCost(date: string): CostDraft { return { date, category: "FUEL", description: "", amount: "" }; }
function messageFrom(error: unknown) { return error instanceof Error ? error.message : "Não foi possível concluir a operação."; }
function periodTitle(overview: ApiWorkOverview | null, period: ApiWorkPeriod, referenceDate: string) {
  if (!overview) return "";
  const formatter = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "short", year: "numeric" });
  if (period === "DAY") return formatter.format(parseDate(referenceDate));
  if (period === "WEEK") return `${formatter.format(parseDate(overview.rangeStart))} – ${formatter.format(parseDate(overview.rangeEnd))}`;
  return new Intl.DateTimeFormat("pt-BR", { month: "long", year: "numeric" }).format(parseDate(referenceDate));
}

export default function WorkScreen({ navigation, initialAction = null }: {
  navigation: ReactNode;
  initialAction?: "work" | "cost" | null;
}) {
  const [period, setPeriod] = useState<ApiWorkPeriod>("DAY");
  const [referenceDate, setReferenceDate] = useState(() => isoDate(new Date()));
  const [overview, setOverview] = useState<ApiWorkOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [draft, setDraft] = useState<WorkDraft | null>(() => initialAction === "work" ? emptyDraft(isoDate(new Date())) : null);
  const [costDraft, setCostDraft] = useState<CostDraft | null>(() => initialAction === "cost" ? emptyCost(isoDate(new Date())) : null);
  const [newRecordOpen, setNewRecordOpen] = useState(false);
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const requestId = useRef(0);

  const load = useCallback(async () => {
    const currentRequest = ++requestId.current;
    setLoading(true); setError("");
    try {
      const response = await financeApi.workOverview(referenceDate, period);
      if (currentRequest === requestId.current) setOverview(response);
    } catch (requestError) {
      if (currentRequest === requestId.current) setError(messageFrom(requestError));
    } finally { if (currentRequest === requestId.current) setLoading(false); }
  }, [period, referenceDate]);

  useEffect(() => { const timer = window.setTimeout(() => void load(), 0); return () => window.clearTimeout(timer); }, [load]);

  function movePeriod(direction: number) {
    const date = parseDate(referenceDate);
    if (period === "DAY") date.setDate(date.getDate() + direction);
    if (period === "WEEK") date.setDate(date.getDate() + 7 * direction);
    if (period === "MONTH") date.setMonth(date.getMonth() + direction, 1);
    setReferenceDate(isoDate(date));
  }
  function openNew() { setNewRecordOpen(false); setEditing(false); setDraft(emptyDraft(referenceDate)); }
  function openDay(day: ApiWorkDay) {
    const uber = day.earnings.find((earning) => earning.source.toLowerCase() === "uber");
    const ninetyNine = day.earnings.find((earning) => earning.source === "99");
    setEditing(true);
    setDraft({ date: day.date, kilometers: day.kilometers ? String(day.kilometers).replace(".", ",") : "",
      uber: moneyForInput(Number(uber?.amount ?? 0)), ninetyNine: moneyForInput(Number(ninetyNine?.amount ?? 0)),
      additional: day.earnings.filter((earning) => earning !== uber && earning !== ninetyNine)
        .map((earning) => ({ source: earning.source, amount: moneyForInput(Number(earning.amount)) })) });
  }
  function openCost(cost?: ApiWorkCost) {
    setNewRecordOpen(false);
    setCostDraft(cost ? { id: cost.id, date: cost.date, category: cost.category,
      description: cost.description ?? "", amount: moneyForInput(Number(cost.amount)) } : emptyCost(referenceDate));
  }

  async function saveDay() {
    if (!draft || saving) return;
    const additional = draft.additional.filter((item) => item.source.trim() || item.amount);
    if (additional.some((item) => !item.source.trim())) { setError("Informe o nome de cada fonte adicional."); return; }
    setSaving(true); setError("");
    try {
      await financeApi.saveWorkDay(draft.date, { kilometers: numberFromInput(draft.kilometers), earnings: [
        { source: "Uber", amount: numberFromInput(draft.uber) }, { source: "99", amount: numberFromInput(draft.ninetyNine) },
        ...additional.map((item) => ({ source: item.source.trim(), amount: numberFromInput(item.amount) })),
      ] });
      setReferenceDate(draft.date); setDraft(null); await load();
    } catch (requestError) { setError(messageFrom(requestError)); } finally { setSaving(false); }
  }
  async function saveCost() {
    if (!costDraft || saving) return;
    if (numberFromInput(costDraft.amount) <= 0) { setError("Informe um valor de custo maior que zero."); return; }
    setSaving(true); setError("");
    const payload = { date: costDraft.date, category: costDraft.category,
      description: costDraft.description.trim(), amount: numberFromInput(costDraft.amount) };
    try {
      if (costDraft.id) await financeApi.updateWorkCost(costDraft.id, payload);
      else await financeApi.createWorkCost(payload);
      setReferenceDate(costDraft.date); setCostDraft(null); await load();
    } catch (requestError) { setError(messageFrom(requestError)); } finally { setSaving(false); }
  }
  async function deleteDay() {
    if (!draft || saving || !window.confirm("Excluir o registro deste dia?")) return;
    setSaving(true); try { await financeApi.deleteWorkDay(draft.date); setDraft(null); await load(); }
    catch (requestError) { setError(messageFrom(requestError)); } finally { setSaving(false); }
  }
  async function deleteCost() {
    if (!costDraft?.id || saving || !window.confirm("Excluir este custo?")) return;
    setSaving(true); try { await financeApi.deleteWorkCost(costDraft.id); setCostDraft(null); await load(); }
    catch (requestError) { setError(messageFrom(requestError)); } finally { setSaving(false); }
  }

  const summary = overview?.summary;
  return <main className="work-screen">
    <header className="dashboard-header work-header"><div className="header-brand" aria-label="AMCash">
      <Image src="/amcash-logo.png" alt="" width={1254} height={1254} priority /><span><b>AM</b>Cash</span>
    </div><span className="work-header-label">TRABALHO</span></header>
    <div className="work-content">
      <section className="work-heading"><div><span>SEU RESULTADO</span><h1>Quanto seu trabalho rendeu?</h1></div>
        <div className="work-new-record">
          <button type="button" className="work-new-record-trigger" aria-expanded={newRecordOpen} onClick={() => setNewRecordOpen((open) => !open)}><span>+</span> Novo registro</button>
          {newRecordOpen && <><div className="work-record-dismiss" role="presentation" onMouseDown={() => setNewRecordOpen(false)} /><div className="work-new-record-menu" role="menu">
            <button type="button" role="menuitem" onClick={openNew}><span className="work-record-icon">↗</span><span><strong>Trabalho do dia</strong><small>Quilômetros e faturamento</small></span></button>
            <button type="button" role="menuitem" onClick={() => openCost()}><span className="work-record-icon cost">−</span><span><strong>Custo do trabalho</strong><small>Combustível e outros gastos</small></span></button>
          </div></>}
        </div></section>
      <div className="work-period-tabs" role="group" aria-label="Período do resumo">{(Object.keys(periodLabels) as ApiWorkPeriod[]).map((option) =>
        <button key={option} type="button" className={period === option ? "selected" : ""} onClick={() => setPeriod(option)}>{periodLabels[option]}</button>)}</div>
      <div className="work-period-selector"><button type="button" onClick={() => movePeriod(-1)} aria-label="Período anterior">‹</button>
        <strong>{periodTitle(overview, period, referenceDate)}</strong><button type="button" onClick={() => movePeriod(1)} aria-label="Próximo período">›</button></div>
      {error && <p className="work-error" role="alert">{error}</p>}
      {loading ? <div className="work-loading"><div className="loader" /><span>Calculando seus resultados</span></div> : <>
        <section className="work-net-card"><span>LÍQUIDO NO PERÍODO</span><strong className={(summary?.netEarnings ?? 0) < 0 ? "negative" : ""}>{currency(Number(summary?.netEarnings ?? 0))}</strong>
          <small>Faturamento menos os custos registrados</small></section>
        <section className="work-metrics"><article><span>Faturamento</span><strong>{currency(Number(summary?.grossEarnings ?? 0))}</strong></article>
          <article><span>Custos</span><strong className="cost">{currency(Number(summary?.totalCosts ?? 0))}</strong></article>
          <article><span>Quilômetros</span><strong>{decimal(Number(summary?.kilometers ?? 0))} km</strong></article>
          <article><span>Por km</span><strong>{currency(Number(summary?.earningsPerKilometer ?? 0))}</strong></article></section>
        <section className="work-days-section"><div className="work-section-title"><div><span>FATURAMENTO</span><h2>Dias trabalhados</h2></div><b>{summary?.workedDays ?? 0}</b></div>
          {!overview?.days.length ? <div className="work-empty"><strong>Nenhum dia registrado</strong><span>Registre os quilômetros e o faturamento do seu trabalho.</span></div> : overview.days.map((day) =>
            <button type="button" className="work-day-row" key={day.id} onClick={() => openDay(day)}><DateBadge date={day.date} />
              <span className="work-day-main"><strong>{currency(Number(day.grossEarnings))} faturados</strong><small>{decimal(Number(day.kilometers))} km · {currency(Number(day.earningsPerKilometer))}/km</small></span>
              <span className="work-day-values"><b>{currency(Number(day.grossEarnings))}</b></span><span aria-hidden="true">›</span></button>)}</section>
        <section className="work-days-section work-costs-section"><div className="work-section-title"><div><span>GASTOS DO TRABALHO</span><h2>Custos do período</h2></div><b>{overview?.costs.length ?? 0}</b></div>
          {!overview?.costs.length ? <div className="work-empty"><strong>Nenhum custo registrado</strong><span>Abastecimentos e outros gastos podem ser lançados na data em que acontecerem.</span></div> : overview.costs.map((cost) =>
            <button type="button" className="work-day-row work-cost-row" key={cost.id} onClick={() => openCost(cost)}><DateBadge date={cost.date} />
              <span className="work-day-main"><strong>{cost.description || costLabels[cost.category]}</strong><small>{costLabels[cost.category]}</small></span>
              <span className="work-day-values"><b>- {currency(Number(cost.amount))}</b></span><span aria-hidden="true">›</span></button>)}</section>
      </>}
    </div>{navigation}

    {draft && <ModalBackdrop onClose={() => !saving && setDraft(null)}><section className="work-sheet" role="dialog" aria-modal="true" aria-labelledby="work-sheet-title" onMouseDown={(event) => event.stopPropagation()}>
      <SheetHeader eyebrow="TRABALHO DO DIA" title={editing ? "Editar resultado" : "Registrar resultado"} id="work-sheet-title" onClose={() => setDraft(null)} />
      <div className="work-form-scroll"><label className="work-field"><span>Data</span><input type="date" value={draft.date} disabled={editing} onChange={(event) => setDraft({ ...draft, date: event.target.value })} /></label>
        <label className="work-field"><span>Quilômetros rodados</span><div className="work-input-suffix"><input inputMode="decimal" placeholder="0" value={draft.kilometers} onChange={(event) => setDraft({ ...draft, kilometers: numberInput(event.target.value) })} /><b>km</b></div></label>
        <div className="work-form-section"><span>FATURAMENTO</span><h3>Quanto você faturou?</h3></div><div className="work-form-grid">
          <MoneyField label="Uber" value={draft.uber} onChange={(value) => setDraft({ ...draft, uber: value })} /><MoneyField label="99" value={draft.ninetyNine} onChange={(value) => setDraft({ ...draft, ninetyNine: value })} /></div>
        {draft.additional.map((item, index) => <div className="work-additional-row" key={index}><label className="work-field"><span>Fonte adicional</span><input placeholder="Ex.: particular" value={item.source} onChange={(event) => setDraft({ ...draft, additional: draft.additional.map((current, i) => i === index ? { ...current, source: event.target.value } : current) })} /></label>
          <MoneyField label="Valor" value={item.amount} onChange={(value) => setDraft({ ...draft, additional: draft.additional.map((current, i) => i === index ? { ...current, amount: value } : current) })} />
          <button type="button" onClick={() => setDraft({ ...draft, additional: draft.additional.filter((_, i) => i !== index) })} aria-label="Remover fonte">×</button></div>)}
        <button type="button" className="work-add-source" onClick={() => setDraft({ ...draft, additional: [...draft.additional, { source: "", amount: "" }] })}>+ Adicionar outra fonte</button></div>
      <footer>{editing && <button type="button" className="work-delete" disabled={saving} onClick={deleteDay}>Excluir</button>}<button type="button" className="work-save" disabled={saving || !draft.date} onClick={saveDay}>{saving ? "Salvando..." : "Salvar resultado"}</button></footer>
    </section></ModalBackdrop>}

    {costDraft && <ModalBackdrop onClose={() => !saving && setCostDraft(null)}><section className="work-sheet work-cost-sheet" role="dialog" aria-modal="true" aria-labelledby="cost-sheet-title" onMouseDown={(event) => event.stopPropagation()}>
      <SheetHeader eyebrow="CUSTO DO TRABALHO" title={costDraft.id ? "Editar custo" : "Registrar custo"} id="cost-sheet-title" onClose={() => setCostDraft(null)} />
      <div className="work-form-scroll"><label className="work-field"><span>Data</span><input type="date" value={costDraft.date} onChange={(event) => setCostDraft({ ...costDraft, date: event.target.value })} /></label>
        <label className="work-field"><span>Categoria</span><select value={costDraft.category} onChange={(event) => setCostDraft({ ...costDraft, category: event.target.value as ApiWorkCostCategory })}>{(Object.keys(costLabels) as ApiWorkCostCategory[]).map((category) => <option key={category} value={category}>{costLabels[category]}</option>)}</select></label>
        <label className="work-field"><span>Descrição (opcional)</span><input maxLength={160} placeholder="Ex.: abastecimento do tanque" value={costDraft.description} onChange={(event) => setCostDraft({ ...costDraft, description: event.target.value })} /></label>
        <MoneyField label="Valor" value={costDraft.amount} onChange={(value) => setCostDraft({ ...costDraft, amount: value })} /></div>
      <footer>{costDraft.id && <button type="button" className="work-delete" disabled={saving} onClick={deleteCost}>Excluir</button>}<button type="button" className="work-save" disabled={saving || !costDraft.date || !costDraft.amount} onClick={saveCost}>{saving ? "Salvando..." : "Salvar custo"}</button></footer>
    </section></ModalBackdrop>}
  </main>;
}

function DateBadge({ date }: { date: string }) { return <span className="work-day-date"><b>{String(parseDate(date).getDate()).padStart(2, "0")}</b><small>{new Intl.DateTimeFormat("pt-BR", { weekday: "short" }).format(parseDate(date)).replace(".", "")}</small></span>; }
function MoneyField({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) { return <label className="work-field"><span>{label}</span><div className="work-money-input"><b>R$</b><input inputMode="numeric" placeholder="0,00" value={value} onChange={(event) => onChange(inputMoney(event.target.value))} /></div></label>; }
function ModalBackdrop({ children, onClose }: { children: ReactNode; onClose: () => void }) { return <div className="work-sheet-backdrop" role="presentation" onMouseDown={onClose}>{children}</div>; }
function SheetHeader({ eyebrow, title, id, onClose }: { eyebrow: string; title: string; id: string; onClose: () => void }) { return <><div className="work-sheet-handle" /><header><div><span>{eyebrow}</span><h2 id={id}>{title}</h2></div><button type="button" onClick={onClose} aria-label="Fechar">×</button></header></>; }
