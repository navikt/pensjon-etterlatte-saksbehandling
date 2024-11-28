import * as amplitude from '@amplitude/analytics-browser'

export enum LogEvents {
  CLICK = 'klikk',
}

export enum ClickEvent {
  // Vilkaarsvurdering
  SLETT_VILKAARSVURDERING = 'slett vilkaarsvurdering',

  // Gosys
  FERDIGSTILL_GOSYS_OPPGAVE = 'ferdigstill gosys oppgave',
  FLYTT_GOSYS_OPPGAVE = 'flytt gosys oppgave',

  // Journalpost
  FEILREGISTRER_JOURNALPOST = 'feilregistrer journalpost',
  OPPHEV_FEILREGISTRERING_JOURNALPOST = 'opphev feilregistrering journalpost',
  FLYTT_JOURNALPOST = 'flytt journalpost',
  KNYTT_JOURNALPOST_TIL_ANNEN_SAK = 'knytt journalpost til annen sak',
  OPPRETT_JOURNALFOERINGSOPPGAVE = 'opprett journalfoeringsoppgave',

  // Brev
  OPPRETT_NYTT_BREV = 'opprett nytt brev',
  SLETT_BREV = 'slett brev',
  LAST_OPP_BREV = 'last opp brev',

  // Notat
  OPPRETT_NYTT_NOTAT = 'opprett nytt notat',
  SLETT_NOTAT = 'slett notat',
  JOURNALFOER_NOTAT = 'journalfoer notat',

  // Avkorting
  AVKORTING_FORVENTET_INNTEKT_HJELPETEKST = 'avkorting forventet inntekt hjelpetekst',
  AVKORTING_INNVILGA_MAANEDER_HJELPETEKST = 'avkorting innvilga måneder hjelpetekst',

  // Trygdetid
  KOPIER_TRYGDETIDSGRUNNLAG_FRA_BEHANDLING_MED_SAMME_AVDOEDE = 'kopier trygdetidsgrunnlag fra behandling med samme avdoede',
  IKKE_KOPIER_TRYGDETIDSGRUNNLAG_FRA_BEHANDLING_MED_SAMME_AVDOEDE = 'ikke kopier trygdetidsgrunnlag fra behandling med samme avdoede',
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

export const trackClick = (name: ClickEvent) => {
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
