const path = process.env.REACT_APP_VEDTAK_URL

export const hentBrev = async (id: string): Promise<Blob> =>
    await fetch(`${path}/pdf/${id}`)
        .then(res => res.arrayBuffer())
        .then(buffer => new Blob([buffer], { type: 'application/pdf' }))
