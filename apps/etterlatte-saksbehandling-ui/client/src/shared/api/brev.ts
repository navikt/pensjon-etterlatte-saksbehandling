import { JournalpostResponse } from '../../components/behandling/types'

const path = process.env.REACT_APP_VEDTAK_URL

export const hentMaler = async (): Promise<any> => await fetch(`${path}/brev/maler`).then((res) => res.json())

export const hentMottakere = async (): Promise<any> => await fetch(`${path}/brev/mottakere`).then((res) => res.json())

export const hentBrevForBehandling = async (behandlingId: string): Promise<any> =>
  await fetch(`${path}/brev/behandling/${behandlingId}`).then((res) => res.json())

export const hentInnkommendeBrev = async (fnr: string): Promise<JournalpostResponse> =>
  await fetch(`${path}/brev/innkommende/${fnr}`).then((res) => res.json())

export const hentInnkommendeBrevInnhold = async (journalpostId: string, dokumentInfoId: string): Promise<Blob> =>
  await fetch(`${path}/brev/innkommende/${journalpostId}/${dokumentInfoId}`, { method: 'POST' })
    .then((res) => {
      if (res.status == 200) {
        return res.arrayBuffer()
      } else {
        throw Error(res.statusText)
      }
    })
    .then((buffer) => new Blob([buffer], { type: 'application/pdf' }))

export const nyttBrevForBehandling = async (behandlingId: string, mottaker: Mottaker, mal: any): Promise<any> =>
  await fetch(`${path}/brev/behandling/${behandlingId}`, {
    method: 'POST',
    body: JSON.stringify({ mottaker, mal }),
    headers: {
      'Content-Type': 'application/json',
    },
  }).then((res) => res.json())

export const opprettBrevFraPDF = async (behandlingId: string, mottaker: Mottaker, pdf: FormData): Promise<any> => {
    return await fetch(`${path}/brev/pdf/${behandlingId}`, {
        method: 'POST',
        body: pdf,
    }).then((res) => res.json())
}


export const opprettEllerOppdaterBrevForVedtak = async (behandlingId: string): Promise<any> =>
  await fetch(`${path}/brev/behandling/${behandlingId}/vedtak`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
  }).then((res) => res.text())

export const ferdigstillBrev = async (brevId: string): Promise<any> =>
  await fetch(`${path}/brev/${brevId}/ferdigstill`, {
    method: 'POST',
  }).then((res) => res.json())

export const slettBrev = async (brevId: string): Promise<any> =>
  await fetch(`${path}/brev/${brevId}`, {
    method: 'DELETE',
  }).then((res) => res.text())

export const genererPdf = async (brevId: string): Promise<Blob> =>
  await fetch(`${path}/brev/${brevId}/pdf`, { method: 'POST' })
    .then((res) => {
      if (res.status == 200) {
        return res.arrayBuffer()
      } else {
        throw Error(res.statusText)
      }
    })
    .then((buffer) => new Blob([buffer], { type: 'application/pdf' }))

export const hentForhaandsvisning = async (mottaker: Mottaker, mal: any): Promise<Blob> =>
  await fetch(`${path}/brev/forhaandsvisning`, {
    method: 'POST',
    body: JSON.stringify({ mottaker, mal }),
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
