/** Backend base URL. Override with VITE_API_BASE_URL in a .env.local file for the frontend. */
export const API_BASE_URL: string =
  (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? 'http://localhost:8080';

/** Must match caramel.limits.max-message-chars (application.yml, default 2000). */
export const MAX_MESSAGE_CHARS = 2000;
