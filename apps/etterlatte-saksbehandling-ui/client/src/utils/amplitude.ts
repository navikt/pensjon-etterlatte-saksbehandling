import * as amplitude from '@amplitude/analytics-browser'
import { JaNei } from '~shared/types/ISvar'

export enum LogEvents {
  CLICK = 'klikk',
}

export enum ClickEvent {
  // Sak
  VIS_SAKSHISTORIKK = 'vis sakshistorikk',
  MANUELT_ENDRET_ENHET = 'manuelt endret enhet',

  //Aktivitetsplikt
  SJEKKER_SISTE_BEREGNING = 'sjekker siste beregning',

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

  // Brev
  OPPRETT_NYTT_BREV = 'opprett nytt brev',
  SLETT_BREV = 'slett brev',
  LAST_OPP_BREV = 'last opp brev',
  TILBAKESTILL_MOTTAKERE_BREV = 'tilbakestiller mottakere for brev',

  // Notat
  OPPRETT_NYTT_NOTAT = 'opprett nytt notat',
  SLETT_NOTAT = 'slett notat',
  JOURNALFOER_NOTAT = 'journalfoer notat',

  // Oppgave
  OPPRETT_GENERELL_OPPGAVE = 'opprett generell oppgave',
  OPPRETT_JOURNALFOERINGSOPPGAVE = 'opprett journalfoeringsoppgave',
  TILDEL_TILKNYTTEDE_OPPGAVER = 'tildel tilknyttede oppgaver',
  AAPNE_OPPFOELGINGSOPPGAVE_MODAL = 'åpne opprett oppfølgingsoppgave modal',
  OPPRETT_OPPFOELGINGSOPPGAVE = 'opprett oppfølgingsoppgave',

  // Avkorting
  AVKORTING_FORVENTET_INNTEKT_HJELPETEKST = 'avkorting forventet inntekt hjelpetekst',
  AVKORTING_INNVILGA_MAANEDER_HJELPETEKST = 'avkorting innvilga måneder hjelpetekst',

  // Trygdetid
  KOPIER_TRYGDETIDSGRUNNLAG_FRA_BEHANDLING_MED_SAMME_AVDOEDE = 'kopier trygdetidsgrunnlag fra behandling med samme avdoede',

  // Vilkårsvurdering
  KOPIER_VILKAAR_FRA_BEHANDLING_MED_SAMME_AVDOED = 'kopier vilkår fra behandling med samme avdoed',

  // Tilbakemeldinger
  TILBAKEMELDING_INFORMASJON_FRA_BRUKER_FOERSTEGANGSBEHANDLING = 'tilbakemelding informasjon fra bruker førstegangsbehandling',
  TILBAKEMELDING_INFORMASJON_FRA_BRUKER_REVURDERING = 'tilbakemelding informasjon fra bruker revurdering',
  TILBAKEMELDING_SAKSBEHANDLING_UTLAND_FOERSTEGANGSBEHANDLING = 'tilbakemelding saksbehandling utland førstegangsbehandling',
  TILBAKEMELDING_SAKSBEHANDLING_UTLAND_AVSLAG = 'tilbakemelding saksbehandling utland avslag',
  TILBAKEMELDING_SAKSBEHANDLING_UTLAND_SLUTTBEHANDLING = 'tilbakemelding saksbehandling utland sluttbehandling',

  // Generelt
  VIS_VARSLINGER = 'vis varslinger',
  VIS_BEHANDLING_HISTORIKK = 'vis behandling historikk',
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

export const trackClickJaNei = (name: ClickEvent, svar: JaNei) => trackClickMedSvar(name, svar)

export const trackClickMedSvar = (name: ClickEvent, svar: string) => {
  if (!amplitudeInstance) {
    console.warn('Amplitude is not initialized. Ignoring')
    return
  }
  amplitudeInstance.track({
    event_type: LogEvents.CLICK,
    event_properties: {
      tekst: name,
      svar,
    },
  })
}
