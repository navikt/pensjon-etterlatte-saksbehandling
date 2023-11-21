import { Alert, BodyLong, Button, Heading, Modal, Panel } from '@navikt/ds-react'
import { BrevStatus, IBrev } from '~shared/types/Brev'
import { isInitial, isPending, isSuccess, mapAllApiResult, useApiCall } from '~shared/hooks/useApiCall'
import { useEffect, useState } from 'react'
import { distribuerBrev, ferdigstillBrev, journalfoerBrev } from '~shared/api/brev'
import { FlexRow } from '~shared/styled'
import Spinner from '~shared/Spinner'

interface Props {
  brev: IBrev
  setKanRedigeres: (kanRedigeres: boolean) => void
}

export default function NyttBrevHandlingerPanel({ brev, setKanRedigeres }: Props) {
  const [isOpen, setIsOpen] = useState(false)
  const [statusModalOpen, setStatusModalOpen] = useState(false)

  const [ferdigstillStatus, apiFerdigstillBrev] = useApiCall(ferdigstillBrev)
  const [journalfoerStatus, apiJournalfoerBrev] = useApiCall(journalfoerBrev)
  const [distribuerStatus, apiDistribuerBrev] = useApiCall(distribuerBrev)

  const ferdigstill = () => {
    setKanRedigeres(false)

    apiFerdigstillBrev(
      {
        brevId: brev.id,
        sakId: brev.sakId,
      },
      () => journalfoer()
    )
  }

  const journalfoer = () => {
    apiJournalfoerBrev({ brevId: brev.id, sakId: brev.sakId }, () => distribuer())
  }

  const distribuer = () =>
    apiDistribuerBrev(
      { brevId: brev.id, sakId: brev.sakId },
      () => void setTimeout(() => window.location.reload(), 2000)
    )

  if (brev.status === BrevStatus.DISTRIBUERT) {
    return null
  }

  useEffect(() => {
    setStatusModalOpen(
      isSuccess(distribuerStatus) ||
        !(isInitial(ferdigstillStatus) && isInitial(journalfoerStatus) && isInitial(distribuerStatus))
    )
  }, [ferdigstillStatus, journalfoerStatus, distribuerStatus])

  return (
    <Panel>
      <Heading spacing level="2" size="medium">
        Handlinger
      </Heading>

      {brev.status === BrevStatus.FERDIGSTILT ? (
        <Button variant="primary" onClick={journalfoer} loading={isPending(journalfoerStatus)}>
          Journalfør
        </Button>
      ) : brev.status === BrevStatus.JOURNALFOERT ? (
        <Button variant="primary" onClick={distribuer} loading={isPending(distribuerStatus)}>
          Distribuer
        </Button>
      ) : (
        <Button variant="primary" onClick={() => setIsOpen(true)} loading={isPending(ferdigstillStatus)}>
          Ferdigstill
        </Button>
      )}

      {statusModalOpen && (
        <Modal open={true} width="medium">
          {mapAllApiResult(
            ferdigstillStatus,
            <Spinner label="Forsøker å ferdigstille brevet ..." visible />,
            null,
            () => (
              <Alert variant="error">Feil oppsto ved ferdigstilling av brevet</Alert>
            ),
            () => (
              <Alert variant="info">Ferdigstilt ok!</Alert>
            )
          )}

          {mapAllApiResult(
            journalfoerStatus,
            <Spinner label="Journalfører brevet i dokarkiv ..." visible />,
            null,
            () => (
              <Alert variant="error">Feil oppsto ved journalføring av brevet</Alert>
            ),
            () => (
              <Alert variant="info">Journalført ok!</Alert>
            )
          )}

          {mapAllApiResult(
            distribuerStatus,
            <Spinner label="Sender brev til distribusjon ..." visible />,
            null,
            () => (
              <Alert variant="error">Feil ved sending til distribusjon</Alert>
            ),
            () => (
              <Alert variant="success">Brev sendt til distribusjon. Laster inn brev på nytt...</Alert>
            )
          )}
        </Modal>
      )}

      <Modal open={isOpen && !statusModalOpen} aria-labelledby="modal-heading" className="padding-modal">
        <Modal.Body style={{ textAlign: 'center' }}>
          <Heading level="1" spacing size="medium" id="modal-heading">
            Er du sikker på at du vil ferdigstille brevet?
          </Heading>

          <BodyLong spacing>
            Når du ferdigstiller brevet vil det bli journalført og distribuert. Denne handlingen kan ikke angres.
          </BodyLong>

          <FlexRow justify="center">
            <Button variant="secondary" onClick={() => setIsOpen(false)}>
              Nei, fortsett redigering
            </Button>
            <Button variant="primary" className="button" onClick={ferdigstill}>
              Ja, ferdigstill brev
            </Button>
          </FlexRow>
        </Modal.Body>
      </Modal>
    </Panel>
  )
}
