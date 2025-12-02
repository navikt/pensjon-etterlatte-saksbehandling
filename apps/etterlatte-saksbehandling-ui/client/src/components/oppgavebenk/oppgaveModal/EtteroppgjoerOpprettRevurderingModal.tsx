import { Alert, BodyShort, Button, HStack, Modal, Textarea, VStack } from '@navikt/ds-react'
import { EyeIcon } from '@navikt/aksel-icons'
import React, { useState } from 'react'

import { erOppgaveRedigerbar, OppgaveDTO, Oppgavestatus, Oppgavetype } from '~shared/types/oppgave'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ferdigstillOppgaveMedMerknad } from '~shared/api/oppgaver'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { formaterDato } from '~utils/formatering/dato'
import { opprettRevurderingEtteroppgjoer as opprettRevurderingApi } from '~shared/api/revurdering'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useForm } from 'react-hook-form'
import { Opprinnelse } from '~shared/types/IDetaljertBehandling'
import { useNavigate } from 'react-router-dom'

type Props = {
  oppgave: OppgaveDTO
  oppdaterStatus: (oppgaveId: string, status: Oppgavestatus) => void
}

export const EtteroppgjoerOpprettRevurderingModal = ({ oppgave, oppdaterStatus }: Props) => {
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

  const [opprettRevurderingResult, opprettRevurderingRequest] = useApiCall(opprettRevurderingApi)

  const opprettRevurderingEtteroppgjoer = () => {
    opprettRevurderingRequest({ sakId: oppgave.sakId, opprinnelse: Opprinnelse.AUTOMATISK_JOBB }, (result) => {
      ferdigstill({ id: oppgave.id, merknad: oppgave.merknad }, () => {
        oppdaterStatus(oppgave.id, Oppgavestatus.FERDIGSTILT)
        navigate(`/behandling/${result}/etteroppgjoeroversikt`)
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

  const tittel =
    oppgave.type === Oppgavetype.ETTEROPPGJOER_OPPRETT_REVURDERING
      ? 'Etteroppgjør – svarfrist utløpt'
      : 'Etteroppgjør - opprett revurdering'

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
        header={{ heading: tittel }}
      >
        <Modal.Body>
          <VStack gap="4">
            <HStack gap="4">
              <Info label="Opprettet" tekst={formaterDato(oppgave.opprettet)} />
              <Info label="Frist" tekst={formaterDato(oppgave.frist)} />
            </HStack>

            {oppgave.merknad && <Alert variant="info">{oppgave.merknad}</Alert>}

            <BodyShort>Etteroppgjøret kan ferdigstilles</BodyShort>

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
                  description="Legg til kommentar hvis du avslutter oppgaven. Dette er ikke nødvendig dersom du oppretter en revurdering eller forbehandling."
                  error={errors.kommentar?.message}
                />
              ) : (
                <BodyShort>Du må tildele deg oppgaven for å endre den.</BodyShort>
              ))}

            {mapResult(opprettRevurderingResult, {
              error: (error) => (
                <ApiErrorAlert>Kunne ikke opprette revurdering for etteroppgjør. {error.detail}</ApiErrorAlert>
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
                <Button loading={isPending(opprettRevurderingResult)} onClick={() => opprettRevurderingEtteroppgjoer()}>
                  Opprett revurdering
                </Button>
              )}
            </HStack>
          </VStack>
        </Modal.Body>
      </Modal>
    </>
  )
}
