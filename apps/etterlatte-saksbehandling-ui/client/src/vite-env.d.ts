/// <reference types="vite/client" />
export {}

declare global {
  interface Window {
    umami: { track: (eventName: string, eventData?: string | object) => void }
  }
}
