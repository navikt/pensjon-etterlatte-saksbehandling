import { format } from 'date-fns'
import { IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import { SakType } from '~shared/types/sak'
import { VedtakType } from '~components/vedtak/typer'
import { Oppgavestatus } from '~shared/api/oppgaver'
import { Spraak } from '~shared/types/Brev'
import { Journalposttype, Journalstatus } from '~shared/types/Journalpost'

export const capitalize = (s?: string) => {
  if (!s) return ''
  return s.charAt(0).toUpperCase() + s.slice(1).toLowerCase()
}

export const formaterEnumTilLesbarString = (string: string): string => {
  const storForbokstav = capitalize(string.toLowerCase())
  return storForbokstav.replace('_', ' ')
}

export const formaterDato = (dato: Date) => format(dato, 'dd.MM.yyyy').toString()

export const formaterStringDato = (dato: string) => format(new Date(dato), 'dd.MM.yyyy').toString()
export const formaterKanskjeStringDato = (dato?: string): string =>
  formaterKanskjeStringDatoMedFallback('Ukjent dato', dato)

export const formaterKanskjeStringDatoMedFallback = (fallback: string, dato?: string): string =>
  dato ? formaterStringDato(dato) : fallback

export const formaterTidspunktTimeMinutterSekunder = (dato: Date) => format(new Date(dato), 'HH:mm:ss').toString()

export const formaterStringTidspunktTimeMinutter = (dato: string) => format(new Date(dato), 'HH:mm').toString()

export const formaterDatoMedTidspunkt = (dato: Date) => format(new Date(dato), 'dd.MM.yyyy HH:mm').toString()

export const formaterDatoMedKlokkeslett = (dato: Date | string) =>
  format(new Date(dato), "dd.MM.yyyy 'kl.' HH:mm").toString()

export const formaterBehandlingstype = (behandlingstype: IBehandlingsType): string => {
  switch (behandlingstype) {
    case IBehandlingsType.FØRSTEGANGSBEHANDLING:
      return 'Førstegangsbehandling'
    case IBehandlingsType.REVURDERING:
      return 'Revurdering'
    case IBehandlingsType.MANUELT_OPPHOER:
      return 'Manuelt opphør'
  }
}

export const formaterSakstype = (sakstype: SakType): string => {
  switch (sakstype) {
    case SakType.BARNEPENSJON:
      return 'Barnepensjon'
    case SakType.OMSTILLINGSSTOENAD:
      return 'Omstillingsstønad'
  }
}

export const formaterVedtakType = (type: VedtakType): string => {
  switch (type) {
    case VedtakType.INNVILGELSE:
      return 'Innvilgelse'
    case VedtakType.OPPHOER:
      return 'Opphør'
    case VedtakType.AVSLAG:
      return 'Avslag'
    case VedtakType.ENDRING:
      return 'Endring'
    case VedtakType.TILBAKEKREVING:
      return 'Tilbakekreving'
  }
}

export const formaterOppgaveStatus = (status: Oppgavestatus): string => {
  switch (status) {
    case 'NY':
      return 'Ny'
    case 'UNDER_BEHANDLING':
      return 'Under behandling'
    case 'PAA_VENT':
      return 'På Vent'
    case 'FERDIGSTILT':
      return 'Ferdigstilt'
    case 'FEILREGISTRERT':
      return 'Feilregistrert'
    case 'AVBRUTT':
      return 'Avbrutt'
  }
}

export const formaterFnr = (fnr: string) => {
  if (fnr.length === 11) return fnr.replace(/\d{6}(?=.)/g, '$& ')
  return fnr
}

export const formaterSpraak = (spraak: Spraak) => {
  switch (spraak) {
    case Spraak.NB:
      return 'Bokmål'
    case Spraak.NN:
      return 'Nynorsk'
    case Spraak.EN:
      return 'Engelsk'
  }
}

export const formaterJournalpostType = (type: Journalposttype) => {
  switch (type) {
    case Journalposttype.I:
      return 'Inngående'
    case Journalposttype.U:
      return 'Utgående'
    case Journalposttype.N:
      return 'Notat'
    default:
      return 'Ukjent'
  }
}

export const formaterJournalpostStatus = (status: Journalstatus) => {
  switch (status) {
    case Journalstatus.MOTTATT:
      return 'Mottatt'
    case Journalstatus.JOURNALFOERT:
      return 'Journalført'
    case Journalstatus.FERDIGSTILT:
      return 'Ferdigstilt'
    case Journalstatus.EKSPEDERT:
      return 'Ekspedert'
    case Journalstatus.UNDER_ARBEID:
      return 'Under arbeid'
    case Journalstatus.FEILREGISTRERT:
      return 'Feilregistrert'
    case Journalstatus.UTGAAR:
      return 'Utgår'
    case Journalstatus.AVBRUTT:
      return 'Avbrutt'
    case Journalstatus.UKJENT_BRUKER:
      return 'Ukjent bruker'
    case Journalstatus.RESERVERT:
      return 'Reservert'
    case Journalstatus.OPPLASTING_DOKUMENT:
      return 'Opplasting dokument'
    case Journalstatus.UKJENT:
      return 'Ukjent'
  }
}

export enum DatoFormat {
  AAR_MAANED_DAG = 'yyyy-MM-dd',
  DAG_MAANED_AAR = 'dd.MM.yyyy',
}
const norskKroneFormat = new Intl.NumberFormat('NO-nb', {
  currency: 'nok',
})
export const NOK = (beloep: number | undefined) => (beloep == null ? '' : norskKroneFormat.format(beloep) + ' kr')
