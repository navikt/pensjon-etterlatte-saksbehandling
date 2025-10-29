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
import { useNavigate } from 'react-router-dom'
import { opprettOppgaveForOmgjoering } from '~shared/api/klage'

type Props = {
  oppgave: OppgaveDTO
  oppdaterStatus: (oppgaveId: string, status: Oppgavestatus) => void
}

export const KlageBehandleSvarFraKa = ({ oppgave, oppdaterStatus }: Props) => {
  const [open, setOpen] = useState(false)

  const navigate = useNavigate()

  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const [ferdigstillStatus, ferdigstill] = useApiCall(ferdigstillOppgaveMedMerknad)
  const [ferdigstillOppgaveStatus, avsluttOppgave] = useApiCall(ferdigstillOppgaveMedMerknad)

  const erTildeltSaksbehandler = innloggetSaksbehandler.ident === oppgave.saksbehandler?.ident
  const kanRedigeres = erOppgaveRedigerbar(oppgave.status)

  const {
    formState: { errors },
    handleSubmit,
    register,
  } = useForm<{ kommentar: string }>({ defaultValues: { kommentar: '' } })

  const [opprettOmgjoeringOppgaveResult, opprettOmgjoeringOppgaveRequest] = useApiCall(opprettOppgaveForOmgjoering)

  const opprettOmgjoeringOppgave = () => {
    opprettOmgjoeringOppgaveRequest({ klageId: oppgave.referanse!! }, () => {
      ferdigstill({ id: oppgave.id, merknad: oppgave.merknad }, () => {
        oppdaterStatus(oppgave.id, Oppgavestatus.FERDIGSTILT)
        navigate('/person', { state: { fnr: oppgave.fnr } })
      })
    })
  }

  const avslutt = ({ kommentar }: { kommentar: string }) => {
    const nyMerknad = `${oppgave.merknad} – Kommentar: ${kommentar}`

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
        header={{ heading: 'Klage - Behandle svar fra KA' }}
      >
        <Modal.Body>
          <VStack gap="4">
            <HStack gap="4">
              <Info label="Opprettet" tekst={formaterDato(oppgave.opprettet)} />
              <Info label="Frist" tekst={formaterDato(oppgave.frist)} />
            </HStack>

            {oppgave.merknad && <Alert variant="info">{oppgave.merknad}</Alert>}

            <BodyShort>Klage svar fra KA</BodyShort>

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
                  description="Legg til kommentar hvis du avslutter oppgaven. Dette er ikke nødvendig dersom du oppretter en omgjøring av vedtaket."
                  error={errors.kommentar?.message}
                />
              ) : (
                <BodyShort>Du må tildele deg oppgaven for å endre den.</BodyShort>
              ))}

            {mapResult(opprettOmgjoeringOppgaveResult, {
              error: (error) => <ApiErrorAlert>Det oppstod en feil: {error.detail}</ApiErrorAlert>,
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
                <Button loading={isPending(opprettOmgjoeringOppgaveResult)} onClick={() => opprettOmgjoeringOppgave()}>
                  Opprett omgjøring av vedtak
                </Button>
              )}
            </HStack>
          </VStack>
        </Modal.Body>
      </Modal>
    </>
  )
}
