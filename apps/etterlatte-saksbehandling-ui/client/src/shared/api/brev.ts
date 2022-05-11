const path = process.env.REACT_APP_VEDTAK_URL

export const hentAlleBrev = async (behandlingId: string): Promise<any> =>
    await fetch(`${path}/brev/${behandlingId}`)
        .then(res => res.json())

export const opprettBrev = async (behandlingId: string, mottaker: any): Promise<any> =>
    await fetch(`${path}/brev/${behandlingId}`, {
      method: 'POST',
      body: JSON.stringify(mottaker),
      headers: {
        'Content-Type': 'application/json'
      }
    })
        .then(res => res.json())

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
