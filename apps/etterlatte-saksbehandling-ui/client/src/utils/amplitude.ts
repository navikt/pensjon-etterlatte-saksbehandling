import * as amplitude from '@amplitude/analytics-browser'

export enum LogEvents {
  CLICK = 'klikk',
}

let amplitudeInstance: amplitude.Types.BrowserClient | undefined = undefined

declare global {
  interface Window {
    amplitude: typeof amplitudeInstance
  }
}

const getAmplitudeKey = () => {
  if (window.location.href.includes('dev.nav.no')) return '28b46e03719df046811bfa59aa9327c5' // dev
  if (window.location.href.includes('nav.no')) return '03045b3929c5e068c5ad55324f67f384' // prod
  return '' // other e.g. localhost
}

export const initAmplitude = () => {
  if (!import.meta.env.PROD) return
  if (window.amplitude) {
    return
  }
  amplitudeInstance = amplitude.createInstance()
  amplitudeInstance.init(getAmplitudeKey(), '', {
    serverUrl: 'https://amplitude.nav.no/collect',
    ingestionMetadata: {
      sourceName: window.location.toString(),
    },
    autocapture: {
      attribution: false,
      pageViews: true,
      sessions: true,
      formInteractions: false,
      fileDownloads: false,
    },
  })

  window.amplitude = amplitudeInstance
  return amplitudeInstance
}

// Eksempelfunksjon for track av klikk
export const trackClick = (name: string) => {
  if (!amplitudeInstance) {
    console.warn('Amplitude is not initialized. Ignoring')
    return
  }
  amplitudeInstance.track({
    event_type: LogEvents.CLICK,
    event_properties: {
      tekst: name,
    },
  })
}
