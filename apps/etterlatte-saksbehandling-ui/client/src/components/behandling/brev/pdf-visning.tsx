import { BodyLong } from '@navikt/ds-react'
import styled from 'styled-components'

const PdfViewer = styled.iframe`
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

export const PdfVisning = ({ fileUrl, error }: { fileUrl?: string; error?: string }) => {
  return (
    <Container>
      {error && (
        <BodyLong>
          En feil har oppstått ved henting av PDF:
          <br />
          <code>{error}</code>
        </BodyLong>
      )}

      <>{fileUrl && <PdfViewer src={fileUrl} />}</>
    </Container>
  )
}
