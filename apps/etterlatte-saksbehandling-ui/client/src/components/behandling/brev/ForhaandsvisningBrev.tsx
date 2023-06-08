import { useEffect, useState } from 'react'
import { genererPdf } from '~shared/api/brev'
import { PdfVisning } from '~shared/brev/pdf-visning'
import { IBrev } from '~shared/types/Brev'

export default function ForhaandsvisningBrev({ brev }: { brev: IBrev }) {
  const [fileURL, setFileURL] = useState<string>()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string>()

  useEffect(() => {
    if (!brev.id || !brev.behandlingId) return

    genererPdf(brev.id, brev.behandlingId)
      .then((res) => {
        if (res.status === 'ok') {
          return new Blob([res.data], { type: 'application/pdf' })
        } else {
          throw Error(res.error)
        }
      })
      .then((file) => URL.createObjectURL(file!!))
      .then((url) => setFileURL(url))
      .catch((e) => setError(`Feil ved forhåndsvisning av PDF:\n${e.message}`))
      .finally(() => {
        if (fileURL) URL.revokeObjectURL(fileURL)

        setLoading(false)
      })
  }, [brev.id, brev.behandlingId])

  return <PdfVisning fileUrl={fileURL} error={error} loading={loading} />
}
