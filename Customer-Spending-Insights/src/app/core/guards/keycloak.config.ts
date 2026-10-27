import Keycloak from 'keycloak-js';
import { environment } from '../../../environments/environment';

export const keycloak = new Keycloak(environment.keycloak);
