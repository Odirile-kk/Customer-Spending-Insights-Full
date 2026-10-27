export const SORT_BY = ['date_desc', 'date_asc', 'amount_desc', 'amount_asc'] as const;
export type SortBy = (typeof SORT_BY)[number];

export type Period = '7d' | '30d' | '90d' | '1y';

export interface DateRange {
  startDate: string;
  endDate: string;
}

export interface TransactionQuery {
  startDate?: string;
  endDate?: string;
  category?: string;
  sortBy?: SortBy;
  limit?: number;
  offset?: number;
}

// GET /api/customers/{customerId}/profile
export interface CustomerProfile {
  customerId: string;
  name: string;
  email: string;
  joinDate: string;
  accountType: string;
  totalSpent: number;
  currency: string;
}

// GET /api/customers/{customerId}/spending/summary
export interface SpendingSummary {
  period: Period;
  totalSpent: number;
  transactionCount: number;
  averageTransaction: number;
  topCategory: string | null;
  comparedToPrevious: {
    spentChange: number;
    transactionChange: number;
  };
}

// GET /api/customers/{customerId}/spending/categories
export interface CategorySpend {
  name: string;
  amount: number;
  percentage: number;
  transactionCount: number;
  color: string;
  icon: string;
}
export interface CategoryBreakdown {
  dateRange: DateRange;
  totalAmount: number;
  categories: CategorySpend[];
}

// GET /api/customers/{customerId}/spending/trends
export interface MonthlyTrend {
  month: string;
  totalSpent: number;
  transactionCount: number;
  averageTransaction: number;
}
export interface SpendingTrends {
  trends: MonthlyTrend[];
}

// GET /api/customers/{customerId}/transactions
export interface Transaction {
  id: string;
  date: string;
  merchant: string;
  category: string;
  amount: number;
  description: string;
  paymentMethod: string;
  icon: string;
  categoryColor: string;
}
export interface TransactionPage {
  transactions: Transaction[];
  pagination: {
    total: number;
    limit: number;
    offset: number;
    hasMore: boolean;
  };
}

// GET /api/customers/{customerId}/goals
export type GoalStatus = 'on_track' | 'warning' | 'exceeded';
export interface SpendingGoal {
  id: string;
  category: string;
  monthlyBudget: number;
  currentSpent: number;
  percentageUsed: number;
  daysRemaining: number;
  status: GoalStatus;
}
export interface GoalsResponse {
  goals: SpendingGoal[];
}

// GET /api/customers/{customerId}/filters
export interface FilterCategory {
  name: string;
  color: string;
  icon: string;
}
export interface DateRangePreset {
  label: string;
  value: Period;
}
export interface FiltersResponse {
  categories: FilterCategory[];
  dateRangePresets: DateRangePreset[];
}
