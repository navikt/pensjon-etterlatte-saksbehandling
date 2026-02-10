import { Notat, slettNotat } from '~shared/api/notat'
import { Alert, Button, Heading, HStack, Modal } from '@navikt/ds-react'
import { TrashIcon } from '@navikt/aksel-icons'
import { isPending } from '~shared/api/apiUtils'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ClickEvent, trackClick } from '~utils/analytics'

export const SlettNotatModal = ({ notat, fjernNotat }: { notat: Notat; fjernNotat: (id: number) => void }) => {
  const [isOpen, setIsOpen] = useState(false)

  const [slettStatus, apiSlettNotat] = useApiCall(slettNotat)

  const slett = () => {
    trackClick(ClickEvent.SLETT_NOTAT)

    apiSlettNotat(notat.id, () => {
      fjernNotat(notat.id)
    })
  }

  return (
    <>
      <Button variant="danger" size="small" icon={<TrashIcon aria-hidden />} onClick={() => setIsOpen(true)}>
        Slett
      </Button>

      <Modal open={isOpen} onClose={() => setIsOpen(false)} aria-labelledby="modal-heading">
        <Modal.Body>
          <Heading size="xsmall">{notat.tittel}</Heading>

          <Alert variant="warning">Er du sikker på at du vil slette notatet? Denne handlingen kan ikke angres.</Alert>
        </Modal.Body>

        <Modal.Footer>
          <HStack gap="space-4" align="center">
            <Button variant="tertiary" onClick={() => setIsOpen(false)}>
              Lukk
            </Button>

            <Button variant="danger" icon={<TrashIcon aria-hidden />} onClick={slett} loading={isPending(slettStatus)}>
              Slett
            </Button>
          </HStack>
        </Modal.Footer>
      </Modal>
    </>
  )
}
