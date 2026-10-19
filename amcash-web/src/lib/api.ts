export type ApiEntryType = "EXPENSE" | "INCOME";
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
  remainingAmount: number;
  weeklyTarget: number;
  dailyTarget: number;
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
    dailyTarget: number;
    periodRemaining: number;
    covered: boolean;
    todaySelected: boolean;
  };
  nextExpense: { id: string; name: string; dueDate: string; amount: number } | null;
};

export type ApiWorkPeriod = "DAY" | "WEEK" | "MONTH";

export type ApiWorkEarning = {
  id: string;
  source: string;
  amount: number;
};

export type ApiWorkDay = {
  id: string;
  date: string;
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

async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = window.localStorage.getItem("amcash.accessToken");
  const response = await fetch(`${apiUrl}${path}`, {
    ...init,
    headers: {
      ...(init.body ? { "Content-Type": "application/json" } : {}),
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
