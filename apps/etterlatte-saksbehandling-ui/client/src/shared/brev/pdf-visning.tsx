import { ErrorMessage } from '@navikt/ds-react'
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
  fileUrl: string | undefined
  error?: string | undefined
  loading?: boolean
}

export const PdfVisning = ({ fileUrl, error, loading }: Props) => {
  return (
    <Container>
      {error && (
        <ErrorMessage>
          En feil har oppstått ved henting av PDF:
          <br />
          <code>{error}</code>
        </ErrorMessage>
      )}

      {loading ? (
        <Spinner visible={true} label={'Klargjør forhåndsvisning av PDF ...'} />
      ) : (
        fileUrl && <PdfViewer src={`${fileUrl}#toolbar=0`} />
      )}
    </Container>
  )
}
