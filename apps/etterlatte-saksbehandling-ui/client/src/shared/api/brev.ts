const path = process.env.REACT_APP_VEDTAK_URL

export const hentBrev = async (id: string): Promise<Blob> =>
    await fetch(`${path}/pdf/${id}`)
        .then(res => {
          if (res.status == 200) {
            return res.arrayBuffer()
          } else {
            throw Error(res.statusText)
          }
        })
        .then(buffer => new Blob([buffer], { type: 'application/pdf' }))
