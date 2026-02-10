import { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { slettBrev } from '~shared/api/brev'
import { BodyShort, Button, HStack, Modal } from '@navikt/ds-react'
import { TrashIcon } from '@navikt/aksel-icons'
import { isPending } from '~shared/api/apiUtils'
import { BrevStatus, IBrev } from '~shared/types/Brev'
import { ClickEvent, trackClick } from '~utils/analytics'
import { gyldigbrevkode } from '~components/person/brev/BrevOversikt'

const kanSlettes = (brev: IBrev) => {
  return (
    !brev.behandlingId &&
    [BrevStatus.OPPRETTET, BrevStatus.OPPDATERT].includes(brev.status) &&
    gyldigbrevkode(brev.brevkoder)
  )
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
    trackClick(ClickEvent.SLETT_BREV)

    apiSlettBrev({ brevId: brev.id, sakId: brev.sakId }, () => {
      window.location.reload()
    })
  }

  return (
    <>
      <Button
        variant="danger"
        icon={<TrashIcon aria-hidden />}
        onClick={() => setIsOpen(true)}
        title="Slett brev"
        size="small"
      >
        Slett
      </Button>

      <Modal open={isOpen} onClose={() => setIsOpen(false)} aria-label="Slett brev">
        <Modal.Body>
          <BodyShort spacing>Er du sikker på at du vil slette brevet? Handlingen kan ikke angres.</BodyShort>

          <HStack gap="space-4" justify="center">
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
