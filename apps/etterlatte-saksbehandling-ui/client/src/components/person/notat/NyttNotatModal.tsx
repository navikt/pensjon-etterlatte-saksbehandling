import { DocPencilIcon } from '@navikt/aksel-icons'
import { isPending } from '~shared/api/apiUtils'
import { Alert, Box, Button, HStack, Modal } from '@navikt/ds-react'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Notat, NotatMal, opprettNotatForSak } from '~shared/api/notat'

export const NyttNotatModal = ({ sakId, leggTilNotat }: { sakId: number; leggTilNotat: (notat: Notat) => void }) => {
  const [isOpen, setIsOpen] = useState(false)

  const [opprettNotatStatus, opprettNotat] = useApiCall(opprettNotatForSak)

  const opprettNyttNotat = () => {
    opprettNotat({ sakId, mal: NotatMal.TOM_MAL }, (notat) => {
      leggTilNotat(notat)
      setIsOpen(false)
    })
  }

  return (
    <Box>
      <Button variant="primary" icon={<DocPencilIcon />} iconPosition="right" onClick={() => setIsOpen(true)}>
        Nytt notat
      </Button>

      <Modal open={isOpen} onClose={() => setIsOpen(false)} aria-labelledby="modal-heading" width="medium">
        <Modal.Body>
          <Alert variant="info">Du oppretter nå et tomt notat</Alert>
        </Modal.Body>

        <Modal.Footer>
          <HStack gap="4" justify="end">
            <Button variant="tertiary" onClick={() => setIsOpen(false)}>
              Avbryt
            </Button>
            <Button variant="primary" onClick={opprettNyttNotat} loading={isPending(opprettNotatStatus)}>
              Opprett
            </Button>
          </HStack>
        </Modal.Footer>
      </Modal>
    </Box>
  )
}
