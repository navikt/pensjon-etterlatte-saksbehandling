import { Alert, Box, Button, Dropdown, Heading, HStack, Modal } from '@navikt/ds-react'
import React, { useState } from 'react'
import { hentDokumentPDF } from '~shared/api/dokument'
import Spinner from '~shared/Spinner'
import { DokumentVisningModal, PdfVisning } from '~shared/brev/PdfVisning'
import { Journalpost } from '~shared/types/Journalpost'
import styled from 'styled-components'
import { useApiCall } from '~shared/hooks/useApiCall'
import { mapApiResult } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { ChevronDownIcon, EyeIcon } from '@navikt/aksel-icons'

export default function DokumentModal({ journalpost }: { journalpost: Journalpost }) {
  const { tittel, journalpostId, dokumenter } = journalpost

  const [fileURL, setFileURL] = useState<string>('')
  const [isOpen, setIsOpen] = useState<boolean>(false)

  const [pdfStatus, hentPdf] = useApiCall(hentDokumentPDF)

  const open = async (dokumentInfoId: string) => {
    setIsOpen(true)

    hentPdf({ journalpostId, dokumentInfoId }, (res: Blob) => {
      const url = URL.createObjectURL(new Blob([res], { type: 'application/pdf' }))

      setFileURL(url)

      setTimeout(() => {
        if (fileURL) URL.revokeObjectURL(fileURL)
      }, 1000)
    })
  }

  return (
    <>
      {dokumenter.length > 1 ? (
        <Dropdown>
          <Button icon={<ChevronDownIcon aria-hidden />} size="small" as={Dropdown.Toggle}>
            Vis
          </Button>
          <DropdownMenu>
            <Dropdown.Menu.GroupedList>
              <Dropdown.Menu.GroupedList.Heading>Velg dokument</Dropdown.Menu.GroupedList.Heading>
              <Dropdown.Menu.Divider />
              {dokumenter.map((dok, index) => (
                <HStack key={index} gap="space-4">
                  <Dropdown.Menu.GroupedList.Item
                    key={dok.dokumentInfoId}
                    onClick={() => open(dok.dokumentInfoId)}
                    disabled={!dok.dokumentvarianter[0]?.saksbehandlerHarTilgang}
                  >
                    {dok.tittel}
                  </Dropdown.Menu.GroupedList.Item>
                  {!dok.dokumentvarianter[0]?.saksbehandlerHarTilgang && (
                    <Box padding="space-4">
                      <Alert variant="warning" size="small">
                        Ikke Tilgang
                      </Alert>
                    </Box>
                  )}
                </HStack>
              ))}
            </Dropdown.Menu.GroupedList>
          </DropdownMenu>
        </Dropdown>
      ) : dokumenter[0].dokumentvarianter[0]?.saksbehandlerHarTilgang ? (
        <Button
          icon={<EyeIcon aria-hidden />}
          size="small"
          onClick={() => open(dokumenter[0].dokumentInfoId)}
          disabled={!dokumenter[0].dokumentvarianter[0]?.saksbehandlerHarTilgang}
        >
          Vis
        </Button>
      ) : (
        <Alert variant="warning" size="small">
          Ingen tilgang
        </Alert>
      )}

      <DokumentVisningModal open={isOpen} onClose={() => setIsOpen(false)} aria-label={tittel}>
        <Modal.Header>
          <Heading spacing level="2" size="medium">
            {tittel}
          </Heading>
        </Modal.Header>

        <Modal.Body>
          {mapApiResult(
            pdfStatus,
            <Spinner label="Laster inn PDF" />,
            (error) => (
              <ApiErrorAlert>{error.detail || 'Feil oppsto ved visning av dokument'}</ApiErrorAlert>
            ),
            () => (
              <PdfVisning fileUrl={fileURL} />
            )
          )}
        </Modal.Body>

        <Modal.Footer>
          <HStack justify="end">
            <Button variant="secondary" onClick={() => setIsOpen(false)}>
              Lukk
            </Button>
          </HStack>
        </Modal.Footer>
      </DokumentVisningModal>
    </>
  )
}

const DropdownMenu = styled(Dropdown.Menu)`
  width: 50ch;
`
