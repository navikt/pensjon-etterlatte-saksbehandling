import { Button, Modal } from '@navikt/ds-react'
import { useState } from 'react'
import { hentDokumentPDF } from '~shared/api/brev'
import styled from 'styled-components'
import { Findout } from '@navikt/ds-icons'
import { PdfVisning } from '../behandling/brev/pdf-visning'
import Spinner from '~shared/Spinner'

const ButtonRow = styled.div`
  background: white;
  width: 100%;
  text-align: right;
`

export default function DokumentModal({
  tittel,
  journalpostId,
  dokumentInfoId,
}: {
  tittel: string
  journalpostId: string
  dokumentInfoId: string
}) {
  const [error, setError] = useState<string>()
  const [fileURL, setFileURL] = useState<string>('')
  const [isOpen, setIsOpen] = useState<boolean>(false)
  const [hasLoaded, setHasLoaded] = useState<boolean>(false)

  const open = (journalpostId: string, dokumentInfoId: string) => {
    setIsOpen(true)

    hentDokumentPDF(journalpostId, dokumentInfoId)
      .then((file) => URL.createObjectURL(file))
      .then((url) => setFileURL(url))
      .catch((e) => setError(e.message))
      .finally(() => {
        if (error) URL.revokeObjectURL(fileURL)
        setHasLoaded(true)
      })
  }

  return (
    <>
      <Button variant={'secondary'} size={'small'} onClick={() => open(journalpostId, dokumentInfoId)}>
        <Findout />
      </Button>

      <Modal open={isOpen} onClose={() => setIsOpen(false)}>
        <Modal.Content>
          <h2>{tittel}</h2>

          <PdfVisning fileUrl={fileURL} error={error} />
          <Spinner visible={!hasLoaded} label="Laster inn PDF" />

          <ButtonRow>
            <Button variant={'secondary'} onClick={() => setIsOpen(false)}>
              Lukk
            </Button>
          </ButtonRow>
        </Modal.Content>
      </Modal>
    </>
  )
}
