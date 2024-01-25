import { Journalpost } from '~shared/types/Journalpost'
import { fnrHarGyldigFormat } from '~utils/fnr'

export const kanFerdigstilleJournalpost = (journalpost: Journalpost): [boolean, string[]] => {
  const feilmeldinger = []

  if (!journalpost.sak?.fagsakId) {
    feilmeldinger.push('Sakid er ikke satt')
  }
  if (!journalpost.sak?.sakstype) {
    feilmeldinger.push('Sakstype mangler')
  }
  if (!journalpost.sak?.fagsaksystem) {
    feilmeldinger.push('Fagsaksystem er ikke satt')
  }

  if (!journalpost.avsenderMottaker.id && !journalpost.avsenderMottaker.navn) {
    feilmeldinger.push('Avsender/mottaker må ha navn og/eller ID')
  }
  if (!journalpost.avsenderMottaker.navn && !fnrHarGyldigFormat(journalpost.avsenderMottaker.id)) {
    feilmeldinger.push('Avsender/mottaker må ha et gyldig fnr. hvis navn ikke er satt')
  }

  if (!journalpost.tema) {
    feilmeldinger.push('Tema mangler')
  }

  if (!journalpost.tittel) {
    feilmeldinger.push('Journalpostens tittel mangler')
  }

  if (!journalpost.dokumenter) {
    feilmeldinger.push('Journalposten er tom (ingen dokumenter)')
  } else if (journalpost.dokumenter.find((dok) => !dok.tittel)) {
    feilmeldinger.push('Et eller flere dokumenter mangler tittel')
  }

  return [!feilmeldinger.length, feilmeldinger]
}

export const temaTilhoererGjenny = (journalpost: Journalpost) => {
  return journalpost.tema === 'EYO' || journalpost.tema === 'EYB'
}

export const kanEndreJournalpost = (journalpost: Journalpost | undefined) => {
  return (
    !!journalpost && ['MOTTATT', 'UNDER_ARBEID'].includes(journalpost.journalstatus) && temaTilhoererGjenny(journalpost)
  )
}
