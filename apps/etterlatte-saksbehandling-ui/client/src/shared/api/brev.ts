const path = process.env.REACT_APP_VEDTAK_URL

export const hentMaler = async (): Promise<any> =>
    await fetch(`${path}/brev/maler`).then(res => res.json())

export const hentBrevForBehandling = async (behandlingId: string): Promise<any> =>
    await fetch(`${path}/brev/behandling/${behandlingId}`)
        .then(res => res.json())

export const nyttBrevForBehandling = async (behandlingId: string, mottaker: any, mal: any): Promise<any> =>
    await fetch(`${path}/brev/behandling/${behandlingId}`, {
      method: 'POST',
      body: JSON.stringify({ mottaker, mal }),
      headers: {
        'Content-Type': 'application/json'
      }
    }).then(res => res.json())

export const ferdigstillBrev = async (brevId: string): Promise<any> =>
    await fetch(`${path}/brev/${brevId}/ferdigstill`, {
      method: 'POST'
    }).then(res => res.json())

export const slettBrev = async (brevId: string): Promise<any> =>
    await fetch(`${path}/brev/${brevId}`, {
      method: 'DELETE'
    }).then(res => res.text())

export const genererPdf = async (brevId: string): Promise<Blob> =>
    await fetch(`${path}/brev/${brevId}/pdf`, { method: 'POST' })
        .then(res => {
          if (res.status == 200) {
            return res.arrayBuffer()
          } else {
            throw Error(res.statusText)
          }
        })
        .then(buffer => new Blob([buffer], { type: 'application/pdf' }))
