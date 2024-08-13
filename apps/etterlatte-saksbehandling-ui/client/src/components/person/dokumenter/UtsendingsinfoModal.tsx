import React, { useState } from 'react'
import { Journalpost } from '~shared/types/Journalpost'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentUtsendingsinfo } from '~shared/api/dokument'
import { Button, Heading, Modal } from '@navikt/ds-react'
import { InformationSquareIcon } from '@navikt/aksel-icons'
import { mapApiResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Info } from '~components/behandling/soeknadsoversikt/Info'

export const UtsendingsinfoModal = ({ journalpost }: { journalpost: Journalpost }) => {
  const [isOpen, setIsOpen] = useState(false)

  const [status, apiHentUtsendingsinfo] = useApiCall(hentUtsendingsinfo)

  const open = () => {
    setIsOpen(true)
    apiHentUtsendingsinfo({ journalpostId: journalpost.journalpostId })
  }

  return (
    <>
      <Button variant="tertiary" title="Utsendingsinfo" size="small" icon={<InformationSquareIcon />} onClick={open} />

      <Modal open={isOpen} onClose={() => setIsOpen(false)} aria-label="Utsendingsinfo">
        <Modal.Header>
          <Heading size="medium">Utsendingsinfo</Heading>
        </Modal.Header>

        <Modal.Body>
          {mapApiResult(
            status,
            <Spinner label="Henter utsendingsinfo" />,
            (error) => (
              <ApiErrorAlert>{error.detail || 'Feil ved henting av utsendingsinfo'}</ApiErrorAlert>
            ),
            (result) => (
              <>
                {!result.utsendingsinfo && <i>Ingen utsendingsinformasjon på journalposten</i>}

                {result.utsendingsinfo?.fysiskpostSendt?.adressetekstKonvolutt && (
                  <Info
                    label="Adressetekst konvolutt"
                    tekst={
                      <div style={{ whiteSpace: 'pre-wrap' }}>
                        {result.utsendingsinfo?.fysiskpostSendt?.adressetekstKonvolutt}
                      </div>
                    }
                  />
                )}

                {result.utsendingsinfo?.digitalpostSendt?.adresse && (
                  <Info
                    label="Adressetekst konvolutt"
                    tekst={
                      <div style={{ whiteSpace: 'pre-wrap' }}>{result.utsendingsinfo?.digitalpostSendt?.adresse}</div>
                    }
                  />
                )}
              </>
            )
          )}
        </Modal.Body>
      </Modal>
    </>
  )
}
