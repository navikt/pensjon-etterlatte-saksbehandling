import { Alert, BodyShort, Button, HStack, Modal, Textarea, VStack } from '@navikt/ds-react'
import { ExternalLinkIcon, EyeIcon } from '@navikt/aksel-icons'
import React, { useEffect, useState } from 'react'
import { erOppgaveRedigerbar, OppgaveDTO, Oppgavestatus } from '~shared/types/oppgave'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ferdigstillOppgaveMedMerknad } from '~shared/api/oppgaver'
import { isPending, mapResult, mapSuccess } from '~shared/api/apiUtils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { useForm } from 'react-hook-form'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { formaterDato, formaterDatoMedKlokkeslett } from '~utils/formatering/dato'
import { hentBrev } from '~shared/api/brev'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { NavLink } from 'react-router-dom'
import { BrevStatus } from '~shared/types/Brev'

export const BrevOppgaveModal = ({
  oppgave,
  oppdaterStatus,
}: {
  oppgave: OppgaveDTO
  oppdaterStatus: (oppgaveId: string, status: Oppgavestatus) => void
}) => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const [isOpen, setIsOpen] = useState<boolean>(false)

  const [brevResult, hentBrevMedId] = useApiCall(hentBrev)
  const [ferdigstillOppgaveStatus, avsluttOppgave] = useApiCall(ferdigstillOppgaveMedMerknad)

  const {
    formState: { errors },
    handleSubmit,
    register,
  } = useForm<{ kommentar: string }>({ defaultValues: { kommentar: '' } })

  const avslutt = ({ kommentar }: { kommentar: string }) => {
    const nyMerknad = `${oppgave.merknad} – Kommentar (${formaterDatoMedKlokkeslett(new Date())}): ${kommentar}`

    avsluttOppgave({ id: oppgave.id, merknad: nyMerknad }, (oppgave) => {
      oppdaterStatus(oppgave.id, oppgave.status)
      setIsOpen(false)
    })
  }

  useEffect(() => {
    if (isOpen) hentBrevMedId({ brevId: Number(oppgave.referanse), sakId: oppgave.sakId })
  }, [isOpen])

  const erTildeltSaksbehandler = innloggetSaksbehandler.ident === oppgave.saksbehandler?.ident
  const kanRedigeres = erOppgaveRedigerbar(oppgave.status)

  return (
    <>
      <Button variant="primary" size="small" icon={<EyeIcon aria-hidden />} onClick={() => setIsOpen(true)}>
        Se oppgave
      </Button>

      <Modal
        open={isOpen}
        aria-labelledby="modal-heading"
        width="medium"
        onClose={() => setIsOpen(false)}
        header={{ heading: 'Manuell utsending av brev' }}
      >
        <Modal.Body>
          <VStack gap="space-4">
            <HStack gap="space-4">
              <Info label="Opprettet" tekst={formaterDato(oppgave.opprettet)} />
              <Info label="Frist" tekst={formaterDato(oppgave.frist)} />
            </HStack>

            <Alert variant="info">{oppgave.merknad}</Alert>

            {kanRedigeres &&
              (erTildeltSaksbehandler ? (
                <Textarea
                  {...register('kommentar', {
                    required: {
                      value: true,
                      message: 'Du må legge til en kommentar',
                    },
                    minLength: {
                      value: 10,
                      message: 'Kommentaren må bestå av minst 10 tegn',
                    },
                  })}
                  label="Kommentar"
                  error={errors.kommentar?.message}
                />
              ) : (
                <BodyShort>Du må tildele deg oppgaven for å endre den.</BodyShort>
              ))}

            {mapResult(brevResult, {
              success: (brev) => {
                if (brev.status !== BrevStatus.DISTRIBUERT) {
                  if (brev.status === BrevStatus.JOURNALFOERT) {
                    return <Alert variant="warning">Brevet er ikke distribuert!</Alert>
                  } else {
                    return <Alert variant="warning">Brevet er ikke journalført eller distribuert!</Alert>
                  }
                } else {
                  return (
                    <Alert variant="success" inline>
                      Brevet er journalført og distribuert. Oppgaven kan avsluttes.
                    </Alert>
                  )
                }
              },
              pending: <Spinner label="Henter informasjon om brevet" />,
              error: (error) => <ApiErrorAlert>{error.detail}</ApiErrorAlert>,
            })}

            <HStack gap="space-4" justify="end">
              <Button
                variant="secondary"
                onClick={() => setIsOpen(false)}
                disabled={isPending(ferdigstillOppgaveStatus)}
              >
                Lukk
              </Button>

              {mapSuccess(brevResult, (brev) => (
                <Button
                  as={NavLink}
                  variant="primary"
                  to={`/person/sak/${brev.sakId}/brev/${brev.id}`}
                  target="_blank"
                  icon={<ExternalLinkIcon aria-hidden />}
                >
                  Åpne brev
                </Button>
              ))}

              {kanRedigeres && erTildeltSaksbehandler && (
                <Button variant="danger" onClick={handleSubmit(avslutt)} loading={isPending(ferdigstillOppgaveStatus)}>
                  Avslutt oppgave
                </Button>
              )}
            </HStack>
          </VStack>
        </Modal.Body>
      </Modal>
    </>
  )
}
