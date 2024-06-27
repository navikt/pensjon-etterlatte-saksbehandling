import { useEffect, useState } from 'react'
import { IBrev } from '~shared/types/Brev'
import { useApiCall } from '~shared/hooks/useApiCall'
import Spinner from '~shared/Spinner'
import styled from 'styled-components'
import { genererPdf } from '~shared/api/brev'

import { isPendingOrInitial, isSuccess } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

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
    genererBrevPdf(
      { brevId: brev.id, behandlingId: brev.behandlingId, sakId: brev.sakId, brevtype: brev.brevtype },
      (bytes) => {
        const blob = new Blob([bytes], { type: 'application/pdf' })

        setFileURL(URL.createObjectURL(blob))
      }
    )
  }, [brev.id, brev.behandlingId])

  return (
    <Container>
      {isPendingOrInitial(pdf) && <Spinner visible={true} label="Klargjør forhåndsvisning av PDF ..." />}
      {isSuccess(pdf) && !!fileURL && <PdfViewer src={fileURL} />}
      {isFailureHandler({
        apiResult: pdf,
        errorMessage: 'En feil har oppstått ved henting av PDF',
      })}
    </Container>
  )
}

export const PdfViewer = styled.embed`
  min-width: 680px;
  width: 100%;
  min-height: 75vh;
  height: 100%;
`

const Container = styled.div`
  margin: auto;
  height: 75vh;
  width: 100%;
`
