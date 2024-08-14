import { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { PdfViewer } from '~components/behandling/brev/ForhaandsvisningBrev'
import { genererNotatPdf } from '~shared/api/notat'
import { ApiErrorAlert } from '~ErrorBoundary'

export default function ForhaandsvisningNotat({ id }: { id: number }) {
  const [fileURL, setFileURL] = useState<string>()
  const [pdfStatus, genererPdf] = useApiCall(genererNotatPdf)

  useEffect(() => {
    if (!!fileURL)
      setTimeout(() => {
        URL.revokeObjectURL(fileURL)
      }, 1000)
  }, [fileURL])

  useEffect(() => {
    genererPdf(id, (bytes) => {
      const blob = new Blob([bytes], { type: 'application/pdf' })

      setFileURL(URL.createObjectURL(blob))
    })
  }, [id])

  return mapResult(pdfStatus, {
    pending: <Spinner label="Klargjør forhåndsvisning av PDF ..." />,
    error: (error) => <ApiErrorAlert>{error.detail || 'Ukjent feil oppsto'}</ApiErrorAlert>,
    success: () => !!fileURL && <PdfViewer src={fileURL} />,
  })
}
