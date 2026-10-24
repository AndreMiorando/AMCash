"use client";

import Image from "next/image";
import { ReactNode, useCallback, useEffect, useRef, useState } from "react";
import { ApiVehicle, ApiWorkCost, ApiWorkCostCategory, ApiWorkDay, ApiWorkOverview, ApiWorkPeriod, financeApi } from "@/lib/api";
import AppHeader from "@/app/components/AppHeader";
import { CostCategoryGrid, CostCategoryIcon, workCostCategoryLabels } from "@/app/components/CostCategoryGrid";
import DatePickerInput, { dateToInputValue } from "@/app/components/DatePickerInput";
import MonthPickerInput from "@/app/components/MonthPickerInput";
import Button from "@/app/components/ui/Button";
import { InputField } from "@/app/components/ui/Input";
import SegmentedControl from "@/app/components/ui/SegmentedControl";
import { KpiLabel, KpiValue, PrimaryKpiCard, SecondaryKpiGroup, SecondaryKpiItem } from "@/app/components/ui/Kpi";

type EarningDraft = { source: string; amount: string };
type CostDraft = { key: string; id?: string; category: ApiWorkCostCategory; description: string; amount: string; homeManaged?: boolean };
type WorkDraft = {
  date: string;
  vehicleId: string | null;
  kilometers: string;
  uber: string;
  ninetyNine: string;
  additional: EarningDraft[];
  costs: CostDraft[];
  originalCostIds: string[];
  hadWorkDay: boolean;
};

const homeCostPrefix = "HOME: ";
const legacyHomeCostDescription = "Custo do dia — Home";

const periodLabels: Record<ApiWorkPeriod, string> = { DAY: "Dia", WEEK: "Semana", MONTH: "Mês" };
const periodOptions = (Object.keys(periodLabels) as ApiWorkPeriod[]).map((value) => ({ value, label: periodLabels[value] }));
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
function isHomeCost(cost: ApiWorkCost) { return cost.description === legacyHomeCostDescription || cost.description?.startsWith(homeCostPrefix); }
function costDescription(cost: ApiWorkCost) {
  if (cost.description?.startsWith(homeCostPrefix)) {
    const description = cost.description.slice(homeCostPrefix.length);
    return description === workCostCategoryLabels[cost.category] ? "" : description;
  }
  if (cost.description === legacyHomeCostDescription) return "";
  return cost.description || "";
}
function earningPriority(source: string) {
  const normalized = source.trim().toLowerCase();
  if (normalized === "uber") return 0;
  if (normalized === "99") return 1;
  return 2;
}
function orderedEarnings(day: ApiWorkDay | undefined) {
  return day?.earnings
    .map((earning, index) => ({ earning, index }))
    .sort((first, second) => earningPriority(first.earning.source) - earningPriority(second.earning.source) || first.index - second.index)
    .map(({ earning }) => earning) ?? [];
}
function messageFrom(error: unknown) { return error instanceof Error ? error.message : "Não foi possível concluir a operação."; }
function periodTitle(overview: ApiWorkOverview | null, period: ApiWorkPeriod, referenceDate: string) {
  if (!overview) return "";
  const formatter = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "short", year: "numeric" });
  if (period === "DAY") return formatter.format(parseDate(referenceDate));
  if (period === "WEEK") return `${formatter.format(parseDate(overview.rangeStart))} – ${formatter.format(parseDate(overview.rangeEnd))}`;
  return new Intl.DateTimeFormat("pt-BR", { month: "long", year: "numeric" }).format(parseDate(referenceDate));
}

export default function WorkScreen({ navigation, onReady }: {
  navigation: (loading: boolean) => ReactNode;
  onReady?: () => void;
}) {
  const [period, setPeriod] = useState<ApiWorkPeriod>("WEEK");
  const [referenceDate, setReferenceDate] = useState(() => dateToInputValue(new Date()));
  const [overview, setOverview] = useState<ApiWorkOverview | null>(null);
  const [vehicles,setVehicles]=useState<ApiVehicle[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [draft, setDraft] = useState<WorkDraft | null>(null);
  const [draftMode, setDraftMode] = useState<"create" | "edit">("edit");
  const [checkingDate, setCheckingDate] = useState(false);
  const [editingCostKey, setEditingCostKey] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const requestId = useRef(0);
  const workDateRequestId = useRef(0);
  const nextCostKey = useRef(0);

  const load = useCallback(async () => {
    const currentRequest = ++requestId.current;
    setLoading(true); setError("");
    try {
      const [response,vehicleList] = await Promise.all([financeApi.workOverview(referenceDate, period),financeApi.vehicles().catch(()=>[] as ApiVehicle[])]);
      if (currentRequest === requestId.current){setOverview(response);setVehicles(vehicleList);}
    } catch (requestError) {
      if (currentRequest === requestId.current) setError(messageFrom(requestError));
    } finally { if (currentRequest === requestId.current) setLoading(false); }
  }, [period, referenceDate]);

  useEffect(() => { const timer = window.setTimeout(() => void load(), 0); return () => window.clearTimeout(timer); }, [load]);
  useEffect(() => { if (!loading) onReady?.(); }, [loading, onReady]);

  function movePeriod(direction: number) {
    const date = parseDate(referenceDate);
    if (period === "DAY") date.setDate(date.getDate() + direction);
    if (period === "WEEK") date.setDate(date.getDate() + 7 * direction);
    if (period === "MONTH") date.setMonth(date.getMonth() + direction, 1);
    setReferenceDate(dateToInputValue(date));
  }
  function openRecord(date: string, day: ApiWorkDay | undefined, costs: ApiWorkCost[]) {
    const uber = day?.earnings.find((earning) => earning.source.toLowerCase() === "uber");
    const ninetyNine = day?.earnings.find((earning) => earning.source === "99");
    setDraft({ date,vehicleId:day?.vehicleId??vehicles.find(vehicle=>vehicle.inUse)?.id??null, kilometers: day?.kilometers ? String(day.kilometers).replace(".", ",") : "",
      uber: moneyForInput(Number(uber?.amount ?? 0)), ninetyNine: moneyForInput(Number(ninetyNine?.amount ?? 0)),
      additional: day?.earnings.filter((earning) => earning !== uber && earning !== ninetyNine)
        .map((earning) => ({ source: earning.source, amount: moneyForInput(Number(earning.amount)) })) ?? [],
      costs: costs.map((cost) => ({ key: cost.id, id: cost.id, category: cost.category,
        description: costDescription(cost), amount: moneyForInput(Number(cost.amount)), homeManaged: isHomeCost(cost) })),
      originalCostIds: costs.map((cost) => cost.id), hadWorkDay: Boolean(day) });
    setEditingCostKey(null);
    setDraftMode("edit");
    setError("");
  }

  function emptyDraft(date: string): WorkDraft {
    return { date,vehicleId:vehicles.find(vehicle=>vehicle.inUse)?.id??null, kilometers: "", uber: "", ninetyNine: "", additional: [], costs: [], originalCostIds: [], hadWorkDay: false };
  }

  async function selectWorkDate(date: string) {
    const currentRequest = ++workDateRequestId.current;
    setCheckingDate(true);
    setError("");
    try {
      const response = await financeApi.workOverview(date, "DAY");
      const day = response.days.find((item) => item.date === date);
      if (currentRequest !== workDateRequestId.current) return;
      const costs = response.costs.filter((cost) => cost.date === date);
      if (day || costs.length) {
        openRecord(date, day, costs);
      } else {
        setDraft(emptyDraft(date));
        setDraftMode("create");
        setEditingCostKey(null);
      }
    } catch (requestError) {
      if (currentRequest === workDateRequestId.current) setError(messageFrom(requestError));
    } finally { if (currentRequest === workDateRequestId.current) setCheckingDate(false); }
  }

  function openCreateRecord() {
    if (checkingDate) return;
    void selectWorkDate(dateToInputValue(new Date()));
  }

  async function saveRecord() {
    if (!draft || saving) return;
    const additional = draft.additional.filter((item) => item.source.trim() || item.amount);
    if (additional.some((item) => !item.source.trim())) { setError("Informe o nome de cada fonte adicional."); return; }
    const validCosts = draft.costs.filter((cost) => numberFromInput(cost.amount) > 0);
    const retainedCostIds = new Set(validCosts.flatMap((cost) => cost.id ? [cost.id] : []));
    const hasWorkValues = numberFromInput(draft.kilometers) > 0 || numberFromInput(draft.uber) > 0
      || numberFromInput(draft.ninetyNine) > 0 || additional.some((item) => numberFromInput(item.amount) > 0);
    if (draftMode === "create" && !hasWorkValues && !validCosts.length) { setError("Informe ao menos um dado do trabalho antes de registrar."); return; }
    setSaving(true); setError("");
    try {
      if (draftMode === "create") {
        const selectedDay = await financeApi.workOverview(draft.date, "DAY");
        const day = selectedDay.days.find((item) => item.date === draft.date);
        if (day) {
          openRecord(draft.date, day, selectedDay.costs.filter((cost) => cost.date === draft.date));
          setError("Os dados existentes desta data foram carregados para edição.");
          return;
        }
      }
      const operations: Promise<unknown>[] = [];
      if (draftMode === "create" || draft.hadWorkDay || hasWorkValues) operations.push(financeApi.saveWorkDay(draft.date, {
        kilometers: numberFromInput(draft.kilometers),
        vehicleId:draft.vehicleId,
        earnings: [
          { source: "Uber", amount: numberFromInput(draft.uber) },
          { source: "99", amount: numberFromInput(draft.ninetyNine) },
          ...additional.map((item) => ({ source: item.source.trim(), amount: numberFromInput(item.amount) })),
        ],
      }));
      operations.push(...validCosts.map((cost) => {
        const description = cost.description.trim();
        const payload = { date: draft.date, category: cost.category,
          description: cost.homeManaged ? `${homeCostPrefix}${description}` : description,
          amount: numberFromInput(cost.amount) };
        return cost.id ? financeApi.updateWorkCost(cost.id, payload) : financeApi.createWorkCost(payload);
      }));
      operations.push(...draft.originalCostIds.filter((id) => !retainedCostIds.has(id))
        .map((id) => financeApi.deleteWorkCost(id)));
      await Promise.all(operations);
      const refreshed = await financeApi.workOverview(draft.date, period);
      setReferenceDate(draft.date); setOverview(refreshed); setDraft(null);
    } catch (requestError) { setError(messageFrom(requestError)); } finally { setSaving(false); }
  }
  async function deleteRecord() {
    if (!draft || saving || !window.confirm("Excluir o trabalho e todos os custos deste dia?")) return;
    setSaving(true); try {
      await Promise.all([
        ...(draft.hadWorkDay ? [financeApi.deleteWorkDay(draft.date)] : []),
        ...draft.originalCostIds.map((id) => financeApi.deleteWorkCost(id)),
      ]);
      setDraft(null); await load();
    } catch (requestError) { setError(messageFrom(requestError)); } finally { setSaving(false); }
  }

  const summary = overview?.summary;
  const comparison = overview?.comparison;
  const comparisonChange = Number(comparison?.changeAmount ?? 0);
  const comparisonTone = comparisonChange > 0 ? "positive" : comparisonChange < 0 ? "negative" : "neutral";
  const comparisonValue = comparison?.changePercentage == null
    ? Number(summary?.netEarnings ?? 0) === 0 ? "0%" : "Novo"
    : `${Number(comparison.changePercentage) > 0 ? "+" : ""}${new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 1 }).format(Number(comparison.changePercentage))}%`;
  const previousPeriodLabel = period === "DAY" ? "dia anterior" : period === "WEEK" ? "semana anterior" : "mês anterior";
  const records = overview ? [...new Set([
    ...overview.days.map((day) => day.date),
    ...overview.costs.map((cost) => cost.date),
  ])].sort((first, second) => second.localeCompare(first)).map((date) => ({
    date,
    day: overview.days.find((day) => day.date === date),
    costs: overview.costs.filter((cost) => cost.date === date),
  })) : [];
  const draftGross = draft ? numberFromInput(draft.uber) + numberFromInput(draft.ninetyNine)
    + draft.additional.reduce((total, earning) => total + numberFromInput(earning.amount), 0) : 0;
  const draftCosts = draft?.costs.reduce((total, cost) => total + numberFromInput(cost.amount), 0) ?? 0;
  const draftNet = draftGross - draftCosts;
  return <main className="work-screen">
    <AppHeader className="work-header"><div className="work-header-actions"><span className="work-header-label">TRABALHO</span><button type="button" className="header-add-button work-register-button" onClick={openCreateRecord} disabled={checkingDate} aria-label={checkingDate ? "Verificando registro" : "Registrar dia"} title={checkingDate ? "Verificando registro" : "Registrar dia"}>{checkingDate ? <span className="work-register-loader" aria-hidden="true" /> : <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><rect x="3.5" y="5" width="17" height="15" rx="2" /><path d="M8 3v4M16 3v4M3.5 9.5h17M12 13v5M9.5 15.5h5" /></svg>}</button></div></AppHeader>
    <div className="work-content">
      {error && !draft && <p className="work-error" role="alert">{error}</p>}
      {overview && <>
        <PrimaryKpiCard className="work-net-card">
          <div className="work-net-main"><KpiLabel>LÍQUIDO NO PERÍODO</KpiLabel><KpiValue className={(summary?.netEarnings ?? 0) < 0 ? "negative" : ""}>{currency(Number(summary?.netEarnings ?? 0))}</KpiValue></div>
          <div className={`work-net-comparison ${comparisonTone}`} aria-label={`${comparisonValue} em relação ao ${previousPeriodLabel}`} title={`Líquido do ${previousPeriodLabel}: ${currency(Number(comparison?.netEarnings ?? 0))}`}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true"><path d="M5 20v-6M12 20V9M19 20V4" /></svg>
            <strong>{comparisonValue}</strong>
          </div>
        </PrimaryKpiCard>
        <SecondaryKpiGroup className="work-metrics">
          <SecondaryKpiItem><span className="work-metric-icon revenue" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="6" width="18" height="12" rx="2" /><path d="M7 10h10M7 14h6" /></svg></span><span className="work-metric-copy"><KpiLabel>Faturamento</KpiLabel><KpiValue>{currency(Number(summary?.grossEarnings ?? 0))}</KpiValue></span></SecondaryKpiItem>
          <SecondaryKpiItem><span className="work-metric-icon expense" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="5" width="18" height="14" rx="2" /><path d="M3 9h18M7 14h5" /></svg></span><span className="work-metric-copy"><KpiLabel>Custos</KpiLabel><KpiValue className="cost">{currency(Number(summary?.totalCosts ?? 0))}</KpiValue></span></SecondaryKpiItem>
          <SecondaryKpiItem><span className="work-metric-icon" aria-hidden="true"><Image src="/road-icon.png" alt="" width={1254} height={1254} /></span><span className="work-metric-copy"><KpiLabel>Quilômetros</KpiLabel><KpiValue>{decimal(Number(summary?.kilometers ?? 0))}</KpiValue></span></SecondaryKpiItem>
          <SecondaryKpiItem><span className="work-metric-icon" aria-hidden="true"><Image src="/earnings-per-km-icon.png" alt="" width={1254} height={1254} /></span><span className="work-metric-copy"><KpiLabel>Por km</KpiLabel><KpiValue>{currency(Number(summary?.earningsPerKilometer ?? 0))}</KpiValue></span></SecondaryKpiItem>
        </SecondaryKpiGroup>
      </>}
      <SegmentedControl className="work-period-tabs" ariaLabel="Período do resumo" value={period} options={periodOptions} onChange={setPeriod} />
      <div className="work-period-selector"><button type="button" onClick={() => movePeriod(-1)} aria-label="Período anterior">‹</button>
        {period === "DAY"
          ? <div className="work-period-date-picker"><DatePickerInput label="Selecionar dia do histórico" value={referenceDate} displayValue={periodTitle(overview, period, referenceDate)} onChange={setReferenceDate} /></div>
          : period === "MONTH"
            ? <div className="work-period-date-picker"><MonthPickerInput label="Selecionar mês do histórico" value={referenceDate} displayValue={periodTitle(overview, period, referenceDate)} onChange={setReferenceDate} /></div>
            : <strong>{periodTitle(overview, period, referenceDate)}</strong>}
        <button type="button" onClick={() => movePeriod(1)} aria-label="Próximo período">›</button></div>
      {loading ? null : <>
        <section className="work-days-section work-records-section"><div className="work-section-title"><div><span>HISTÓRICO</span><h2>Registros do período</h2></div><b>{records.length}</b></div>
          {!records.length ? <div className="work-empty"><strong>Nenhum registro no período</strong><span>Os resultados e custos atualizados pela Home aparecerão aqui.</span></div> : records.map((record) => {
            const gross = Number(record.day?.grossEarnings ?? 0);
            const costs = record.costs.reduce((total, cost) => total + Number(cost.amount), 0);
            const net = gross - costs;
            const earnings = orderedEarnings(record.day);
            return <button type="button" className="work-day-row" key={record.date} onClick={() => openRecord(record.date, record.day, record.costs)}>
              <span className="work-day-overview">
                <span className="work-day-summary-half left"><DateBadge date={record.date} />
                  <span className="work-day-summary-item"><span><b>{decimal(Number(record.day?.kilometers ?? 0))}</b><small>km</small></span></span>
                  <span className="work-day-summary-item"><span><b>{currency(costs)}</b><small>custos</small></span></span>
                </span>
                <span className="work-day-summary-half right">
                  <span className="work-day-summary-item"><span><b>{currency(gross)}</b><small>bruto</small></span></span>
                  <span className={`work-day-summary-item net${net < 0 ? " negative" : ""}`}><span><b>{net < 0 ? "- " : ""}{currency(Math.abs(net))}</b><small>líquido</small></span></span>
                  <span className="work-day-chevron" aria-hidden="true">›</span>
                </span>
              </span>
              {earnings.length > 0 && <span className="work-day-earnings" aria-label="Faturamento por plataforma">
                {earnings.map((earning) => {
                  const source = earning.source.trim();
                  const normalizedSource = source.toLowerCase();
                  const isUber = normalizedSource === "uber";
                  const isNinetyNine = normalizedSource === "99";
                  return <span className="work-day-earning" key={earning.id}>
                    <span className="work-day-earning-source">
                      {isUber && <><Image className="work-day-platform-logo uber" src="/uber-logo.jpeg" alt="" width={96} height={96} /><span className="sr-only">Uber</span></>}
                      {isNinetyNine && <><Image className="work-day-platform-logo ninety-nine" src="/99-logo.png" alt="" width={96} height={96} /><span className="sr-only">99</span></>}
                      {!isUber && !isNinetyNine && <><span className="work-day-source-mark" aria-hidden="true">{source.charAt(0).toUpperCase() || "+"}</span><span className="work-day-source-name" title={source}>{source}</span></>}
                    </span>
                    <b>{currency(Number(earning.amount))}</b>
                  </span>;
                })}
              </span>}
            </button>;
          })}</section>
      </>}
    </div>{navigation(loading)}

    {draft && <section className="work-record-screen" aria-labelledby="work-record-title">
      <header className="work-record-page-header"><button type="button" onClick={() => { if (!saving) setDraft(null); }} aria-label="Voltar"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="m15 18-6-6 6-6" /></svg></button><h2 id="work-record-title">{draftMode === "create" ? "Registrar trabalho" : "Editar trabalho"}</h2><span aria-hidden="true" /></header>
      <div className="work-form-scroll">
        {error && <p className="work-error" role="alert">{error}</p>}
        <div className="work-record-date-field"><DatePickerInput label="Data do trabalho" value={draft.date} max={dateToInputValue(new Date())} onChange={selectWorkDate} />{draft.vehicleId&&<small className="work-record-active-vehicle">Veículo em uso: {vehicles.find(vehicle=>vehicle.id===draft.vehicleId)?.nickname||vehicles.find(vehicle=>vehicle.id===draft.vehicleId)?.model}</small>}</div>
        <label className="work-record-kilometers"><span>Quilômetros rodados</span><div><span className="work-record-road-icon"><Image src="/road-icon.png" alt="" width={1254} height={1254} /></span><span className="work-record-kilometers-input"><b>km</b><input inputMode="decimal" aria-label="Quilômetros rodados" placeholder="0" value={draft.kilometers} onChange={(event) => setDraft({ ...draft, kilometers: numberInput(event.target.value) })} /></span></div></label>
        <div className="work-record-section-title"><h3>Faturamento</h3></div><div className="work-form-grid">
          <BrandedMoneyField label="Uber" logo="/uber-logo.jpeg" value={draft.uber} onChange={(value) => setDraft({ ...draft, uber: value })} /><BrandedMoneyField label="99" logo="/99-logo.png" value={draft.ninetyNine} onChange={(value) => setDraft({ ...draft, ninetyNine: value })} /></div>
        {draft.additional.map((item, index) => <div className="work-additional-row" key={index}><InputField fieldClassName="work-field" label="Fonte adicional" maxLength={30} placeholder="Ex.: particular" value={item.source} onChange={(event) => setDraft({ ...draft, additional: draft.additional.map((current, i) => i === index ? { ...current, source: event.target.value } : current) })} />
          <MoneyField label="Valor" value={item.amount} onChange={(value) => setDraft({ ...draft, additional: draft.additional.map((current, i) => i === index ? { ...current, amount: value } : current) })} />
          <button type="button" onClick={() => setDraft({ ...draft, additional: draft.additional.filter((_, i) => i !== index) })} aria-label="Remover fonte"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6M10 10v6M14 10v6" /></svg></button></div>)}
        <button type="button" className="work-add-source" onClick={() => setDraft({ ...draft, additional: [...draft.additional, { source: "", amount: "" }] })}>+ Adicionar outra fonte</button>

        <div className="work-record-section-title"><h3>Custos do dia</h3></div>
        <div className="work-record-costs">{draft.costs.map((cost, index) => <div className={`work-record-cost-item${editingCostKey === cost.key ? " expanded" : ""}`} key={cost.key}>
          <button type="button" className="work-record-cost-summary" aria-expanded={editingCostKey === cost.key} onClick={() => setEditingCostKey((current) => current === cost.key ? null : cost.key)}>
            <span className="work-record-cost-icon"><CostCategoryIcon category={cost.category} /></span><span className="work-record-cost-copy"><strong>{workCostCategoryLabels[cost.category]}</strong>{cost.description.trim() && <small>{cost.description.trim()}</small>}</span><b>{currency(numberFromInput(cost.amount))}</b><span className="work-record-cost-chevron" aria-hidden="true">›</span>
          </button>
          {editingCostKey === cost.key && <div className="work-record-cost-fields"><div className="work-cost-category"><span>Categoria</span><CostCategoryGrid value={cost.category} ariaLabel="Categoria do custo" onChange={(category) => setDraft({ ...draft, costs: draft.costs.map((current, i) => i === index ? { ...current, category } : current) })} /></div>
            <MoneyField label="Valor" value={cost.amount} onChange={(amount) => setDraft({ ...draft, costs: draft.costs.map((current, i) => i === index ? { ...current, amount } : current) })} />
            <InputField fieldClassName="work-field" label="Descrição (opcional)" maxLength={60} placeholder="Ex.: Abastecimento no posto" value={cost.description} onChange={(event) => setDraft({ ...draft, costs: draft.costs.map((current, i) => i === index ? { ...current, description: event.target.value } : current) })} />
            <button type="button" className="work-record-cost-remove" onClick={() => { setDraft({ ...draft, costs: draft.costs.filter((_, i) => i !== index) }); setEditingCostKey(null); }}>× <span>Remover custo</span></button></div>}
        </div>)}</div>
        <button type="button" className="work-add-source work-add-cost" onClick={() => { const key = `new-${nextCostKey.current++}`; setDraft({ ...draft, costs: [...draft.costs, { key, category: "FUEL", description: "", amount: "" }] }); setEditingCostKey(key); }}>+ Adicionar custo</button>

        <div className="work-record-result"><span><b>Resultado líquido do dia</b><small>Faturamento {currency(draftGross)} · Custos {currency(draftCosts)}</small></span><strong className={draftNet < 0 ? "negative" : ""}>{draftNet < 0 ? "- " : ""}{currency(Math.abs(draftNet))}</strong></div>
      </div>
      <footer><Button type="button" className="work-save" fullWidth loading={saving} loadingLabel="Salvando..." disabled={checkingDate || !draft.date} onClick={saveRecord}>{draftMode === "create" ? "Registrar trabalho" : "Salvar alterações"}</Button>{draftMode === "edit" && <button type="button" className="work-delete" disabled={saving || checkingDate} onClick={deleteRecord}>Excluir registro</button>}</footer>
    </section>}
  </main>;
}

function DateBadge({ date }: { date: string }) { return <span className="work-day-date"><b>{String(parseDate(date).getDate()).padStart(2, "0")}</b><small>{new Intl.DateTimeFormat("pt-BR", { weekday: "short" }).format(parseDate(date)).replace(".", "")}</small></span>; }
function MoneyField({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) { return <InputField fieldClassName="work-field" controlClassName="work-money-input" label={label} prefix="R$" inputMode="numeric" placeholder="0,00" value={value} onChange={(event) => onChange(inputMoney(event.target.value))} />; }
function BrandedMoneyField({ label, logo, value, onChange }: { label: string; logo: string; value: string; onChange: (value: string) => void }) { return <label className="work-branded-money"><span>{label}</span><div><span className="work-branded-logo"><Image src={logo} alt="" width={96} height={96} /></span><span className="work-branded-input"><b>R$</b><input inputMode="numeric" placeholder="0,00" value={value} onChange={(event) => onChange(inputMoney(event.target.value))} /></span></div></label>; }
