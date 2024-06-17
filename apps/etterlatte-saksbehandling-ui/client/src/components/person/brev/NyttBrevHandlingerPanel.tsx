import { Alert, BodyLong, Button, Heading, HStack, Modal } from '@navikt/ds-react'
import { BrevStatus, Brevtype, IBrev } from '~shared/types/Brev'
import { useApiCall } from '~shared/hooks/useApiCall'
import { useEffect, useState } from 'react'
import { distribuerBrev, ferdigstillBrev, journalfoerBrev } from '~shared/api/brev'
import Spinner from '~shared/Spinner'

import { isInitial, isPending, isSuccess, mapAllApiResult } from '~shared/api/apiUtils'

interface Props {
  brev: IBrev
  setKanRedigeres: (kanRedigeres: boolean) => void
  callback?: () => void
}

export default function NyttBrevHandlingerPanel({ brev, setKanRedigeres, callback }: Props) {
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
        brevtype: brev.brevtype,
      },
      () => journalfoer()
    )
    if (callback) {
      callback()
    }
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
    <>
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

      <Modal
        open={statusModalOpen}
        onClose={() => setStatusModalOpen(false)}
        width="medium"
        aria-label="Ferdigstilling av brev"
      >
        {mapAllApiResult(
          ferdigstillStatus,
          <Spinner label="Forsøker å ferdigstille brevet ..." visible />,
          null,
          (error) => (
            <Alert variant="error">{error.detail}</Alert>
          ),
          () => (
            <Alert variant="info">Ferdigstilt ok!</Alert>
          )
        )}

        {mapAllApiResult(
          journalfoerStatus,
          <Spinner label="Journalfører brevet i dokarkiv ..." visible />,
          null,
          (error) => (
            <Alert variant="error">{error.detail}</Alert>
          ),
          () => (
            <Alert variant="info">Journalført ok!</Alert>
          )
        )}

        {mapAllApiResult(
          distribuerStatus,
          <Spinner label="Sender brev til distribusjon ..." visible />,
          null,
          (error) => (
            <Alert variant="error">{error.detail}</Alert>
          ),
          () => (
            <Alert variant="success">Brev sendt til distribusjon. Laster inn brev på nytt...</Alert>
          )
        )}
      </Modal>

      <Modal open={isOpen && !statusModalOpen} onClose={() => setIsOpen(false)} aria-labelledby="modal-heading">
        <Modal.Body>
          <Heading level="1" spacing size="medium" id="modal-heading">
            Er du sikker på at du vil ferdigstille brevet?
          </Heading>

          <BodyLong spacing>
            Når du ferdigstiller brevet vil det bli journalført og distribuert. Denne handlingen kan ikke angres.
          </BodyLong>
          {brev.brevtype == Brevtype.VARSEL && (
            <BodyLong spacing>Når varselbrevet er sendt kan du sette oppgaven på vent.</BodyLong>
          )}
          <Alert variant="info">
            Ikke glem å lagre innholdet i brev <i>før</i> du ferdigstiller!
          </Alert>
        </Modal.Body>

        <Modal.Footer>
          <HStack gap="4" justify="center">
            <Button variant="secondary" onClick={() => setIsOpen(false)}>
              Nei, fortsett redigering
            </Button>
            <Button variant="primary" className="button" onClick={ferdigstill}>
              Ja, ferdigstill brev
            </Button>
          </HStack>
        </Modal.Footer>
      </Modal>
    </>
  )
}
