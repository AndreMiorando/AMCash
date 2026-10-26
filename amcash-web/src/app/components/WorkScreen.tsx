"use client";

import Image from "next/image";
import { ReactNode, useCallback, useEffect, useRef, useState } from "react";
import { ApiVehicle, ApiWorkCost, ApiWorkCostCategory, ApiWorkDay, ApiWorkEarningSource, ApiWorkOverview, ApiWorkPeriod, financeApi } from "@/lib/api";
import AppHeader from "@/app/components/AppHeader";
import CostDialog, { CostDialogValue } from "@/app/components/CostDialog";
import { CostCategoryGrid, CostCategoryIcon, workCostCategoryLabels } from "@/app/components/CostCategoryGrid";
import DatePickerInput, { dateToInputValue } from "@/app/components/DatePickerInput";
import MonthPickerInput from "@/app/components/MonthPickerInput";
import EarningSourceDialog from "@/app/components/EarningSourceDialog";
import Button from "@/app/components/ui/Button";
import { InputField } from "@/app/components/ui/Input";
import SegmentedControl from "@/app/components/ui/SegmentedControl";
import { KpiLabel, KpiValue, PrimaryKpiCard, SecondaryKpiGroup, SecondaryKpiItem } from "@/app/components/ui/Kpi";
import { formatDateRange, formatLongDate, formatMonthYear } from "@/lib/date";
import { WorkPlatform, workPlatformByKey, workPlatformBySource } from "@/lib/workPlatforms";

type EarningDraft = {
  key: string;
  sourceId?: string;
  source: string;
  originalSource: string;
  amount: string;
  persistForFuture: boolean;
  systemKey: ApiWorkEarningSource["systemKey"];
};
type CostDraft = { key: string; id?: string; category: ApiWorkCostCategory; description: string; amount: string; homeManaged?: boolean };
type WorkDraft = {
  date: string;
  vehicleId: string | null;
  kilometers: string;
  earnings: EarningDraft[];
  costs: CostDraft[];
  originalCostIds: string[];
  hadWorkDay: boolean;
};

const homeCostPrefix = "HOME: ";
const legacyHomeCostDescription = "Custo do dia — Home";

const periodLabels: Record<ApiWorkPeriod, string> = { DAY: "Dia", WEEK: "Semana", MONTH: "Mês" };
const periodOptions = (Object.keys(periodLabels) as ApiWorkPeriod[]).map((value) => ({ value, label: periodLabels[value] }));
type ResultMode = "REGISTERED" | "ESTIMATED";
const resultModeOptions: { value: ResultMode; label: string }[] = [
  { value: "REGISTERED", label: "Registrado" },
  { value: "ESTIMATED", label: "Estimado" },
];
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
  const platform = workPlatformBySource(source);
  if (!platform) return 2;
  return platform.key === "UBER" || platform.key === "IFOOD" ? 0 : 1;
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
  if (period === "DAY") return formatLongDate(referenceDate);
  if (period === "WEEK") return formatDateRange(overview.rangeStart, overview.rangeEnd);
  return formatMonthYear(referenceDate);
}

type WorkBreakdownItem = {
  key: string;
  label: string;
  amount: number;
  platform?: WorkPlatform | null;
  category?: ApiWorkCostCategory;
};

function WorkPeriodBreakdown({ overview, incomeExpanded, costsExpanded, onToggleIncome, onToggleCosts }: {
  overview: ApiWorkOverview;
  incomeExpanded: boolean;
  costsExpanded: boolean;
  onToggleIncome: () => void;
  onToggleCosts: () => void;
}) {
  const earningsBySource = new Map<string, { label: string; amount: number }>();
  overview.days.flatMap((day) => day.earnings).forEach((earning) => {
    const label = earning.source.trim() || "Outra renda";
    const key = label.toLocaleLowerCase("pt-BR");
    const current = earningsBySource.get(key);
    earningsBySource.set(key, { label: current?.label ?? label, amount: (current?.amount ?? 0) + Number(earning.amount) });
  });
  const detailedEarnings: WorkBreakdownItem[] = [...earningsBySource.entries()]
    .map(([key, item]) => ({
      key,
      label: item.label,
      amount: item.amount,
      platform: workPlatformBySource(item.label),
    }))
    .sort((first, second) => second.amount - first.amount || earningPriority(first.label) - earningPriority(second.label));

  const costsByCategory = new Map<ApiWorkCostCategory, number>();
  overview.costs.forEach((cost) => costsByCategory.set(cost.category, (costsByCategory.get(cost.category) ?? 0) + Number(cost.amount)));
  const detailedCosts: WorkBreakdownItem[] = [...costsByCategory.entries()]
    .filter(([, amount]) => amount > 0)
    .map(([category, amount]) => ({ key: category, label: workCostCategoryLabels[category], amount, category }))
    .sort((first, second) => second.amount - first.amount);
  const totalEarnings = detailedEarnings.reduce((total, item) => total + item.amount, 0);
  const totalCosts = detailedCosts.reduce((total, item) => total + item.amount, 0);
  const percentage = (amount: number, total: number) => total > 0 ? Math.min(100, Math.max(0, amount / total * 100)) : 0;
  const percentageLabel = (value: number) => `${new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 1 }).format(value)}%`;

  function renderGroup(title: string, total: number, items: WorkBreakdownItem[], tone: "income" | "cost", expanded: boolean, onToggle: () => void) {
    const visibleItems = expanded ? items : items.slice(0, 3);
    const hiddenCount = Math.max(0, items.length - 3);
    return <article className={`work-breakdown-group ${tone}`}>
      <header><span>{title}</span></header>
      {!visibleItems.length ? <p>Nenhum registro no período.</p> : visibleItems.map((item) => {
        const share = percentage(item.amount, total);
        return <div className="work-breakdown-row" key={item.key}>
          <div className="work-breakdown-row-heading">
            <span className="work-breakdown-name">
              {tone === "income" && item.platform && <Image className={`work-breakdown-source-logo ${item.platform.className}`} src={item.platform.logo} alt="" width={96} height={96} />}
              {tone === "income" && !item.platform && <i className="work-breakdown-source-mark" aria-hidden="true">{item.label.charAt(0).toUpperCase()}</i>}
              {tone === "cost" && item.category && <i className="work-breakdown-cost-icon"><CostCategoryIcon category={item.category} /></i>}
              <span>{item.label}</span>
            </span>
            <strong>{currency(item.amount)}</strong>
          </div>
          <div className="work-breakdown-progress-heading"><b>{percentageLabel(share)}</b></div>
          <div className="work-breakdown-progress" role="progressbar" aria-label={`${item.label}: ${percentageLabel(share)} do total`} aria-valuemin={0} aria-valuemax={100} aria-valuenow={Number(share.toFixed(1))}><i style={{ width: `${share}%` }} /></div>
        </div>;
      })}
      {items.length > 3 && <Button type="button" variant="secondary" fullWidth className="work-breakdown-toggle" onClick={onToggle} aria-expanded={expanded}>{expanded ? "Ver menos" : `Ver mais (${hiddenCount})`}</Button>}
    </article>;
  }

  return <section className="work-period-breakdown" aria-labelledby="work-period-breakdown-title">
    <div className="work-breakdown-title">
      <div><span>COMPOSIÇÃO DO PERÍODO</span><h2 id="work-period-breakdown-title">Receitas e custos</h2></div>
    </div>
    <div className="work-breakdown-columns">
      {renderGroup("Faturamento por origem", totalEarnings, detailedEarnings, "income", incomeExpanded, onToggleIncome)}
      {renderGroup("Gastos por categoria", totalCosts, detailedCosts, "cost", costsExpanded, onToggleCosts)}
    </div>
  </section>;
}

export default function WorkScreen({ navigation, onReady, onOperationalCost }: {
  navigation: (loading: boolean) => ReactNode;
  onReady?: () => void;
  onOperationalCost: () => void;
}) {
  const [period, setPeriod] = useState<ApiWorkPeriod>("WEEK");
  const [referenceDate, setReferenceDate] = useState(() => dateToInputValue(new Date()));
  const [overview, setOverview] = useState<ApiWorkOverview | null>(null);
  const [resultMode, setResultMode] = useState<ResultMode>("REGISTERED");
  const [expandedBreakdownGroups, setExpandedBreakdownGroups] = useState({ income: false, costs: false });
  const [vehicles,setVehicles]=useState<ApiVehicle[]>([]);
  const [vehiclesLoading, setVehiclesLoading] = useState(true);
  const [earningSources, setEarningSources] = useState<ApiWorkEarningSource[]>([]);
  const [earningSourcesLoading, setEarningSourcesLoading] = useState(true);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [draft, setDraft] = useState<WorkDraft | null>(null);
  const [draftMode, setDraftMode] = useState<"create" | "edit">("edit");
  const [checkingDate, setCheckingDate] = useState(false);
  const [editingCostKey, setEditingCostKey] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [sourceDialogOpen, setSourceDialogOpen] = useState(false);
  const [costDialogOpen, setCostDialogOpen] = useState(false);
  const requestId = useRef(0);
  const workDateRequestId = useRef(0);
  const nextCostKey = useRef(0);
  const nextEarningKey = useRef(0);

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

  const loadVehicles = useCallback(async () => {
    setVehiclesLoading(true);
    try { setVehicles(await financeApi.vehicles()); }
    catch { setVehicles([]); }
    finally { setVehiclesLoading(false); }
  }, []);

  const loadEarningSources = useCallback(async () => {
    setEarningSourcesLoading(true);
    try { setEarningSources(await financeApi.workEarningSources()); }
    catch (requestError) { setError(messageFrom(requestError)); }
    finally { setEarningSourcesLoading(false); }
  }, []);

  useEffect(() => { const timer = window.setTimeout(() => void load(), 0); return () => window.clearTimeout(timer); }, [load]);
  useEffect(() => { const timer = window.setTimeout(() => void loadVehicles(), 0); return () => window.clearTimeout(timer); }, [loadVehicles]);
  useEffect(() => { const timer = window.setTimeout(() => void loadEarningSources(), 0); return () => window.clearTimeout(timer); }, [loadEarningSources]);
  const pageLoading = loading || vehiclesLoading || earningSourcesLoading;
  useEffect(() => { if (!pageLoading) onReady?.(); }, [pageLoading, onReady]);

  function movePeriod(direction: number) {
    const date = parseDate(referenceDate);
    if (period === "DAY") date.setDate(date.getDate() + direction);
    if (period === "WEEK") date.setDate(date.getDate() + 7 * direction);
    if (period === "MONTH") date.setMonth(date.getMonth() + direction, 1);
    setReferenceDate(dateToInputValue(date));
  }
  function earningDrafts(day: ApiWorkDay | undefined) {
    const usedEarningIds = new Set<string>();
    const persistent = earningSources.map((source) => {
      const earning = day?.earnings.find((item) => !usedEarningIds.has(item.id) && item.source.trim().toLowerCase() === source.name.trim().toLowerCase());
      if (earning) usedEarningIds.add(earning.id);
      return { key: source.id, sourceId: source.id, source: source.name, originalSource: source.name,
        amount: moneyForInput(Number(earning?.amount ?? 0)), persistForFuture: true, systemKey: source.systemKey } satisfies EarningDraft;
    });
    const historical = day?.earnings.filter((earning) => !usedEarningIds.has(earning.id)).map((earning) => ({
      key: `earning-${earning.id}`, source: earning.source, originalSource: earning.source,
      amount: moneyForInput(Number(earning.amount)), persistForFuture: false, systemKey: null,
    } satisfies EarningDraft)) ?? [];
    return [...persistent, ...historical];
  }

  function openRecord(date: string, day: ApiWorkDay | undefined, costs: ApiWorkCost[]) {
    setDraft({ date,vehicleId:day?.vehicleId??vehicles.find(vehicle=>vehicle.inUse)?.id??null, kilometers: day?.kilometers ? String(day.kilometers).replace(".", ",") : "",
      earnings: earningDrafts(day),
      costs: costs.map((cost) => ({ key: cost.id, id: cost.id, category: cost.category,
        description: costDescription(cost), amount: moneyForInput(Number(cost.amount)), homeManaged: isHomeCost(cost) })),
      originalCostIds: costs.map((cost) => cost.id), hadWorkDay: Boolean(day) });
    setEditingCostKey(null);
    setSourceDialogOpen(false);
    setDraftMode("edit");
    setError("");
  }

  function emptyDraft(date: string): WorkDraft {
    return { date,vehicleId:vehicles.find(vehicle=>vehicle.inUse)?.id??null, kilometers: "", earnings: earningDrafts(undefined), costs: [], originalCostIds: [], hadWorkDay: false };
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
    const earnings = draft.earnings.filter((item) => item.sourceId || item.source.trim() || item.amount);
    if (earnings.some((item) => !item.source.trim())) { setError("Informe o nome de cada ganho adicional."); return; }
    const normalizedSources = earnings.map((item) => item.source.trim().toLocaleLowerCase("pt-BR"));
    if (new Set(normalizedSources).size !== normalizedSources.length) { setError("Cada ganho deve aparecer apenas uma vez."); return; }
    const validCosts = draft.costs.filter((cost) => numberFromInput(cost.amount) > 0);
    const retainedCostIds = new Set(validCosts.flatMap((cost) => cost.id ? [cost.id] : []));
    const hasWorkValues = numberFromInput(draft.kilometers) > 0 || earnings.some((item) => numberFromInput(item.amount) > 0);
    const hasSourceChanges = earnings.some((item) => item.sourceId && item.systemKey === null && item.source.trim() !== item.originalSource)
      || earnings.some((item) => !item.sourceId && item.persistForFuture && item.source.trim());
    if (draftMode === "create" && !hasWorkValues && !validCosts.length && !hasSourceChanges) { setError("Informe ao menos um dado do trabalho antes de registrar."); return; }
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
      let resolvedEarnings = earnings;
      for (const earning of earnings) {
        if (earning.sourceId && earning.systemKey === null && earning.source.trim() !== earning.originalSource) {
          const updated = await financeApi.renameWorkEarningSource(earning.sourceId, earning.source.trim());
          resolvedEarnings = resolvedEarnings.map((item) => item.key === earning.key
            ? { ...item, source: updated.name, originalSource: updated.name }
            : item);
        } else if (!earning.sourceId && earning.persistForFuture && earning.source.trim()) {
          const created = await financeApi.createWorkEarningSource(earning.source.trim());
          resolvedEarnings = resolvedEarnings.map((item) => item.key === earning.key
            ? { ...item, sourceId: created.id, source: created.name, originalSource: created.name }
            : item);
        }
        setDraft((current) => current ? { ...current, earnings: current.earnings.map((item) => resolvedEarnings.find((resolved) => resolved.key === item.key) ?? item) } : current);
      }
      const operations: Promise<unknown>[] = [];
      if (draftMode === "create" || draft.hadWorkDay || hasWorkValues) operations.push(financeApi.saveWorkDay(draft.date, {
        kilometers: numberFromInput(draft.kilometers),
        vehicleId:draft.vehicleId,
        earnings: resolvedEarnings.map((item) => ({ source: item.source.trim(), amount: numberFromInput(item.amount) })),
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
      const [refreshed, refreshedSources] = await Promise.all([
        financeApi.workOverview(draft.date, period),
        hasSourceChanges ? financeApi.workEarningSources() : Promise.resolve(earningSources),
      ]);
      setEarningSources(refreshedSources);
      setReferenceDate(draft.date); setOverview(refreshed); setSourceDialogOpen(false); setDraft(null);
    } catch (requestError) { setError(messageFrom(requestError)); } finally { setSaving(false); }
  }
  async function deleteRecord() {
    if (!draft || saving || !window.confirm("Excluir o trabalho e todos os custos deste dia?")) return;
    setSaving(true); try {
      await Promise.all([
        ...(draft.hadWorkDay ? [financeApi.deleteWorkDay(draft.date)] : []),
        ...draft.originalCostIds.map((id) => financeApi.deleteWorkCost(id)),
      ]);
      setSourceDialogOpen(false); setDraft(null); await load();
    } catch (requestError) { setError(messageFrom(requestError)); } finally { setSaving(false); }
  }

  const summary = overview?.summary;
  const estimated = overview?.estimatedSummary;
  const showingEstimated = resultMode === "ESTIMATED";
  const estimateAvailable = Boolean(estimated?.available);
  const displayedNet = showingEstimated ? estimated?.netEarnings : summary?.netEarnings;
  const displayedCosts = showingEstimated ? estimated?.totalCosts : summary?.totalCosts;
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
  const draftGross = draft?.earnings.reduce((total, earning) => total + numberFromInput(earning.amount), 0) ?? 0;
  const draftCosts = draft?.costs.reduce((total, cost) => total + numberFromInput(cost.amount), 0) ?? 0;
  const draftNet = draftGross - draftCosts;
  return <main className="work-screen">
    <AppHeader className="work-header"><div className="work-header-actions"><span className="work-header-label">RESULTADO</span><button type="button" className="header-add-button work-register-button" onClick={openCreateRecord} disabled={checkingDate} aria-label={checkingDate ? "Verificando registro" : "Registrar dia"} title={checkingDate ? "Verificando registro" : "Registrar dia"}>{checkingDate ? <span className="work-register-loader" aria-hidden="true" /> : <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><rect x="3.5" y="5" width="17" height="15" rx="2" /><path d="M8 3v4M16 3v4M3.5 9.5h17M12 13v5M9.5 15.5h5" /></svg>}</button></div></AppHeader>
    <div className="work-content">
      {error && !draft && <p className="work-error" role="alert">{error}</p>}
      {overview && <>
        <PrimaryKpiCard className="work-net-card">
          <SegmentedControl className="work-result-mode" ariaLabel="Tipo de resultado" value={resultMode} options={resultModeOptions} onChange={setResultMode} />
          <div className="work-net-main"><KpiLabel>{showingEstimated ? "RESULTADO OPERACIONAL" : "LÍQUIDO NO PERÍODO"}</KpiLabel>{!showingEstimated || estimateAvailable ? <KpiValue className={(displayedNet ?? 0) < 0 ? "negative" : ""}>{currency(Number(displayedNet ?? 0))}</KpiValue> : <><KpiValue>—</KpiValue><small className="work-estimate-reason">{estimated?.unavailableReason}</small><button type="button" className="work-estimate-action" onClick={onOperationalCost}>Configurar custo operacional</button></>}</div>
          <div className="work-net-side">
            {!showingEstimated ? <div className={`work-net-comparison ${comparisonTone}`} aria-label={`${comparisonValue} em relação ao ${previousPeriodLabel}`} title={`Líquido do ${previousPeriodLabel}: ${currency(Number(comparison?.netEarnings ?? 0))}`}>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true"><path d="M5 20v-6M12 20V9M19 20V4" /></svg>
              <strong>{comparisonValue}</strong>
            </div> : estimateAvailable && <div className="work-net-comparison neutral" aria-label={`Resultado estimado por quilômetro: ${currency(Number(estimated?.resultPerKilometer ?? 0))}`}><KpiLabel>RESULTADO/KM</KpiLabel><strong>{currency(Number(estimated?.resultPerKilometer ?? 0))}</strong></div>}
          </div>
        </PrimaryKpiCard>
        <SecondaryKpiGroup className="work-metrics">
          <SecondaryKpiItem><span className="work-metric-copy"><KpiLabel>Faturamento</KpiLabel><KpiValue>{currency(Number(summary?.grossEarnings ?? 0))}</KpiValue></span></SecondaryKpiItem>
          <SecondaryKpiItem><span className="work-metric-copy"><KpiLabel>{showingEstimated ? "Custos estimados" : "Custos registrados"}</KpiLabel><KpiValue className="cost">{showingEstimated && !estimateAvailable ? "—" : currency(Number(displayedCosts ?? 0))}</KpiValue></span></SecondaryKpiItem>
          <SecondaryKpiItem><span className="work-metric-copy"><KpiLabel>Quilômetros</KpiLabel><KpiValue>{decimal(Number(summary?.kilometers ?? 0))}</KpiValue></span></SecondaryKpiItem>
          <SecondaryKpiItem><span className="work-metric-copy"><KpiLabel>Faturamento por km</KpiLabel><KpiValue>{currency(Number(summary?.earningsPerKilometer ?? 0))}</KpiValue></span></SecondaryKpiItem>
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
        {period !== "DAY" && overview && <WorkPeriodBreakdown
          overview={overview}
          incomeExpanded={expandedBreakdownGroups.income}
          costsExpanded={expandedBreakdownGroups.costs}
          onToggleIncome={() => setExpandedBreakdownGroups((current) => ({ ...current, income: !current.income }))}
          onToggleCosts={() => setExpandedBreakdownGroups((current) => ({ ...current, costs: !current.costs }))}
        />}
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
                  const platform = workPlatformBySource(source);
                  return <span className="work-day-earning" key={earning.id}>
                    <span className="work-day-earning-source">
                      {platform && <><Image className={`work-day-platform-logo ${platform.className}`} src={platform.logo} alt="" width={96} height={96} /><span className="sr-only">{platform.name}</span></>}
                      {!platform && <><span className="work-day-source-mark" aria-hidden="true">{source.charAt(0).toUpperCase() || "+"}</span><span className="work-day-source-name" title={source}>{source}</span></>}
                    </span>
                    <b>{currency(Number(earning.amount))}</b>
                  </span>;
                })}
              </span>}
            </button>;
          })}</section>
      </>}
    </div>{navigation(pageLoading)}

    {draft && <section className="work-record-screen" aria-labelledby="work-record-title">
      <header className="work-record-page-header"><button type="button" onClick={() => { if (!saving) { setSourceDialogOpen(false); setCostDialogOpen(false); setDraft(null); } }} aria-label="Voltar"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="m15 18-6-6 6-6" /></svg></button><h2 id="work-record-title">{draftMode === "create" ? "Registrar trabalho" : "Editar trabalho"}</h2><span aria-hidden="true" /></header>
      <div className="work-form-scroll">
        {error && <p className="work-error" role="alert">{error}</p>}
        <div className="work-record-date-field"><DatePickerInput label="Data do trabalho" value={draft.date} max={dateToInputValue(new Date())} onChange={selectWorkDate} />{draft.vehicleId&&<small className="work-record-active-vehicle">Veículo em uso: {vehicles.find(vehicle=>vehicle.id===draft.vehicleId)?.nickname||vehicles.find(vehicle=>vehicle.id===draft.vehicleId)?.model}</small>}</div>
        <label className="work-record-kilometers"><span>Quilômetros rodados</span><div><span className="work-record-road-icon"><Image src="/road-icon.png" alt="" width={1254} height={1254} /></span><span className="work-record-kilometers-input"><b>km</b><input inputMode="decimal" aria-label="Quilômetros rodados" placeholder="0" value={draft.kilometers} onChange={(event) => setDraft({ ...draft, kilometers: numberInput(event.target.value) })} /></span></div></label>
        <div className="work-record-section-title"><h3>Faturamento</h3></div><div className="work-form-grid">
          {draft.earnings.filter((item) => item.systemKey).map((item) => <BrandedMoneyField key={item.key} label={item.source} logo={item.systemKey ? workPlatformByKey(item.systemKey).logo : "/99-logo.png"} value={item.amount} onChange={(value) => setDraft({ ...draft, earnings: draft.earnings.map((current) => current.key === item.key ? { ...current, amount: value } : current) })} />)}
          {draft.earnings.filter((item) => !item.systemKey).map((item) => <CustomEarningMoneyField key={item.key} source={item.source} value={item.amount} removable={!item.sourceId} onSourceChange={(source) => setDraft({ ...draft, earnings: draft.earnings.map((current) => current.key === item.key ? { ...current, source } : current) })} onAmountChange={(amount) => setDraft({ ...draft, earnings: draft.earnings.map((current) => current.key === item.key ? { ...current, amount } : current) })} onRemove={() => setDraft({ ...draft, earnings: draft.earnings.filter((current) => current.key !== item.key) })} />)}
        </div>
        <button type="button" className="work-add-source" onClick={() => setSourceDialogOpen(true)}>+ Adicionar outro ganho</button>

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
        <button type="button" className="work-add-source work-add-cost" onClick={() => setCostDialogOpen(true)}>+ Adicionar custo</button>

        <div className="work-record-result"><span><b>Resultado líquido do dia</b><small>Faturamento {currency(draftGross)} · Custos {currency(draftCosts)}</small></span><strong className={draftNet < 0 ? "negative" : ""}>{draftNet < 0 ? "- " : ""}{currency(Math.abs(draftNet))}</strong></div>
      </div>
      <footer><Button type="button" className="work-save" fullWidth loading={saving} loadingLabel="Salvando..." disabled={checkingDate || !draft.date} onClick={saveRecord}>{draftMode === "create" ? "Registrar trabalho" : "Salvar alterações"}</Button>{draftMode === "edit" && <button type="button" className="work-delete" disabled={saving || checkingDate} onClick={deleteRecord}>Excluir registro</button>}</footer>
    </section>}
    {draft && sourceDialogOpen && <EarningSourceDialog existingSources={draft.earnings.map((item) => item.source)} onClose={() => setSourceDialogOpen(false)} onAdd={(value) => { const key = `new-earning-${nextEarningKey.current++}`; setDraft({ ...draft, earnings: [...draft.earnings, { key, source: value.source, originalSource: "", amount: value.amount, persistForFuture: value.persistForFuture, systemKey: null }] }); setSourceDialogOpen(false); }} />}
    {draft && costDialogOpen && <CostDialog onClose={() => setCostDialogOpen(false)} onAdd={(value: CostDialogValue) => { const key = `new-${nextCostKey.current++}`; setDraft({ ...draft, costs: [...draft.costs, { key, ...value }] }); setEditingCostKey(null); setCostDialogOpen(false); }} />}
  </main>;
}

function DateBadge({ date }: { date: string }) { return <span className="work-day-date"><b>{String(parseDate(date).getDate()).padStart(2, "0")}</b><small>{new Intl.DateTimeFormat("pt-BR", { weekday: "short" }).format(parseDate(date)).replace(".", "")}</small></span>; }
function MoneyField({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) { return <InputField fieldClassName="work-field" controlClassName="work-money-input" label={label} prefix="R$" inputMode="numeric" placeholder="0,00" value={value} onChange={(event) => onChange(inputMoney(event.target.value))} />; }
function BrandedMoneyField({ label, logo, value, onChange }: { label: string; logo: string; value: string; onChange: (value: string) => void }) { return <label className="work-branded-money"><span>{label}</span><div><span className="work-branded-logo"><Image src={logo} alt="" width={96} height={96} /></span><span className="work-branded-input"><b>R$</b><input inputMode="numeric" placeholder="0,00" value={value} onChange={(event) => onChange(inputMoney(event.target.value))} /></span></div></label>; }
function CustomEarningMoneyField({ source, value, removable, onSourceChange, onAmountChange, onRemove }: { source: string; value: string; removable: boolean; onSourceChange: (value: string) => void; onAmountChange: (value: string) => void; onRemove: () => void }) { return <div className="work-branded-money"><span className="work-branded-label"><input className="work-branded-source-name" aria-label="Nome do ganho" maxLength={30} value={source} onChange={(event) => onSourceChange(event.target.value)} />{removable && <button type="button" aria-label="Remover ganho" onClick={onRemove}><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6M10 10v6M14 10v6" /></svg></button>}</span><div><span className="work-branded-logo additional"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><rect x="3" y="6" width="18" height="13" rx="2" /><path d="M16 10h5v5h-5a2.5 2.5 0 0 1 0-5ZM7 6V4h10v2" /></svg></span><span className="work-branded-input"><b>R$</b><input aria-label={`Valor de ${source}`} inputMode="numeric" placeholder="0,00" value={value} onChange={(event) => onAmountChange(inputMoney(event.target.value))} /></span></div></div>; }
