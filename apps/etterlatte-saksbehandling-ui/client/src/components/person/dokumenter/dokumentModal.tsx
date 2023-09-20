import { Button, Heading, Modal } from '@navikt/ds-react'
import { useState } from 'react'
import { hentDokumentPDF } from '~shared/api/dokument'
import Spinner from '~shared/Spinner'
import { PdfVisning } from '~shared/brev/pdf-visning'
import { FlexRow } from '~shared/styled'

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

  const open = async (journalpostId: string, dokumentInfoId: string) => {
    setIsOpen(true)

    hentDokumentPDF({ journalpostId, dokumentInfoId })
      .then((res) => {
        if (res.status === 'ok') {
          return new Blob([res.data], { type: 'application/pdf' })
        } else {
          throw Error(res.error)
        }
      })
      .then((file) => URL.createObjectURL(file!!))
      .then((url) => setFileURL(url))
      .catch((e) => setError(e.message))
      .finally(() => {
        if (fileURL) URL.revokeObjectURL(fileURL)

        setHasLoaded(true)
      })
  }

  return (
    <>
      <Button variant={'secondary'} size={'small'} onClick={() => open(journalpostId, dokumentInfoId)}>
        Åpne dokument
      </Button>

      <Modal open={isOpen} onClose={() => setIsOpen(false)}>
        <Modal.Header>
          <Heading spacing level={'2'} size={'medium'}>
            {tittel}
          </Heading>
        </Modal.Header>

        <Modal.Body>
          <PdfVisning fileUrl={fileURL} error={error} />
          <Spinner visible={!hasLoaded} label="Laster inn PDF" />

          <FlexRow justify={'right'}>
            <Button variant={'secondary'} onClick={() => setIsOpen(false)}>
              Lukk
            </Button>
          </FlexRow>
        </Modal.Body>
      </Modal>
    </>
  )
}
