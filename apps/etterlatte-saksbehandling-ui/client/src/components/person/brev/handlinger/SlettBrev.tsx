import { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { slettBrev } from '~shared/api/brev'
import { BodyShort, Button, HStack, Modal } from '@navikt/ds-react'
import { TrashIcon } from '@navikt/aksel-icons'
import { isPending } from '~shared/api/apiUtils'
import { BrevStatus, IBrev } from '~shared/types/Brev'

const kanSlettes = (brev: IBrev) => {
  return !brev.behandlingId && [BrevStatus.OPPRETTET, BrevStatus.OPPDATERT].includes(brev.status)
}

/**
 * Gjør det mulig å slette brev som er nye eller under arbeid.
 * Skal ikke være mulig å slette vedtaksbrev eller brev som passert status ferdigstilt.
 **/
export const SlettBrev = ({ brev }: { brev: IBrev }) => {
  const [isOpen, setIsOpen] = useState(false)
  const [slettBrevStatus, apiSlettBrev] = useApiCall(slettBrev)

  if (!kanSlettes(brev)) {
    return null
  }

  const slett = () => {
    apiSlettBrev({ brevId: brev.id, sakId: brev.sakId }, () => {
      window.location.reload()
    })
  }

  return (
    <>
      <Button variant="danger" icon={<TrashIcon />} onClick={() => setIsOpen(true)} title="Slett brev" />

      <Modal open={isOpen} onClose={() => setIsOpen(false)} aria-label="Slett brev">
        <Modal.Body>
          <BodyShort spacing>Er du sikker på at du vil slette brevet? Handlingen kan ikke angres.</BodyShort>

          <HStack gap="4" justify="center">
            <Button variant="secondary" onClick={() => setIsOpen(false)} disabled={isPending(slettBrevStatus)}>
              Nei, avbryt
            </Button>
            <Button variant="danger" onClick={slett} loading={isPending(slettBrevStatus)}>
              Ja, slett
            </Button>
          </HStack>
        </Modal.Body>
      </Modal>
    </>
  )
}
