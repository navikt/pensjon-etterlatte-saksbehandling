import styled from 'styled-components'
import { Modal } from '@navikt/ds-react'

/**
 * Styled ds-react modal tilpasset visning av PDF-er
 **/
export const DokumentVisningModal = styled(Modal)`
  width: 80vw;
  max-width: 1200px;
  min-height: 80vh;
`

const PdfViewer = styled.embed`
  width: 100%;
  height: 80vh;
`

interface Props {
  fileUrl: string | undefined
}

export const PdfVisning = ({ fileUrl }: Props) => {
  return !!fileUrl && <PdfViewer src={fileUrl} />
}
