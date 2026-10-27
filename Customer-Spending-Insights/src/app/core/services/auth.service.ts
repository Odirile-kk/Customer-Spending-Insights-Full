import { Injectable, signal } from '@angular/core';
import { keycloak } from '../guards/keycloak.config';

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly isAuthenticated = signal(keycloak.authenticated === true);
  readonly email = signal('');

  constructor() {
    keycloak.onAuthSuccess = () => this.syncAuthentication();
    keycloak.onAuthRefreshSuccess = () => this.syncAuthentication();
    keycloak.onAuthLogout = () => this.syncAuthentication();
    keycloak.onAuthRefreshError = () => this.syncAuthentication();
    this.syncAuthentication();
  }

  login(): Promise<void> {
    return keycloak.login({ redirectUri: window.location.href });
  }

  logout(): Promise<void> {
    return keycloak.logout({
      redirectUri: window.location.origin,
    });
  }

  async getValidToken(): Promise<string | undefined> {
    if (!keycloak.authenticated) {
      return undefined;
    }

    await keycloak.updateToken(30);
    return keycloak.token;
  }

  private syncAuthentication(): void {
    this.isAuthenticated.set(keycloak.authenticated === true);
    this.email.set(keycloak.tokenParsed?.['email'] ?? '');
  }
}
