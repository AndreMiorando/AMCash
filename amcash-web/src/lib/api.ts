export type ApiEntryType = "EXPENSE" | "INCOME";
export type ApiRecurrenceFrequency = "NONE" | "DAILY" | "WEEKLY" | "MONTHLY";
export type ApiEntryCategory =
  | "FOOD"
  | "HOUSING"
  | "TRANSPORTATION"
  | "HEALTH"
  | "EDUCATION"
  | "CLOTHING"
  | "LEISURE"
  | "BILLS_AND_SERVICES"
  | "FINANCIAL"
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
  | "OTHER_INCOME";

export type ApiSubexpense = {
  id: string;
  name: string;
  amount: number;
  installmentDescription: string | null;
  paid: boolean;
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
};

export type SubexpensePayload = {
  name: string;
  amount: number;
  installmentDescription: string | null;
  paid: boolean;
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
  listMonth(year: number, month: number) {
    return apiRequest<MonthlyEntries>(`/api/v1/transactions?year=${year}&month=${month}`);
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

  update(entryId: string, payload: EntryPayload) {
    return apiRequest<ApiEntry>(`/api/v1/transactions/${entryId}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    });
  },

  setPaid(entryId: string, paid: boolean) {
    return apiRequest<ApiEntry>(`/api/v1/transactions/${entryId}/paid?paid=${paid}`, {
      method: "PATCH",
    });
  },

  delete(entryId: string) {
    return apiRequest<void>(`/api/v1/transactions/${entryId}`, { method: "DELETE" });
  },

  addSubexpense(entryId: string, payload: SubexpensePayload) {
    return apiRequest<ApiSubexpense>(`/api/v1/transactions/${entryId}/subexpenses`, {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },

  updateSubexpense(entryId: string, subexpenseId: string, payload: SubexpensePayload) {
    return apiRequest<ApiSubexpense>(
      `/api/v1/transactions/${entryId}/subexpenses/${subexpenseId}`,
      { method: "PUT", body: JSON.stringify(payload) },
    );
  },

  deleteSubexpense(entryId: string, subexpenseId: string) {
    return apiRequest<void>(
      `/api/v1/transactions/${entryId}/subexpenses/${subexpenseId}`,
      { method: "DELETE" },
    );
  },
};
