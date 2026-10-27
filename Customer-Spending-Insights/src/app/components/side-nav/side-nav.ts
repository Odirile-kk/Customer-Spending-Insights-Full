import { Component, signal } from '@angular/core';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  faArrowRightFromBracket,
  faArrowRightToBracket,
  faChartLine,
  faGaugeHigh,
  faLayerGroup,
  faReceipt,
  faBullseye,
} from '@fortawesome/free-solid-svg-icons';
import { AuthService } from '../../core/services/auth.service';

@Component({
  imports: [FontAwesomeModule],
  selector: 'app-side-nav',
  styleUrl: './side-nav.css',
  templateUrl: './side-nav.html',
})
export class SideNav {
  protected readonly errorMessage = signal('');
  protected readonly brandIcon = faChartLine;
  protected readonly dashboardIcon = faGaugeHigh;
  protected readonly trendsIcon = faChartLine;
  protected readonly transactionsIcon = faReceipt;
  protected readonly categoriesIcon = faLayerGroup;
  protected readonly goalsIcon = faBullseye;
  protected readonly loginIcon = faArrowRightToBracket;
  protected readonly logoutIcon = faArrowRightFromBracket;

  constructor(protected readonly auth: AuthService) {}

  protected async login(): Promise<void> {
    this.errorMessage.set('');
    try {
      await this.auth.login();
    } catch (error) {
      console.error('Keycloak login failed:', error);
      this.errorMessage.set('Unable to start sign in. Please try again.');
    }
  }

  protected async logout(): Promise<void> {
    this.errorMessage.set('');
    try {
      await this.auth.logout();
    } catch (error) {
      console.error('Keycloak logout failed:', error);
      this.errorMessage.set('Unable to sign out. Please try again.');
    }
  }
}
