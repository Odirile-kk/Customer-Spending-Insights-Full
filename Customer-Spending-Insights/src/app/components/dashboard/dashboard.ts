import { HttpErrorResponse } from '@angular/common/http';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, effect, signal } from '@angular/core';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  faArrowRightArrowLeft,
  faBagShopping,
  faBolt,
  faBus,
  faCar,
  faCircleQuestion,
  faFilm,
  faHouse,
  faMagnifyingGlass,
  faMoneyBillWave,
  faPlane,
  faReceipt,
  faTags,
  faUtensils,
  faWallet,
  faHeartPulse,
} from '@fortawesome/free-solid-svg-icons';
import type { IconDefinition } from '@fortawesome/fontawesome-svg-core';
import { AuthService } from '../../core/services/auth.service';
import {
  CategorySpend,
  Period,
  SpendingGoal,
  Transaction,
} from '../../core/interface/customer.interface';
import { CustomerDashboardData, CustomerService } from '../../core/services/customer.service';

const PERIODS: Period[] = ['7d', '30d', '90d', '1y'];
const CHART_COLORS = ['#69c9c2', '#8f9eea', '#ef8fb8', '#eab96f', '#8c75d8', '#74a4df'];

@Component({
  imports: [CurrencyPipe, DatePipe, FontAwesomeModule],
  selector: 'app-dashboard',
  styleUrl: './dashboard.css',
  templateUrl: './dashboard.html',
})
export class Dashboard {
  protected readonly dashboardData = signal<CustomerDashboardData | null>(null);
  protected readonly errorMessage = signal('');
  protected readonly loading = signal(false);
  protected readonly selectedPeriod = signal<Period>('30d');
  protected readonly searchTerm = signal('');
  protected readonly periods = PERIODS;
  protected readonly spentIcon = faMoneyBillWave;
  protected readonly transactionsIcon = faArrowRightArrowLeft;
  protected readonly lifetimeIcon = faWallet;
  protected readonly topCategoryIcon = faTags;
  protected readonly searchIcon = faMagnifyingGlass;

  private readonly categoryIcons: Record<string, IconDefinition> = {
    food: faUtensils,
    dining: faUtensils,
    restaurant: faUtensils,
    grocery: faBagShopping,
    groceries: faBagShopping,
    shopping: faBagShopping,
    transport: faBus,
    transportation: faBus,
    travel: faPlane,
    flight: faPlane,
    car: faCar,
    home: faHouse,
    housing: faHouse,
    utilities: faBolt,
    bills: faReceipt,
    entertainment: faFilm,
    health: faHeartPulse,
    medical: faHeartPulse,
  };

  constructor(
    protected readonly auth: AuthService,
    private readonly customers: CustomerService,
  ) {
    effect(() => {
      if (!this.auth.isAuthenticated()) {
        this.dashboardData.set(null);
        this.loading.set(false);
        this.errorMessage.set('Sign in to load customer data.');
        return;
      }

      if (!this.auth.email()) {
        this.dashboardData.set(null);
        this.loading.set(false);
        this.errorMessage.set('An email is required to load customer data.');
        return;
      }

      this.loadCustomerData(this.selectedPeriod());
    });
  }

  protected loadCustomerData(period: Period = this.selectedPeriod()): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.dashboardData.set(null);

    this.customers.getCustomerDashboardData(period).subscribe({
      next: (data) => this.dashboardData.set(data),
      error: (error: unknown) => {
        console.error('Unable to load customer dashboard data:', error);
        this.errorMessage.set(this.getLoadErrorMessage(error));
        this.loading.set(false);
      },
      complete: () => this.loading.set(false),
    });
  }

  protected changePeriod(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    if (PERIODS.includes(value as Period)) {
      this.selectedPeriod.set(value as Period);
    }
  }

  protected updateSearch(event: Event): void {
    this.searchTerm.set((event.target as HTMLInputElement).value.trim().toLowerCase());
  }

  protected filteredTransactions(transactions: Transaction[]): Transaction[] {
    const search = this.searchTerm();
    if (!search) {
      return transactions;
    }

    return transactions.filter((transaction) =>
      `${transaction.merchant} ${transaction.category} ${transaction.description}`
        .toLowerCase()
        .includes(search),
    );
  }

  protected categoryGradient(categories: CategorySpend[]): string {
    if (!categories.length) {
      return 'conic-gradient(#e7ecea 0% 100%)';
    }

    let offset = 0;
    const slices = categories.map((category, index) => {
      const percentage = Math.max(0, Math.min(100 - offset, category.percentage));
      const start = offset;
      offset += percentage;
      return `${this.safeColor(category.color, index)} ${start}% ${offset}%`;
    });

    if (offset < 100) {
      slices.push(`#e7ecea ${offset}% 100%`);
    }

    return `conic-gradient(${slices.join(', ')})`;
  }

  protected categoryColor(category: CategorySpend, index: number): string {
    return this.safeColor(category.color, index);
  }

  protected iconForCategory(category: string, iconHint = ''): IconDefinition {
    const normalized = `${iconHint} ${category}`.toLowerCase();
    const match = Object.entries(this.categoryIcons).find(([keyword]) =>
      normalized.includes(keyword),
    );
    return match?.[1] ?? faCircleQuestion;
  }

  protected iconForTransaction(transaction: Transaction): IconDefinition {
    return this.iconForCategory(transaction.category, transaction.icon);
  }

  protected iconForGoal(goal: SpendingGoal): IconDefinition {
    return this.iconForCategory(goal.category);
  }

  protected goalProgress(goal: SpendingGoal): string {
    return `${Math.max(0, Math.min(100, goal.percentageUsed))}%`;
  }

  protected trendHeight(amount: number, trends: { totalSpent: number }[]): string {
    const maximum = Math.max(...trends.map((trend) => trend.totalSpent), 1);
    return `${Math.max(6, (amount / maximum) * 100)}%`;
  }

  private safeColor(color: string, index: number): string {
    return /^#[\da-f]{3,8}$/i.test(color) ? color : CHART_COLORS[index % CHART_COLORS.length];
  }

  private getLoadErrorMessage(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) {
      return error instanceof Error
        ? error.message
        : 'Unable to load customer data. Please try again.';
    }

    if (error.status === 0) {
      return 'Unable to reach the backend. Check that it is running and allows requests from this app.';
    }
    if (error.status === 401 || error.status === 403) {
      return 'The backend rejected the authenticated request. Check the token claims and backend email authentication.';
    }
    if (error.status === 404) {
      return 'No customer or seed data was found for the email in this token.';
    }

    return `The backend could not load customer data (HTTP ${error.status}).`;
  }
}
