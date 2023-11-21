import { Alert, BodyLong, Button, Heading, Modal, Panel } from '@navikt/ds-react'
import { BrevStatus, IBrev } from '~shared/types/Brev'
import { isPending, useApiCall } from '~shared/hooks/useApiCall'
import { useState } from 'react'
import { distribuerBrev, ferdigstillBrev, journalfoerBrev } from '~shared/api/brev'
import { FlexRow } from '~shared/styled'

interface Props {
  brev: IBrev
  setKanRedigeres: (kanRedigeres: boolean) => void
}

export default function NyttBrevHandlingerPanel({ brev, setKanRedigeres }: Props) {
  const [isOpen, setIsOpen] = useState(false)

  const [ferdigstillStatus, apiFerdigstillBrev] = useApiCall(ferdigstillBrev)
  const [journalfoerStatus, apiJournalfoerBrev] = useApiCall(journalfoerBrev)
  const [distribuerStatus, apiDistribuerBrev] = useApiCall(distribuerBrev)

  const [error, setError] = useState<string>()

  const handleError = (msg: string) => {
    setError(msg)
    setIsOpen(false)
  }

  const ferdigstill = () => {
    setKanRedigeres(false)

    apiFerdigstillBrev(
      {
        brevId: brev.id,
        sakId: brev.sakId,
      },
      journalfoer,
      () => handleError('Feil oppsto ved ferdigstilling av brev')
    )
  }

  const journalfoer = () => {
    apiJournalfoerBrev({ brevId: brev.id, sakId: brev.sakId }, distribuer, () =>
      handleError('Feil oppsto ved journalføring av brev')
    )
  }

  const distribuer = () => {
    apiDistribuerBrev(
      { brevId: brev.id, sakId: brev.sakId },
      () => {
        setTimeout(() => window.location.reload(), 2000)
      },
      () => handleError('Feil oppsto ved distribusjon av brev')
    )
  }

  if (brev.status === BrevStatus.DISTRIBUERT) {
    return null
  }

  return (
    <Panel>
      <Heading spacing level="2" size="medium">
        Handlinger
      </Heading>

      {error && <Alert variant="error">{error}</Alert>}

      {brev.status === BrevStatus.FERDIGSTILT ? (
        <Button variant="primary" onClick={journalfoer} loading={isPending(journalfoerStatus)}>
          Journalfør
        </Button>
      ) : brev.status === BrevStatus.JOURNALFOERT ? (
        <Button variant="primary" onClick={distribuer} loading={isPending(distribuerStatus)}>
          Distribuer
        </Button>
      ) : (
        <>
          <Button variant="primary" onClick={() => setIsOpen(true)} loading={isPending(ferdigstillStatus)}>
            Ferdigstill
          </Button>

          <Modal
            open={isOpen}
            onClose={() => setIsOpen(false)}
            aria-labelledby="modal-heading"
            className="padding-modal"
          >
            <Modal.Body style={{ textAlign: 'center' }}>
              <Heading level="1" spacing size="medium" id="modal-heading">
                Er du sikker på at du vil ferdigstille brevet?
              </Heading>

              <BodyLong spacing>
                Når du ferdigstiller brevet vil det bli journalført og distribuert. Denne handlingen kan ikke angres.
              </BodyLong>

              <FlexRow justify="center">
                <Button
                  variant="secondary"
                  onClick={() => setIsOpen(false)}
                  loading={isPending(ferdigstillStatus) || isPending(journalfoerStatus) || isPending(distribuerStatus)}
                >
                  Nei, fortsett redigering
                </Button>
                <Button
                  variant="primary"
                  className="button"
                  onClick={ferdigstill}
                  loading={isPending(ferdigstillStatus) || isPending(journalfoerStatus) || isPending(distribuerStatus)}
                >
                  Ja, ferdigstill brev
                </Button>
              </FlexRow>

              {isPending(ferdigstillStatus) && <Alert variant="info">Forsøker å ferdigstille brev ...</Alert>}
              {isPending(journalfoerStatus) && <Alert variant="info">... journalfører brevet i dokarkiv ...</Alert>}
              {isPending(distribuerStatus) && <Alert variant="info">... sender brev til distribusjon ...</Alert>}
            </Modal.Body>
          </Modal>
        </>
      )}
    </Panel>
  )
}
