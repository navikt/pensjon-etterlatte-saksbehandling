import { Alert, BodyLong, Box, Button, ConfirmationPanel, Heading, HStack, Modal } from '@navikt/ds-react'
import { BrevStatus, Brevtype, IBrev } from '~shared/types/Brev'
import { useApiCall } from '~shared/hooks/useApiCall'
import { useState } from 'react'
import { distribuerBrev, ferdigstillBrev, journalfoerBrev } from '~shared/api/brev'
import Spinner from '~shared/Spinner'

import { isPending, mapResult } from '~shared/api/apiUtils'

interface Props {
  brev: IBrev
  setKanRedigeres: (kanRedigeres: boolean) => void
  callback?: () => void
  erAktivitetspliktVarsel?: boolean
}

export default function NyttBrevHandlingerPanel({ brev, setKanRedigeres, callback, erAktivitetspliktVarsel }: Props) {
  const [isOpen, setIsOpen] = useState(false)
  const [statusModalOpen, setStatusModalOpen] = useState(false)
  const [bekreftetInfobrev, setBekreftetInfobrev] = useState(!erAktivitetspliktVarsel)
  const [feilmeldingBekreft, setFeilmeldingBekreft] = useState<string | undefined>()

  const [ferdigstillStatus, apiFerdigstillBrev] = useApiCall(ferdigstillBrev)
  const [journalfoerStatus, apiJournalfoerBrev] = useApiCall(journalfoerBrev)
  const [distribuerStatus, apiDistribuerBrev] = useApiCall(distribuerBrev)
  //TODO: ferdigstill, journalfoer og distribuer burde gjøres i samme kall
  const ferdigstill = () => {
    setFeilmeldingBekreft(undefined)
    if (erAktivitetspliktVarsel && !bekreftetInfobrev) {
      setFeilmeldingBekreft('Du må huke av for at infobrevet er sendt')
      return
    }
    apiFerdigstillBrev(
      {
        brevId: brev.id,
        sakId: brev.sakId,
        brevtype: brev.brevtype,
      },
      () => journalfoer()
    )
    setStatusModalOpen(true)
    setIsOpen(false)
  }

  const journalfoer = () => {
    apiJournalfoerBrev({ brevId: brev.id, sakId: brev.sakId }, () => distribuer())
  }

  const distribuer = () =>
    apiDistribuerBrev({ brevId: brev.id, sakId: brev.sakId }, () => {
      setKanRedigeres(false)
      if (callback) {
        callback()
      }
      void setTimeout(() => window.location.reload(), 2000)
    })

  if (brev.status === BrevStatus.DISTRIBUERT) {
    return null
  }

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
        {mapResult(ferdigstillStatus, {
          pending: <Spinner label="Forsøker å ferdigstille brevet ..." />,
          error: (error) => <Alert variant="error">{error.detail}</Alert>,
          success: () => <Alert variant="info">Ferdigstilt ok!</Alert>,
        })}

        {mapResult(journalfoerStatus, {
          pending: <Spinner label="Journalfører brevet i dokarkiv ..." />,
          error: (error) => <Alert variant="error">{error.detail}</Alert>,
          success: () => <Alert variant="info">Journalført ok!</Alert>,
        })}

        {mapResult(distribuerStatus, {
          pending: <Spinner label="Sender brev til distribusjon ..." />,
          error: (error) => <Alert variant="error">{error.detail}</Alert>,
          success: () => <Alert variant="success">Brev sendt til distribusjon. Laster inn brev på nytt...</Alert>,
        })}
      </Modal>

      <Modal open={isOpen && !statusModalOpen} onClose={() => setIsOpen(false)} aria-labelledby="modal-heading">
        <Modal.Body>
          <Heading level="1" spacing size="medium" id="modal-heading">
            Er du sikker på at du vil ferdigstille brevet?
          </Heading>

          <BodyLong spacing>
            Når du ferdigstiller brevet vil det bli journalført og distribuert. Denne handlingen kan ikke angres.
          </BodyLong>
          {brev.brevtype === Brevtype.VARSEL && (
            <BodyLong spacing>Når varselbrevet er sendt kan du sette oppgaven på vent.</BodyLong>
          )}
          {erAktivitetspliktVarsel && (
            <Box paddingBlock="4">
              <ConfirmationPanel
                label="Infobrevet er sendt for minst tre uker siden, slik at varselsbrevet kan sendes ut"
                onChange={() => setBekreftetInfobrev((bekreftet) => !bekreftet)}
                error={feilmeldingBekreft}
              >
                <strong>Infobrev er sendt</strong>
              </ConfirmationPanel>
            </Box>
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
