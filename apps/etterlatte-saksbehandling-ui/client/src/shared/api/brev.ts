import { JournalpostResponse } from '~components/behandling/types'

export const hentMaler = async (): Promise<Mal[]> => await fetch(`/api/brev/maler`).then((res) => res.json())

export const hentMottakere = async (): Promise<any> => await fetch(`/api/brev/mottakere`).then((res) => res.json())

export const hentBrevForBehandling = async (behandlingId: string): Promise<any> =>
  await fetch(`/api/brev/behandling/${behandlingId}`).then((res) => res.json())

export const nyttBrevForBehandling = async (
  behandlingId: string,
  mottaker: Mottaker,
  mal: any,
  enhet: string
): Promise<any> =>
  await fetch(`/api/brev/behandling/${behandlingId}`, {
    method: 'POST',
    body: JSON.stringify({ mottaker, mal, enhet }),
    headers: {
      'Content-Type': 'application/json',
    },
  }).then((res) => res.json())

export const opprettBrevFraPDF = async (behandlingId: string, mottaker: Mottaker, pdf: FormData): Promise<any> => {
  return await fetch(`/api/brev/pdf/${behandlingId}`, {
    method: 'POST',
    body: pdf,
  }).then((res) => {
    if (res.status == 200) {
      return res.json()
    } else {
      throw Error(res.statusText)
    }
  })
}

export const opprettEllerOppdaterBrevForVedtak = async (sak: number, behandlingId: string): Promise<any> =>
  await fetch(`/api/brev/behandling/${sak}/${behandlingId}/vedtak`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
  }).then((res) => {
    if (res.status == 200) return res.text()
    else throw Error('Feil ved oppretting/oppdatering av vedtaksbrev')
  })

export const ferdigstillBrev = async (brevId: string): Promise<any> =>
  await fetch(`/api/brev/${brevId}/ferdigstill`, {
    method: 'POST',
  }).then((res) => res.json())

export const slettBrev = async (brevId: string): Promise<any> =>
  await fetch(`/api/brev/${brevId}`, {
    method: 'DELETE',
  }).then((res) => res.text())

export const genererPdf = async (brevId: string): Promise<Blob> =>
  await fetch(`/api/brev/${brevId}/pdf`, { method: 'POST' })
    .then((res) => {
      if (res.status == 200) {
        return res.arrayBuffer()
      } else {
        throw Error(res.statusText)
      }
    })
    .then((buffer) => new Blob([buffer], { type: 'application/pdf' }))

export const hentForhaandsvisning = async (mottaker: Mottaker, mal: any, enhet: string): Promise<Blob> =>
  await fetch(`/api/brev/forhaandsvisning`, {
    method: 'POST',
    body: JSON.stringify({ mottaker, mal, enhet }),
    headers: {
      'Content-Type': 'application/json',
    },
  })
    .then((res) => {
      if (res.status == 200) {
        return res.arrayBuffer()
      } else {
        throw Error(res.statusText)
      }
    })
    .then((buffer) => new Blob([buffer], { type: 'application/pdf' }))

export const hentDokumenter = async (fnr: string): Promise<JournalpostResponse> =>
  await fetch(`/api/dokumenter/${fnr}`).then((res) => res.json())

export const hentDokumentPDF = async (journalpostId: string, dokumentInfoId: string): Promise<Blob> =>
  await fetch(`/api/dokumenter/${journalpostId}/${dokumentInfoId}`, { method: 'POST' })
    .then((res) => {
      if (res.status == 200) {
        return res.arrayBuffer()
      } else {
        throw Error(res.statusText)
      }
    })
    .then((buffer) => new Blob([buffer], { type: 'application/pdf' }))

export interface Mottaker {
  foedselsnummer?: string
  orgnummer?: string
  adresse?: Adresse
}

export interface Adresse {
  fornavn?: string
  etternavn?: string
  adresse?: string
  postnummer?: string
  poststed?: string
}

export interface Mal {
  tittel: string
  navn: string
}
