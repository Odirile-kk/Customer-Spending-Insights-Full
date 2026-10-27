import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';
import { keycloak } from './app/core/guards/keycloak.config';

async function startApp(): Promise<void> {
  try {
    await keycloak.init({
      onLoad: 'login-required',
      pkceMethod: 'S256',
      checkLoginIframe: false,
    });
  } catch (error) {
    console.error('Keycloak initialization failed. The app will start unauthenticated:', error);
  }

  await bootstrapApplication(App, appConfig);
}

void startApp();
