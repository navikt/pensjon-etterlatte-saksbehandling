import { IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import { SakType } from '~shared/types/sak'
import { VedtakType } from '~components/vedtak/typer'
import { Spraak } from '~shared/types/Brev'
import { Journalposttype, Journalstatus, Sakstype } from '~shared/types/Journalpost'
import { Oppgavestatus } from '~shared/types/oppgave'

export const capitalize = (s?: string) => {
  if (!s) return ''
  return s.charAt(0).toUpperCase() + s.slice(1).toLowerCase()
}

export const formaterEnumTilLesbarString = (string: string): string => {
  const storForbokstav = capitalize(string.toLowerCase())
  return storForbokstav.replace('_', ' ')
}

export const formaterBehandlingstype = (behandlingstype: IBehandlingsType): string => {
  switch (behandlingstype) {
    case IBehandlingsType.FØRSTEGANGSBEHANDLING:
      return 'Førstegangsbehandling'
    case IBehandlingsType.REVURDERING:
      return 'Revurdering'
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
    case VedtakType.AVVIST_KLAGE:
      return 'Avvist klage'
    case VedtakType.INGEN_ENDRING:
      return 'Ingen endring'
  }
}

export const formaterOppgaveStatus = (status: Oppgavestatus): string => {
  switch (status) {
    case Oppgavestatus.NY:
      return 'Ny'
    case Oppgavestatus.UNDER_BEHANDLING:
      return 'Under behandling'
    case Oppgavestatus.PAA_VENT:
      return 'På vent'
    case Oppgavestatus.FERDIGSTILT:
      return 'Ferdigstilt'
    case Oppgavestatus.ATTESTERING:
      return 'Attestering'
    case Oppgavestatus.UNDERKJENT:
      return 'Underkjent'
    case Oppgavestatus.FEILREGISTRERT:
      return 'Feilregistrert'
    case Oppgavestatus.AVBRUTT:
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

export const formaterJournalpostSakstype = (sakstype: Sakstype) => {
  switch (sakstype) {
    case Sakstype.FAGSAK:
      return 'Fagsak'
    case Sakstype.GENERELL_SAK:
      return 'Generell sak'
  }
}

const norskKroneFormat = new Intl.NumberFormat('nb', {
  currency: 'nok',
})

export const NOK = (beloep: number | undefined) => (beloep == null ? '' : norskKroneFormat.format(beloep) + ' kr')

export const mapRHFArrayToStringArray = (rhfArray?: Array<{ value: string }>): string[] => {
  return !!rhfArray ? rhfArray.map((val) => val.value) : []
}
