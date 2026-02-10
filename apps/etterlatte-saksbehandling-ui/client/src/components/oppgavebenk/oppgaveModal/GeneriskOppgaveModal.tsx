import { Alert, BodyShort, Button, HStack, Modal, Textarea, VStack } from '@navikt/ds-react'
import { EyeIcon } from '@navikt/aksel-icons'
import React, { useState } from 'react'
import { erOppgaveRedigerbar, OppgaveDTO, Oppgavestatus } from '~shared/types/oppgave'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ferdigstillOppgaveMedMerknad } from '~shared/api/oppgaver'
import { isPending } from '~shared/api/apiUtils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { useForm } from 'react-hook-form'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { formaterDato } from '~utils/formatering/dato'

export const GeneriskOppgaveModal = ({
  heading,
  oppgave,
  oppdaterStatus,
}: {
  heading: string
  oppgave: OppgaveDTO
  oppdaterStatus: (oppgaveId: string, status: Oppgavestatus) => void
}) => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const [open, setOpen] = useState<boolean>(false)
  const [ferdigstillOppgaveStatus, avsluttOppgave] = useApiCall(ferdigstillOppgaveMedMerknad)

  const {
    formState: { errors },
    handleSubmit,
    register,
  } = useForm<{ kommentar: string }>({ defaultValues: { kommentar: '' } })

  const avslutt = ({ kommentar }: { kommentar: string }) => {
    // hvis oppgave har merknad, kombiner den med ny kommentar
    // TODO: burde egentlig legge til et eget felt for kommentar så vi slipper tullet under her
    const nyMerknad = kommentar?.trim()
      ? oppgave.merknad
        ? `${oppgave.merknad}${oppgave.merknad.trim().endsWith('.') ? '' : '.'} Kommentar: ${kommentar}`
        : `Kommentar: ${kommentar}`
      : (oppgave.merknad ?? '')

    avsluttOppgave({ id: oppgave.id, merknad: nyMerknad }, () => {
      oppdaterStatus(oppgave.id, Oppgavestatus.FERDIGSTILT)
      setOpen(false)
    })
  }

  const erTildeltSaksbehandler = innloggetSaksbehandler.ident === oppgave.saksbehandler?.ident
  const kanRedigeres = erOppgaveRedigerbar(oppgave.status)

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
        header={{ heading }}
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

            <HStack gap="space-4" justify="end">
              <Button variant="secondary" onClick={() => setOpen(false)} disabled={isPending(ferdigstillOppgaveStatus)}>
                Lukk
              </Button>

              {kanRedigeres && erTildeltSaksbehandler && (
                <Button
                  data-color="danger"
                  variant="primary"
                  onClick={handleSubmit(avslutt)}
                  loading={isPending(ferdigstillOppgaveStatus)}
                >
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
