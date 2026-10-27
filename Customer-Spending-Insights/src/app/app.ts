import { Component } from '@angular/core';
import { Dashboard } from './components/dashboard/dashboard';
import { SideNav } from './components/side-nav/side-nav';

@Component({
  imports: [Dashboard, SideNav],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {}
