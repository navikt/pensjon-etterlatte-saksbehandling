import { Alert, BodyShort, Button, HStack, Modal, Textarea, VStack } from '@navikt/ds-react'
import { EyeIcon } from '@navikt/aksel-icons'
import React, { useState } from 'react'

import { erOppgaveRedigerbar, OppgaveDTO, Oppgavestatus } from '~shared/types/oppgave'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ferdigstillOppgaveMedMerknad } from '~shared/api/oppgaver'
import { isPending } from '~shared/api/apiUtils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { formaterDato } from '~utils/formatering/dato'
import { useForm } from 'react-hook-form'
import { opprettEtteroppgoerForbehandling as opprettForbehandlingApi } from '~shared/api/etteroppgjoer'
import { useNavigate } from 'react-router-dom'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { VelgEtteroppgjoersAar } from '~components/etteroppgjoer/components/utils/VelgEtteroppgjoersAar'

type Props = {
  oppgave: OppgaveDTO
  oppdaterStatus: (oppgaveId: string, status: Oppgavestatus) => void
}

export const OpprettEtteroppgjoerForbehandlingModal = ({ oppgave, oppdaterStatus }: Props) => {
  const [open, setOpen] = useState(false)
  const [valgtEtteroppgjoer, setValgtEtteroppgjoer] = useState<string>('')

  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const [ferdigstillOppgaveStatus, avsluttOppgave] = useApiCall(ferdigstillOppgaveMedMerknad)

  const [opprettForbehandlingResult, opprettForbehandlingRequest] = useApiCall(opprettForbehandlingApi)

  const erTildeltSaksbehandler = innloggetSaksbehandler.ident === oppgave.saksbehandler?.ident

  const kanRedigeres = erOppgaveRedigerbar(oppgave.status)

  const navigate = useNavigate()

  const {
    formState: { errors },
    handleSubmit,
    register,
    reset,
  } = useForm<{ kommentar: string }>({
    defaultValues: { kommentar: '' },
  })

  const lukkModal = () => {
    setValgtEtteroppgjoer('')
    reset()
    setOpen(false)
  }

  const opprettForbehandling = () => {
    if (!valgtEtteroppgjoer) return

    opprettForbehandlingRequest(
      {
        sakId: oppgave.sakId,
        oppgaveId: oppgave.id,
        inntektsaar: valgtEtteroppgjoer,
      },
      (forbehandling) => {
        lukkModal()
        navigate(`/etteroppgjoer/${forbehandling.id}`)
      }
    )
  }

  const avslutt = ({ kommentar }: { kommentar: string }) => {
    const nyMerknad = `${oppgave.merknad ?? ''} – Kommentar: ${kommentar}`

    avsluttOppgave({ id: oppgave.id, merknad: nyMerknad }, (oppgaveResponse) => {
      oppdaterStatus(oppgaveResponse.id, oppgaveResponse.status)
      lukkModal()
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
        onClose={lukkModal}
        header={{ heading: 'Etteroppgjør – opprett forbehandling' }}
      >
        <Modal.Body>
          <VStack gap="4">
            <HStack gap="4">
              <Info label="Opprettet" tekst={formaterDato(oppgave.opprettet)} />
              <Info label="Frist" tekst={formaterDato(oppgave.frist)} />
            </HStack>

            {oppgave.merknad && <Alert variant="info">{oppgave.merknad}</Alert>}

            {kanRedigeres && erTildeltSaksbehandler && (
              <VelgEtteroppgjoersAar
                sakId={oppgave.sakId.toString()}
                value={valgtEtteroppgjoer}
                onChange={setValgtEtteroppgjoer}
              />
            )}

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
                  description="Legg til kommentar hvis du avslutter oppgaven. Dette er ikke nødvendig dersom du oppretter forbehandling."
                  error={errors.kommentar?.message}
                />
              ) : (
                <BodyShort>Du må tildele deg oppgaven for å endre den.</BodyShort>
              ))}

            {isFailureHandler({
              apiResult: opprettForbehandlingResult,
              errorMessage: 'Kunne ikke opprette forbehandling',
            })}

            <HStack gap="4" justify="end">
              <Button variant="secondary" onClick={lukkModal} disabled={isPending(ferdigstillOppgaveStatus)}>
                Lukk
              </Button>

              {kanRedigeres && erTildeltSaksbehandler && (
                <Button variant="primary" onClick={handleSubmit(avslutt)} loading={isPending(ferdigstillOppgaveStatus)}>
                  Avslutt oppgave
                </Button>
              )}

              {kanRedigeres && erTildeltSaksbehandler && (
                <Button
                  loading={isPending(opprettForbehandlingResult)}
                  disabled={!valgtEtteroppgjoer}
                  onClick={opprettForbehandling}
                >
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
