import { Alert, BodyLong, BodyShort, Button, Heading, Modal, Panel } from '@navikt/ds-react'
import { BrevStatus, IBrev } from '~shared/types/Brev'
import { isPending, useApiCall } from '~shared/hooks/useApiCall'
import { useState } from 'react'
import { distribuerBrev, ferdigstillBrev, journalfoerBrev } from '~shared/api/brev'
import Spinner from '~shared/Spinner'
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

  const [loadingMsg, setLoadingMsg] = useState<string>()
  const [error, setError] = useState<string>()

  const handleError = (msg: string) => {
    setError(msg)
    setIsOpen(false)
  }

  const ferdigstill = () => {
    setKanRedigeres(false)

    const idPayload = { brevId: brev.id, sakId: brev.sakId }

    setLoadingMsg('Forsøker å ferdigstille brev ...')

    apiFerdigstillBrev(idPayload, journalfoer, () => handleError('Feil oppsto ved ferdigstilling av brev'))
  }

  const journalfoer = () => {
    setLoadingMsg('... journalfører brevet i dokarkiv ...')

    apiJournalfoerBrev({ brevId: brev.id, sakId: brev.sakId }, distribuer, () =>
      handleError('Feil oppsto ved journalføring av brev')
    )
  }

  const distribuer = () => {
    setLoadingMsg('... sender brev til distribusjon ...')

    apiDistribuerBrev(
      { brevId: brev.id, sakId: brev.sakId },
      () => {
        setLoadingMsg('Distribuert OK – laster inn på nytt ...')
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

      <BodyShort spacing size="small">
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
                {loadingMsg ? (
                  <Spinner visible label={loadingMsg} />
                ) : (
                  <>
                    <Heading level="1" spacing size="medium" id="modal-heading">
                      Er du sikker på at du vil ferdigstille brevet?
                    </Heading>

                    <BodyLong spacing>
                      Når du ferdigstiller brevet vil det bli journalført og distribuert. Denne handlingen kan ikke
                      angres.
                    </BodyLong>

                    <FlexRow justify="center">
                      <Button variant="secondary" onClick={() => setIsOpen(false)}>
                        Nei, fortsett redigering
                      </Button>
                      <Button variant="primary" className="button" onClick={ferdigstill}>
                        Ja, ferdigstill brev
                      </Button>
                    </FlexRow>
                  </>
                )}
              </Modal.Body>
            </Modal>
          </>
        )}
      </BodyShort>
    </Panel>
  )
}
