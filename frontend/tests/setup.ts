import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

// Vitest runs with globals off, so React Testing Library cannot register its own cleanup.
// Unmount rendered trees after each test so one test's DOM never leaks into the next.
afterEach(() => {
  cleanup();
});
