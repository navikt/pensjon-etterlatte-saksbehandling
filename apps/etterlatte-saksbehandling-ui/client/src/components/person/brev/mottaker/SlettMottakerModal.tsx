import { Button, Heading, HStack, Modal, VStack } from '@navikt/ds-react'
import React, { useState } from 'react'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { TrashIcon } from '@navikt/aksel-icons'
import { useApiCall } from '~shared/hooks/useApiCall'
import { slettMottaker } from '~shared/api/brev'

interface Props {
  brevId: number
  sakId: number
  mottakerId: string
  fjernMottaker: (mottakerId: string) => void
}

export function SlettMottakerModal({ brevId, sakId, mottakerId, fjernMottaker }: Props) {
  const [isOpen, setIsOpen] = useState(false)

  const [slettMottakerResult, apiSlettMottaker] = useApiCall(slettMottaker)

  const avbryt = () => {
    setIsOpen(false)
  }

  const slett = () => {
    apiSlettMottaker({ brevId, mottakerId, sakId }, () => {
      fjernMottaker(mottakerId)
      setIsOpen(false)
    })
  }

  return (
    <>
      <Button
        data-color="danger"
        variant="primary"
        onClick={() => setIsOpen(true)}
        icon={<TrashIcon title="Slett mottaker" />}
        size="small"
      />
      <Modal open={isOpen} onClose={avbryt} width="medium" aria-label="Slett mottaker">
        <Modal.Body>
          <VStack gap="space-4">
            <Heading size="large" spacing>
              Er du sikker på at du vil slette mottakeren?
            </Heading>

            {isFailureHandler({
              apiResult: slettMottakerResult,
              errorMessage: 'Kunne ikke slette mottaker.',
            })}

            <HStack gap="space-4" justify="end">
              <Button variant="secondary" disabled={isPending(slettMottakerResult)} onClick={avbryt}>
                Avbryt
              </Button>
              <Button data-color="danger" variant="primary" loading={isPending(slettMottakerResult)} onClick={slett}>
                Slett
              </Button>
            </HStack>
          </VStack>
        </Modal.Body>
      </Modal>
    </>
  )
}
