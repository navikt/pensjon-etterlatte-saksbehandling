import { Button, Dropdown, Heading, Modal } from '@navikt/ds-react'
import { useState } from 'react'
import { hentDokumentPDF } from '~shared/api/dokument'
import Spinner from '~shared/Spinner'
import { PdfVisning } from '~shared/brev/pdf-visning'
import { FlexRow } from '~shared/styled'
import { Journalpost } from '~shared/types/Journalpost'
import styled from 'styled-components'

export default function DokumentModal({ journalpost }: { journalpost: Journalpost }) {
  const { tittel, journalpostId, dokumenter } = journalpost

  const [error, setError] = useState<string>()
  const [fileURL, setFileURL] = useState<string>('')
  const [isOpen, setIsOpen] = useState<boolean>(false)
  const [hasLoaded, setHasLoaded] = useState<boolean>(false)

  const open = async (dokumentInfoId: string) => {
    setIsOpen(true)

    hentDokumentPDF({ journalpostId, dokumentInfoId })
      .then((res) => {
        if (res.ok) {
          return new Blob([res.data], { type: 'application/pdf' })
        } else {
          throw Error(res.detail)
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
      {dokumenter.length > 1 ? (
        <Dropdown>
          <Button variant="secondary" size="small" as={Dropdown.Toggle}>
            Åpne
          </Button>
          <DropdownMenu>
            <Dropdown.Menu.GroupedList>
              <Dropdown.Menu.GroupedList.Heading>Velg dokument</Dropdown.Menu.GroupedList.Heading>
              <Dropdown.Menu.Divider />
              {dokumenter.map((dok) => (
                <Dropdown.Menu.GroupedList.Item key={dok.dokumentInfoId} onClick={() => open(dok.dokumentInfoId)}>
                  {dok.tittel}
                </Dropdown.Menu.GroupedList.Item>
              ))}
            </Dropdown.Menu.GroupedList>
          </DropdownMenu>
        </Dropdown>
      ) : dokumenter.length === 1 ? (
        <Button variant="secondary" size="small" onClick={() => open(dokumenter[0].dokumentInfoId)}>
          Åpne
        </Button>
      ) : null}

      <Modal open={isOpen} onClose={() => setIsOpen(false)}>
        <Modal.Header>
          <Heading spacing level="2" size="medium">
            {tittel}
          </Heading>
        </Modal.Header>

        <Modal.Body>
          <PdfVisning fileUrl={fileURL} error={error} />
          <Spinner visible={!hasLoaded} label="Laster inn PDF" />

          <FlexRow justify="right">
            <Button variant="secondary" onClick={() => setIsOpen(false)}>
              Lukk
            </Button>
          </FlexRow>
        </Modal.Body>
      </Modal>
    </>
  )
}

const DropdownMenu = styled(Dropdown.Menu)`
  width: 50ch;
`
