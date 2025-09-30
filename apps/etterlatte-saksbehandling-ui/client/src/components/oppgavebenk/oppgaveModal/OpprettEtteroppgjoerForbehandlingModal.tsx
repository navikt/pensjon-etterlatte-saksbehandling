import { Alert, BodyShort, Button, HStack, Modal, Textarea, VStack } from '@navikt/ds-react'
import { EyeIcon } from '@navikt/aksel-icons'
import React, { useState } from 'react'

import { erOppgaveRedigerbar, OppgaveDTO, Oppgavestatus } from '~shared/types/oppgave'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ferdigstillOppgaveMedMerknad } from '~shared/api/oppgaver'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { formaterDato } from '~utils/formatering/dato'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useForm } from 'react-hook-form'
import { opprettEtteroppgoerForbehandling as opprettForbehandlingApi } from '~shared/api/etteroppgjoer'
import { useNavigate } from 'react-router-dom'

type Props = {
  oppgave: OppgaveDTO
  oppdaterStatus: (oppgaveId: string, status: Oppgavestatus) => void
}

export const OpprettEtteroppgjoerForbehandlingModal = ({ oppgave, oppdaterStatus }: Props) => {
  const [open, setOpen] = useState(false)
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const [ferdigstillStatus] = useApiCall(ferdigstillOppgaveMedMerknad)
  const [ferdigstillOppgaveStatus, avsluttOppgave] = useApiCall(ferdigstillOppgaveMedMerknad)

  const erTildeltSaksbehandler = innloggetSaksbehandler.ident === oppgave.saksbehandler?.ident
  const kanRedigeres = erOppgaveRedigerbar(oppgave.status)

  const navigate = useNavigate()

  const {
    formState: { errors },
    handleSubmit,
    register,
  } = useForm<{ kommentar: string }>({ defaultValues: { kommentar: '' } })

  const [opprettForbehandlingResult, opprettForbehandlingRequest] = useApiCall(opprettForbehandlingApi)

  const opprettForbehandling = () => {
    opprettForbehandlingRequest({ sakId: oppgave.sakId, oppgaveId: oppgave.id }, (forbehandling) => {
      navigate(`/etteroppgjoer/${forbehandling.id}`)
    })
  }

  const avslutt = ({ kommentar }: { kommentar: string }) => {
    const nyMerknad = `${oppgave.merknad} – \nKommentar: ${kommentar}`

    avsluttOppgave({ id: oppgave.id, merknad: nyMerknad }, (oppgave) => {
      oppdaterStatus(oppgave.id, oppgave.status)
      setOpen(false)
    })
  }

  return (
    <>
      <Button variant="primary" size="small" icon={<EyeIcon aria-hidden />} onClick={() => setOpen(true)}>
        Se oppgave
      </Button>

      <Modal
        open={open}
        aria-labelledby="modal-heading"
        width="medium"
        onClose={() => setOpen(false)}
        header={{ heading: 'Etteroppgjør – opprett forbehandling' }}
      >
        <Modal.Body>
          <VStack gap="4">
            <HStack gap="4">
              <Info label="Opprettet" tekst={formaterDato(oppgave.opprettet)} />
              <Info label="Frist" tekst={formaterDato(oppgave.frist)} />
            </HStack>

            {oppgave.merknad && <Alert variant="info">{oppgave.merknad}</Alert>}

            {kanRedigeres &&
              (erTildeltSaksbehandler ? (
                <Textarea
                  {...register('kommentar', {
                    required: {
                      value: true,
                      message: 'Du må legge til en kommentar',
                    },
                  })}
                  label="Kommentar"
                  error={errors.kommentar?.message}
                />
              ) : (
                <BodyShort>Du må tildele deg oppgaven for å endre den.</BodyShort>
              ))}

            {mapResult(opprettForbehandlingResult, {
              error: (error) => (
                <ApiErrorAlert>Kunne ikke opprette forbehandling for etteroppgjør. {error.detail}</ApiErrorAlert>
              ),
            })}

            <HStack gap="4" justify="end">
              <Button variant="secondary" onClick={() => setOpen(false)} disabled={isPending(ferdigstillStatus)}>
                Lukk
              </Button>

              {kanRedigeres && erTildeltSaksbehandler && (
                <Button variant="primary" onClick={handleSubmit(avslutt)} loading={isPending(ferdigstillOppgaveStatus)}>
                  Avslutt oppgave
                </Button>
              )}

              {kanRedigeres && erTildeltSaksbehandler && (
                <Button loading={isPending(opprettForbehandlingResult)} onClick={() => opprettForbehandling()}>
                  Opprett forbehandling
                </Button>
              )}
            </HStack>
          </VStack>
        </Modal.Body>
      </Modal>
    </>
  )
}
