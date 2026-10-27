import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { forkJoin, from, map, Observable, switchMap, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CategoryBreakdown,
  CustomerProfile,
  DateRange,
  FiltersResponse,
  GoalsResponse,
  Period,
  SpendingSummary,
  SpendingTrends,
  TransactionQuery,
  TransactionPage,
} from '../interface/customer.interface';
import { AuthService } from './auth.service';

export interface CustomerDashboardData {
  profile: CustomerProfile;
  summary: SpendingSummary;
  categories: CategoryBreakdown;
  trends: SpendingTrends;
  transactions: TransactionPage;
  goals: GoalsResponse;
  filters: FiltersResponse;
}

@Injectable({
  providedIn: 'root',
})
export class CustomerService {
  private readonly customersPath = `${environment.apiBaseUrl}/customers`;

  constructor(
    private http: HttpClient,
    private auth: AuthService,
  ) {}

  public getTransactions(
    customerId: string,
    query: TransactionQuery = {},
  ): Observable<TransactionPage> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== '') {
        params = params.set(key, value);
      }
    }

    return this.http.get<TransactionPage>(`${this.customerPath(customerId)}/transactions`, {
      params,
    });
  }

  public getSpendingTrends(customerId: string): Observable<SpendingTrends> {
    return this.http.get<SpendingTrends>(`${this.customerPath(customerId)}/spending/trends`);
  }

  public getSpendingSummary(
    customerId: string,
    period: Period = '30d',
  ): Observable<SpendingSummary> {
    const params = new HttpParams().set('period', period);
    return this.http.get<SpendingSummary>(`${this.customerPath(customerId)}/spending/summary`, {
      params,
    });
  }

  public getCategoryBreakdown(
    customerId: string,
    dateRange?: DateRange,
  ): Observable<CategoryBreakdown> {
    let params = new HttpParams();
    if (dateRange) {
      params = params.set('startDate', dateRange.startDate).set('endDate', dateRange.endDate);
    }

    return this.http.get<CategoryBreakdown>(
      `${this.customerPath(customerId)}/spending/categories`,
      { params },
    );
  }

  public getCustomerProfile() {
    return from(this.auth.getValidToken()).pipe(
      switchMap((token) => {
        if (!token) {
          return throwError(() => new Error('No Keycloak access token is available.'));
        }

        const headers = new HttpHeaders({
          Authorization: `Bearer ${token}`,
        });

        return this.http.get<CustomerProfile>(`${this.customersPath}/current/profile`, { headers });
      }),
      tap((profile) => console.log('Current customer profile:', profile)),
    );
  }

  public getGoals(customerId: string): Observable<GoalsResponse> {
    return this.http.get<GoalsResponse>(`${this.customerPath(customerId)}/goals`);
  }

  public getFilters(customerId: string): Observable<FiltersResponse> {
    return this.http.get<FiltersResponse>(`${this.customerPath(customerId)}/filters`);
  }

  public getCustomerDashboardData(period: Period = '30d'): Observable<CustomerDashboardData> {
    return this.getCustomerProfile().pipe(
      switchMap((profile) => {
        const customerId = profile.customerId?.trim();
        if (!customerId) {
          return throwError(
            () => new Error('The current-customer profile did not return a customer ID.'),
          );
        }

        return forkJoin({
          summary: this.getSpendingSummary(customerId, period),
          categories: this.getCategoryBreakdown(customerId),
          trends: this.getSpendingTrends(customerId),
          transactions: this.getTransactions(customerId),
          goals: this.getGoals(customerId),
          filters: this.getFilters(customerId),
        }).pipe(map((data) => ({ ...data, profile })));
      }),
    );
  }

  private customerPath(customerId: string): string {
    return `${this.customersPath}/${encodeURIComponent(customerId)}`;
  }
}
