import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
// Self-hosted fonts (UI-NFR-5). wght.css declares font-display: swap and per-subset unicode-range.
import '@fontsource-variable/figtree/wght.css';
import '@fontsource-variable/jetbrains-mono/wght.css';
import './styles/app.css';
import { App } from './App';

createRoot(document.getElementById('root') as HTMLElement).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
