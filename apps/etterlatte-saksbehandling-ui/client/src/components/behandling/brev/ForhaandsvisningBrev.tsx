import { useEffect, useState } from 'react'
import { IBrev } from '~shared/types/Brev'
import { isFailure, isPendingOrInitial, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { Alert } from '@navikt/ds-react'
import Spinner from '~shared/Spinner'
import styled from 'styled-components'
import { genererPdf } from '~shared/api/brev'

export default function ForhaandsvisningBrev({ brev }: { brev: IBrev }) {
  const [fileURL, setFileURL] = useState<string>()
  const [pdf, genererBrevPdf] = useApiCall(genererPdf)

  useEffect(() => {
    if (!!fileURL)
      setTimeout(() => {
        URL.revokeObjectURL(fileURL)
      }, 1000)
  }, [fileURL])

  useEffect(() => {
    genererBrevPdf({ brevId: brev.id, behandlingId: brev.behandlingId, sakId: brev.sakId }, (bytes) => {
      const blob = new Blob([bytes], { type: 'application/pdf' })

      setFileURL(URL.createObjectURL(blob))
    })
  }, [brev.id, brev.behandlingId])

  return (
    <Container>
      {isPendingOrInitial(pdf) && <Spinner visible={true} label="Klargjør forhåndsvisning av PDF ..." />}
      {isSuccess(pdf) && !!fileURL && <PdfViewer src={`${fileURL}#toolbar=0`} />}
      {isFailure(pdf) && (
        <Alert variant="error">
          En feil har oppstått ved henting av PDF: <code>{JSON.stringify(pdf.error)}</code>
        </Alert>
      )}
    </Container>
  )
}

const PdfViewer = styled.embed`
  min-width: 680px;
  width: 100%;
  min-height: 600px;
  height: 100%;
`

const Container = styled.div`
  margin: auto;
  height: 100%;
  width: 100%;
`
