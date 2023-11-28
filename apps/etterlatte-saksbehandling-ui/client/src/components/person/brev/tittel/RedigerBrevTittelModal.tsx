import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterTittel } from '~shared/api/brev'
import React, { useState } from 'react'
import { Button, Heading, Modal, TextField } from '@navikt/ds-react'
import { DocPencilIcon } from '@navikt/aksel-icons'
import styled from 'styled-components'
import { FlexRow } from '~shared/styled'
import { ApiErrorAlert } from '~ErrorBoundary'

export default function RedigerBrevTittelModal({
  brevId,
  sakId,
  tittel,
  oppdater,
}: {
  brevId: number
  sakId: number
  tittel: string
  oppdater: (tittel: string) => void
}) {
  const [isOpen, setIsOpen] = useState(false)
  const [nyTittel, setNyTittel] = useState(tittel)

  const [status, apiOppdaterTittel] = useApiCall(oppdaterTittel)

  const lagre = () => {
    if (tittel === nyTittel) {
      setIsOpen(false) // Ikke gjør noe hvis tittel er uendret
      return
    }

    apiOppdaterTittel({ brevId, sakId, tittel: nyTittel }, () => {
      setIsOpen(false)
      oppdater(nyTittel)
    })
  }

  const avbryt = () => {
    setNyTittel(tittel)
    setIsOpen(false)
  }

  return (
    <>
      <Button
        variant="secondary"
        onClick={() => setIsOpen(true)}
        icon={<DocPencilIcon />}
        style={{ float: 'right' }}
        size="small"
      />

      <TittelModal open={isOpen} onClose={avbryt}>
        <Modal.Body>
          <Heading size="large" spacing>
            Endre tittel
          </Heading>

          <TextField label="Ny tittel" value={nyTittel || ''} onChange={(e) => setNyTittel(e.target.value)} />

          {isFailure(status) && <ApiErrorAlert>Kunne ikke oppdatere tittel...</ApiErrorAlert>}
        </Modal.Body>

        <Modal.Footer>
          <FlexRow justify="right">
            <Button variant="secondary" disabled={isPending(status)} onClick={avbryt}>
              Avbryt
            </Button>
            <Button variant="primary" loading={isPending(status)} onClick={lagre}>
              Lagre
            </Button>
          </FlexRow>
        </Modal.Footer>
      </TittelModal>
    </>
  )
}

const TittelModal = styled(Modal)`
  width: 40rem;
  padding: 3rem;
`
