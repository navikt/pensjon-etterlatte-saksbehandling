import { BodyLong } from '@navikt/ds-react'
import styled from 'styled-components'
import Spinner from '~shared/Spinner'

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

interface Props {
  fileUrl?: string
  error?: string
  loading?: boolean
}

export const PdfVisning = ({ fileUrl, error, loading }: Props) => {
  return (
    <Container>
      {error && (
        <BodyLong>
          En feil har oppstått ved henting av PDF:
          <br />
          <code>{error}</code>
        </BodyLong>
      )}

      {loading ? (
        <Spinner visible={true} label={'Oppretter brev ...'} />
      ) : (
        fileUrl && <PdfViewer src={`${fileUrl}#toolbar=0`} />
      )}
    </Container>
  )
}
