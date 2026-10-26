export type ApiEntryType = "EXPENSE" | "INCOME";
export type ApiPaymentMethod = "CASH" | "PIX" | "DEBIT_CARD" | "CREDIT_CARD" | "BANK_TRANSFER" | "OTHER";
export type ApiRecurrenceFrequency = "NONE" | "DAILY" | "WEEKLY" | "MONTHLY";
export type ApiSeriesScope = "CURRENT" | "CURRENT_AND_FUTURE" | "ALL";
export type ApiUsageProfile = "PERSONAL" | "RIDE_DRIVER" | "DELIVERY";
export type ApiUserProfile = {
  id: string;
  name: string;
  email: string;
  pictureUrl: string | null;
  usageProfile: ApiUsageProfile | null;
};
export type ApiEntryCategory =
  | "FOOD"
  | "HOUSING"
  | "TRANSPORTATION"
  | "HEALTH"
  | "EDUCATION"
  | "CLOTHING"
  | "LEISURE"
  | "BILLS_AND_SERVICES"
  | "SUBSCRIPTIONS"
  | "CREDIT_CARD"
  | "LOANS_AND_FINANCING"
  | "FINANCIAL"
  | "INSURANCE"
  | "PERSONAL_CARE"
  | "TRAVEL"
  | "TAXES"
  | "FAMILY"
  | "PETS"
  | "SHOPPING"
  | "OTHER_EXPENSE"
  | "SALARY"
  | "FREELANCE"
  | "BUSINESS"
  | "INVESTMENT_INCOME"
  | "RENTAL_INCOME"
  | "BENEFITS"
  | "REFUND"
  | "GIFT"
  | "UBER"
  | "NINETY_NINE"
  | "OTHER_INCOME";

export type ApiSubexpense = {
  id: string;
  name: string;
  amount: number;
  installmentDescription: string | null;
  paid: boolean;
  recurrenceFrequency: ApiRecurrenceFrequency;
  recurrenceCount: number;
  recurrenceIndex: number;
  seriesId: string | null;
  createdAt: string;
  updatedAt: string;
};

export type ApiEntry = {
  id: string;
  name: string;
  category: ApiEntryCategory;
  type: ApiEntryType;
  amount: number;
  dueDate: string;
  recurrenceFrequency: ApiRecurrenceFrequency;
  recurrenceCount: number;
  recurrenceIndex: number;
  seriesId: string | null;
  hasSubexpenses: boolean;
  paid: boolean;
  paidOccurrences: number;
  totalOccurrences: number;
  completedSubexpenses: number;
  totalSubexpenses: number;
  subexpenses: ApiSubexpense[];
  paymentMethod: ApiPaymentMethod | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
};

export type EntryPayload = {
  name: string;
  category: ApiEntryCategory;
  type: ApiEntryType;
  amount: number;
  dueDate: string;
  recurrenceFrequency: ApiRecurrenceFrequency;
  recurrenceCount: number;
  hasSubexpenses: boolean;
  subexpenses?: SubexpensePayload[];
  paymentMethod?: ApiPaymentMethod | null;
  notes?: string | null;
};

export type SubexpensePayload = {
  name: string;
  amount: number;
  installmentDescription: string | null;
  paid: boolean;
  recurrenceFrequency: ApiRecurrenceFrequency;
  recurrenceCount: number;
};

export type MonthlyEntries = {
  year: number;
  month: number;
  summary: {
    income: number;
    expenses: number;
    balance: number;
  };
  entries: ApiEntry[];
};

export type ApiForecastEvent = {
  type: "STARTING" | "ENDING";
  source: "ENTRY" | "ITEM";
  name: string;
  amount: number;
  date: string;
  recurrenceFrequency: ApiRecurrenceFrequency;
};

export type ApiForecastMonth = {
  year: number;
  month: number;
  income: number;
  totalExpenses: number;
  paidExpenses: number;
  pendingExpenses: number;
  projectedBalance: number;
  freeBalance: number;
  expenseChange: number;
  freeBalanceChange: number;
  events: ApiForecastEvent[];
};

export type ApiForecast = {
  startYear: number;
  startMonth: number;
  months: number;
  timeline: ApiForecastMonth[];
};

export type ApiGoal = {
  year: number;
  month: number;
  referenceDate: string;
  income: number;
  totalExpenses: number;
  paidExpenses: number;
  pendingExpenses: number;
  includeExpenses: boolean;
  consideredExpenses: number;
  additionalAmount: number;
  targetAmount: number;
  realizedAmount: number;
  remainingAmount: number;
  weeklyTarget: number;
  dailyTarget: number;
  todayTarget: number;
  todayRemaining: number;
  availableDays: number;
  progressPercentage: number;
  covered: boolean;
  todaySelected: boolean;
  selectedDays: number[];
};

export type ApiCustomGoalPeriod = "DAILY" | "WEEKLY" | "MONTHLY";
export type ApiPrimaryGoalType = "EXPENSES" | "CUSTOM";

export type ApiCustomGoal = {
  id: string;
  name: string;
  amount: number;
  period: ApiCustomGoalPeriod;
  referenceDate: string;
  periodStart: string;
  periodEnd: string;
  grossEarnings: number;
  costs: number;
  netResult: number;
  remainingAmount: number;
  dailyTarget: number;
  todayTarget: number;
  todayRemaining: number;
  availableDays: number;
  progressPercentage: number;
  achieved: boolean;
  todaySelected: boolean;
  selectedDays: number[];
};

export type CustomGoalPayload = {
  name: string;
  amount: number;
  period: ApiCustomGoalPeriod;
};

export type ApiHome = {
  date: string;
  hasFinancialEntries: boolean;
  financial: { income: number; pendingExpenses: number; pendingExpenseCount: number; balance: number };
  goal: {
    type: ApiPrimaryGoalType;
    name: string;
    period: ApiCustomGoalPeriod | null;
    targetAmount: number;
    dailyTarget: number;
    baseDailyTarget: number;
    todayGross: number;
    todayCosts: number;
    todayRemaining: number;
    progressPercentage: number;
    periodRemaining: number;
    achieved: boolean;
    covered: boolean;
    todaySelected: boolean;
  };
  nextExpense: { id: string; name: string; dueDate: string; amount: number } | null;
};

export type ApiFinancialNotification = {
  id: string;
  type: "UPCOMING" | "OVERDUE" | "MILEAGE_UPDATE" | "MILEAGE_SUGGESTION";
  entryId: string | null;
  expenseName: string;
  amount: number | null;
  dueDate: string;
  createdAt: string;
  readAt: string | null;
  source: "FINANCIAL" | "VEHICLE" | "OPERATIONAL_COST";
  vehicleId: string | null;
  message: string | null;
  suggestionSource: ApiMileageSource | null;
};

export type ApiVehicleType = "CAR" | "MOTORCYCLE";
export type ApiFuelType = "GASOLINE" | "ETHANOL" | "FLEX" | "DIESEL" | "ELECTRIC" | "HYBRID";
export type ApiFinancialStatus = "PAID_OFF" | "FINANCED" | "RENTED";
export type ApiMaintenanceType = "OIL_CHANGE" | "FILTERS" | "TIRES" | "BRAKES" | "INSPECTION" | "ALIGNMENT_BALANCING" | "OTHER";

export type VehiclePayload = {
  type: ApiVehicleType; brand: string; model: string; manufactureYear: number; modelYear: number;
  version: string | null; nickname: string | null; licensePlate: string | null; fuelType: ApiFuelType;
  currentMileage: number; mileageReadingDate: string; gasolineConsumption: number | null;
  ethanolConsumption: number | null; dieselConsumption: number | null; electricConsumption: number | null; financialStatus: ApiFinancialStatus;
  monthlyAmount: number | null; totalInstallments: number | null; paidInstallments: number | null;
  nextDueDate: string | null; acquisitionDate: string | null;
};

export type MaintenancePayload = {
  type: ApiMaintenanceType; customName: string | null; date: string; mileage: number; amount: number;
  description: string | null; nextMileage: number | null; nextDate: string | null;
};

export type ApiVehicleMaintenance = MaintenancePayload & { id: string };
export type ApiVehicle = Omit<VehiclePayload, "financialStatus" | "monthlyAmount" | "totalInstallments" | "paidInstallments" | "nextDueDate"> & {
  id: string; inUse: boolean; hasPhoto: boolean;
  finance: { status: ApiFinancialStatus; monthlyAmount: number | null; totalInstallments: number | null;
    paidInstallments: number | null; nextDueDate: string | null; remainingInstallments: number | null;
    projectedRemainingInstallments: number | null; projectedEndDate: string | null };
  odometerReadings: { id: string; mileage: number; readingDate: string; correction: boolean }[];
  maintenances: ApiVehicleMaintenance[];
};

export type ApiCostPeriod = "MONTHLY" | "ANNUAL";
export type ApiMaintenanceCalculationMethod = "MANUAL" | "LAST_12_MONTHS";
export type ApiMileageSource = "MANUAL" | "ODOMETER" | "WORK";
export type OperationalCustomFixedCostPayload = { title: string; amount: number; period: ApiCostPeriod; workCostCategory: ApiWorkCostCategory | null };
export type OperationalCostPayload = {
  monthlyMileage: number; mileageSource: ApiMileageSource; fuelType: "GASOLINE" | "ETHANOL" | "DIESEL" | "ELECTRIC"; fuelPrice: number;
  includeFinance: boolean; annualIpva: number; annualFees: number; insuranceAmount: number | null;
  insurancePeriod: ApiCostPeriod | null; maintenanceMethod: ApiMaintenanceCalculationMethod;
  annualMaintenanceEstimate: number | null; includeDepreciation: boolean; currentVehicleValue: number | null;
  annualDepreciationRate: number | null; customFixedCosts: OperationalCustomFixedCostPayload[];
};
export type ApiOperationalCost = OperationalCostPayload & {
  id: string; vehicleId: string; vehicleName: string;
  maintenanceHistory: { available: boolean; unavailableReason: string | null; periodStart: string | null; periodEnd: string | null;
    kilometers: number | null; amount: number | null; maintenanceCount: number; costPerKm: number | null } | null;
  calculation: { fuelMonthly: number; financeMonthly: number; taxesMonthly: number; insuranceMonthly: number;
    otherFixedMonthly: number; maintenanceMonthly: number; totalMonthly: number; cashCostPerKm: number;
    depreciationAnnual: number; depreciationMonthly: number; depreciationPerKm: number };
};
export type ApiMileageSuggestions = {
  configuredMileage: number;
  configuredSource: ApiMileageSource;
  options: { source: Exclude<ApiMileageSource, "MANUAL">; monthlyMileage: number | null; periodStart: string | null; periodEnd: string | null; sampleDays: number; unavailableReason: string | null; available: boolean }[];
};

export type ApiWorkPeriod = "DAY" | "WEEK" | "MONTH";

export type ApiWorkEarning = {
  id: string;
  source: string;
  amount: number;
};

export type ApiWorkEarningSource = {
  id: string;
  name: string;
  systemKey: "UBER" | "NINETY_NINE" | "IFOOD" | "NINETY_NINE_FOOD" | null;
  active: boolean;
  sortOrder: number;
};

export type ApiWorkDay = {
  id: string;
  date: string;
  vehicleId: string | null;
  kilometers: number;
  grossEarnings: number;
  earningsPerKilometer: number;
  earnings: ApiWorkEarning[];
};

export type ApiWorkCostCategory = "FUEL" | "MAINTENANCE" | "TOLL" | "WASH" | "FOOD" | "OTHER";

export type ApiWorkCost = {
  id: string;
  date: string;
  category: ApiWorkCostCategory;
  description: string | null;
  amount: number;
};

export type ApiWorkOverview = {
  period: ApiWorkPeriod;
  referenceDate: string;
  rangeStart: string;
  rangeEnd: string;
  summary: {
    grossEarnings: number;
    fuelCost: number;
    otherCosts: number;
    totalCosts: number;
    netEarnings: number;
    kilometers: number;
    earningsPerKilometer: number;
    workedDays: number;
  };
  estimatedSummary: {
    available: boolean;
    unavailableReason: string | null;
    grossEarnings: number | null;
    operationalCost: number | null;
    additionalCosts: number | null;
    totalCosts: number | null;
    netEarnings: number | null;
    kilometers: number | null;
    earningsPerKilometer: number | null;
    resultPerKilometer: number | null;
  };
  comparison: {
    rangeStart: string;
    rangeEnd: string;
    netEarnings: number;
    changeAmount: number;
    changePercentage: number | null;
  };
  days: ApiWorkDay[];
  costs: ApiWorkCost[];
};

export type WorkDayPayload = {
  kilometers: number;
  vehicleId: string | null;
  earnings: { source: string; amount: number }[];
};

export type WorkCostPayload = {
  date: string;
  category: ApiWorkCostCategory;
  description: string;
  amount: number;
};

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
  ) {
    super(message);
  }
}

const apiUrl = (process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080").replace(/\/$/, "");
const inFlightGetRequests = new Map<string, Promise<unknown>>();

async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = window.localStorage.getItem("amcash.accessToken");
  const method = (init.method ?? "GET").toUpperCase();
  const requestKey = `${token ?? "anonymous"}:${path}`;
  if (method === "GET") {
    const pendingRequest = inFlightGetRequests.get(requestKey);
    if (pendingRequest) return pendingRequest as Promise<T>;
  }

  const request = (async () => {
    const response = await fetch(`${apiUrl}${path}`, {
      ...init,
      headers: {
        ...(init.body && !(init.body instanceof FormData) ? { "Content-Type": "application/json" } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...init.headers,
      },
    });

    if (!response.ok) {
      let message = "Não foi possível concluir a operação.";
      try {
        const body = await response.json() as { message?: string };
        if (body.message) message = body.message;
      } catch {
        // The status code still provides a useful fallback when the body is empty.
      }
      if (response.status === 401) window.dispatchEvent(new Event("amcash:unauthorized"));
      throw new ApiError(message, response.status);
    }

    if (response.status === 204) return undefined as T;
    return response.json() as Promise<T>;
  })();

  if (method !== "GET") return request;
  inFlightGetRequests.set(requestKey, request);
  void request.finally(() => {
    if (inFlightGetRequests.get(requestKey) === request) inFlightGetRequests.delete(requestKey);
  }).catch(() => undefined);
  return request;
}

export const financeApi = {
  userProfile() {
    return apiRequest<ApiUserProfile>("/api/v1/users/me");
  },

  updateUsageProfile(usageProfile: ApiUsageProfile) {
    return apiRequest<ApiUserProfile>("/api/v1/users/me/usage-profile", {
      method: "PUT",
      body: JSON.stringify({ usageProfile }),
    });
  },

  home(date: string) {
    return apiRequest<ApiHome>(`/api/v1/home?date=${date}`);
  },

  notifications() {
    return apiRequest<ApiFinancialNotification[]>("/api/v1/notifications");
  },

  markNotificationsRead() {
    return apiRequest<ApiFinancialNotification[]>("/api/v1/notifications/read", { method: "PATCH" });
  },

  vehicles() { return apiRequest<ApiVehicle[]>("/api/v1/vehicles"); },
  vehicle(id: string) { return apiRequest<ApiVehicle>(`/api/v1/vehicles/${id}`); },
  createVehicle(payload: VehiclePayload) { return apiRequest<ApiVehicle>("/api/v1/vehicles", { method: "POST", body: JSON.stringify(payload) }); },
  updateVehicle(id: string, payload: VehiclePayload) { return apiRequest<ApiVehicle>(`/api/v1/vehicles/${id}`, { method: "PUT", body: JSON.stringify(payload) }); },
  selectVehicle(id: string) { return apiRequest<ApiVehicle>(`/api/v1/vehicles/${id}/select`, { method: "PATCH" }); },
  deleteVehicle(id: string) { return apiRequest<void>(`/api/v1/vehicles/${id}`, { method: "DELETE" }); },
  updateOdometer(id: string, payload: { mileage: number; readingDate: string; correction: boolean }) { return apiRequest<{ id: string; mileage: number; readingDate: string; correction: boolean }>(`/api/v1/vehicles/${id}/odometer`, { method: "PUT", body: JSON.stringify(payload) }); },
  createOdometerReading(id: string, payload: { mileage: number; readingDate: string; correction: boolean }) { return apiRequest<{ id: string; mileage: number; readingDate: string; correction: boolean }>(`/api/v1/vehicles/${id}/odometer`, { method: "POST", body: JSON.stringify(payload) }); },
  updateOdometerReading(vehicleId: string, readingId: string, payload: { mileage: number; readingDate: string; correction: boolean }) { return apiRequest<{ id: string; mileage: number; readingDate: string; correction: boolean }>(`/api/v1/vehicles/${vehicleId}/odometer/${readingId}`, { method: "PUT", body: JSON.stringify(payload) }); },
  deleteOdometerReading(vehicleId: string, readingId: string) { return apiRequest<void>(`/api/v1/vehicles/${vehicleId}/odometer/${readingId}`, { method: "DELETE" }); },
  createMaintenance(vehicleId: string, payload: MaintenancePayload) { return apiRequest<ApiVehicleMaintenance>(`/api/v1/vehicles/${vehicleId}/maintenances`, { method: "POST", body: JSON.stringify(payload) }); },
  updateMaintenance(vehicleId: string, maintenanceId: string, payload: MaintenancePayload) { return apiRequest<ApiVehicleMaintenance>(`/api/v1/vehicles/${vehicleId}/maintenances/${maintenanceId}`, { method: "PUT", body: JSON.stringify(payload) }); },
  deleteMaintenance(vehicleId: string, maintenanceId: string) { return apiRequest<void>(`/api/v1/vehicles/${vehicleId}/maintenances/${maintenanceId}`, { method: "DELETE" }); },
  uploadVehiclePhoto(vehicleId: string, file: Blob) { const body = new FormData(); body.append("file", file, file.type === "image/webp" ? "vehicle.webp" : "vehicle.jpg"); return apiRequest<void>(`/api/v1/vehicles/${vehicleId}/photo`, { method: "PUT", body }); },
  deleteVehiclePhoto(vehicleId: string) { return apiRequest<void>(`/api/v1/vehicles/${vehicleId}/photo`, { method: "DELETE" }); },
  async vehiclePhoto(vehicleId: string) {
    const token = window.localStorage.getItem("amcash.accessToken");
    const response = await fetch(`${apiUrl}/api/v1/vehicles/${vehicleId}/photo`, { headers: token ? { Authorization: `Bearer ${token}` } : {} });
    if (!response.ok) throw new ApiError("Não foi possível carregar a foto.", response.status);
    return response.blob();
  },

  operationalCosts() { return apiRequest<ApiOperationalCost[]>("/api/v1/operational-costs"); },
  operationalCost(vehicleId: string) { return apiRequest<ApiOperationalCost>(`/api/v1/operational-costs/vehicles/${vehicleId}`); },
  createOperationalCost(vehicleId: string, payload: OperationalCostPayload) { return apiRequest<ApiOperationalCost>(`/api/v1/operational-costs/vehicles/${vehicleId}`, { method: "POST", body: JSON.stringify(payload) }); },
  updateOperationalCost(vehicleId: string, payload: OperationalCostPayload) { return apiRequest<ApiOperationalCost>(`/api/v1/operational-costs/vehicles/${vehicleId}`, { method: "PUT", body: JSON.stringify(payload) }); },
  deleteOperationalCost(vehicleId: string) { return apiRequest<void>(`/api/v1/operational-costs/vehicles/${vehicleId}`, { method: "DELETE" }); },
  operationalMileageSuggestions(vehicleId: string) { return apiRequest<ApiMileageSuggestions>(`/api/v1/operational-costs/vehicles/${vehicleId}/mileage-suggestions`); },
  updateOperationalMileage(vehicleId: string, monthlyMileage: number, mileageSource: ApiMileageSource) { return apiRequest<ApiOperationalCost>(`/api/v1/operational-costs/vehicles/${vehicleId}/monthly-mileage`, { method: "PATCH", body: JSON.stringify({ monthlyMileage, mileageSource }) }); },
  dismissOperationalMileageSuggestion(vehicleId: string,source: Exclude<ApiMileageSource,"MANUAL">) { return apiRequest<void>(`/api/v1/operational-costs/vehicles/${vehicleId}/mileage-suggestions/${source}/dismiss`, { method: "PATCH" }); },

  listMonth(year: number, month: number) {
    return apiRequest<MonthlyEntries>(`/api/v1/transactions?year=${year}&month=${month}`);
  },

  forecast(year: number, month: number, months: 6 | 12) {
    return apiRequest<ApiForecast>(
      `/api/v1/transactions/forecast?year=${year}&month=${month}&months=${months}`,
    );
  },

  goal(date: string, year?: number, month?: number) {
    const target = year !== undefined && month !== undefined ? `&year=${year}&month=${month}` : "";
    return apiRequest<ApiGoal>(`/api/v1/goals?date=${date}${target}`);
  },

  updateGoalPreferences(date: string, selectedDays: number[], year?: number, month?: number) {
    const target = year !== undefined && month !== undefined ? `&year=${year}&month=${month}` : "";
    return apiRequest<ApiGoal>(`/api/v1/goals/preferences?date=${date}${target}`, {
      method: "PUT",
      body: JSON.stringify({ selectedDays }),
    });
  },

  updateGoalConfiguration(date: string, payload: { includeExpenses: boolean; additionalAmount: number }) {
    return apiRequest<ApiGoal>(`/api/v1/goals/configuration?date=${date}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    });
  },

  customGoal(date: string) {
    return apiRequest<ApiCustomGoal | undefined>(`/api/v1/goals/custom?date=${date}`);
  },

  createCustomGoal(date: string, payload: CustomGoalPayload) {
    return apiRequest<ApiCustomGoal>(`/api/v1/goals/custom?date=${date}`, {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },

  updateCustomGoal(date: string, payload: CustomGoalPayload) {
    return apiRequest<ApiCustomGoal>(`/api/v1/goals/custom?date=${date}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    });
  },

  deleteCustomGoal() {
    return apiRequest<void>("/api/v1/goals/custom", { method: "DELETE" });
  },

  primaryGoal() {
    return apiRequest<{ type: ApiPrimaryGoalType }>("/api/v1/goals/primary");
  },

  updatePrimaryGoal(type: ApiPrimaryGoalType) {
    return apiRequest<{ type: ApiPrimaryGoalType }>("/api/v1/goals/primary", {
      method: "PUT",
      body: JSON.stringify({ type }),
    });
  },

  workOverview(date: string, period: ApiWorkPeriod) {
    return apiRequest<ApiWorkOverview>(`/api/v1/work?date=${date}&period=${period}`);
  },

  workEarningSources() {
    return apiRequest<ApiWorkEarningSource[]>("/api/v1/work/earning-sources");
  },

  createWorkEarningSource(name: string) {
    return apiRequest<ApiWorkEarningSource>("/api/v1/work/earning-sources", {
      method: "POST",
      body: JSON.stringify({ name }),
    });
  },

  renameWorkEarningSource(sourceId: string, name: string) {
    return apiRequest<ApiWorkEarningSource>(`/api/v1/work/earning-sources/${sourceId}`, {
      method: "PUT",
      body: JSON.stringify({ name }),
    });
  },

  deactivateWorkEarningSource(sourceId: string, date?: string) {
    const query = date ? `?date=${encodeURIComponent(date)}` : "";
    return apiRequest<void>(`/api/v1/work/earning-sources/${sourceId}${query}`, { method: "DELETE" });
  },

  saveWorkDay(date: string, payload: WorkDayPayload) {
    return apiRequest<ApiWorkDay>(`/api/v1/work/days/${date}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    });
  },

  deleteWorkDay(date: string) {
    return apiRequest<void>(`/api/v1/work/days/${date}`, { method: "DELETE" });
  },

  createWorkCost(payload: WorkCostPayload) {
    return apiRequest<ApiWorkCost>("/api/v1/work/costs", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },

  updateWorkCost(costId: string, payload: WorkCostPayload) {
    return apiRequest<ApiWorkCost>(`/api/v1/work/costs/${costId}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    });
  },

  deleteWorkCost(costId: string) {
    return apiRequest<void>(`/api/v1/work/costs/${costId}`, { method: "DELETE" });
  },

  create(payload: EntryPayload) {
    return apiRequest<{ entries: ApiEntry[] }>("/api/v1/transactions", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },

  get(entryId: string) {
    return apiRequest<ApiEntry>(`/api/v1/transactions/${entryId}`);
  },

  update(entryId: string, payload: EntryPayload, scope: ApiSeriesScope = "CURRENT") {
    return apiRequest<ApiEntry>(`/api/v1/transactions/${entryId}?scope=${scope}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    });
  },

  setPaid(entryId: string, paid: boolean) {
    return apiRequest<ApiEntry>(`/api/v1/transactions/${entryId}/paid?paid=${paid}`, {
      method: "PATCH",
    });
  },

  delete(entryId: string, scope: ApiSeriesScope = "CURRENT") {
    return apiRequest<void>(`/api/v1/transactions/${entryId}?scope=${scope}`, { method: "DELETE" });
  },

  addSubexpense(entryId: string, payload: SubexpensePayload) {
    return apiRequest<ApiSubexpense>(`/api/v1/transactions/${entryId}/subexpenses`, {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },

  updateSubexpense(entryId: string, subexpenseId: string, payload: SubexpensePayload, scope: ApiSeriesScope = "CURRENT") {
    return apiRequest<ApiSubexpense>(
      `/api/v1/transactions/${entryId}/subexpenses/${subexpenseId}?scope=${scope}`,
      { method: "PUT", body: JSON.stringify(payload) },
    );
  },

  deleteSubexpense(entryId: string, subexpenseId: string, scope: ApiSeriesScope = "CURRENT") {
    return apiRequest<void>(
      `/api/v1/transactions/${entryId}/subexpenses/${subexpenseId}?scope=${scope}`,
      { method: "DELETE" },
    );
  },
};
