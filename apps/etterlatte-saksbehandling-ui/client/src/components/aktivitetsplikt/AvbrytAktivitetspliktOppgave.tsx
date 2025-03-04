import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/AktivitetspliktOppgaveVurderingRoutes'
import { useApiCall } from '~shared/hooks/useApiCall'
import { avbrytOppgaveMedMerknad } from '~shared/api/oppgaver'
import { Alert, Box, Button, HStack, Textarea } from '@navikt/ds-react'
import { isPending } from '~shared/api/apiUtils'
import React, { useState } from 'react'
import { erOppgaveRedigerbar } from '~shared/types/oppgave'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { SidebarPanel } from '~shared/components/Sidebar'
import { TrashIcon } from '@navikt/aksel-icons'
import { Controller, useForm } from 'react-hook-form'
import { useNavigate } from 'react-router'

interface AvbrytOppgave {
  merknad: string
}

export function AvbrytAktivitetspliktOppgave() {
  const navigate = useNavigate()

  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const { oppgave } = useAktivitetspliktOppgaveVurdering()
  const [showAvsluttForm, setShowAvsluttForm] = useState(false)

  const erTildeltSaksbehandler = innloggetSaksbehandler.ident === oppgave.saksbehandler?.ident
  const kanRedigeres = erOppgaveRedigerbar(oppgave.status)

  const [avbrytOppgaveMedMerknadResult, avbrytOppgaveMedMerknadRequest] = useApiCall(avbrytOppgaveMedMerknad)

  const {
    control,
    watch,
    formState: { errors },
  } = useForm<AvbrytOppgave>({
    defaultValues: { merknad: 'Oppgave avbrytes av saksbehandler' },
  })

  const avbryt = () => {
    const merknad = watch('merknad')
    avbrytOppgaveMedMerknadRequest({ id: oppgave.id, merknad: merknad })
    navigate('/person', { state: { fnr: oppgave.fnr } })
  }

  return (
    <SidebarPanel $border>
      {!showAvsluttForm && (
        <>
          <div>
            <Button
              variant="secondary"
              icon={<TrashIcon aria-hidden />}
              onClick={() => setShowAvsluttForm(!showAvsluttForm)}
            >
              Avbryt oppgave
            </Button>
          </div>
        </>
      )}
      {showAvsluttForm && (
        <Box>
          {kanRedigeres && erTildeltSaksbehandler ? (
            <HStack gap="4">
              <Controller
                name="merknad"
                control={control}
                render={({ field }) => {
                  const { value, ...rest } = field
                  return (
                    <Textarea
                      label="Begrunnelse"
                      description="Utdyp hvorfor oppgaven avsluttes"
                      value={value ?? ''}
                      {...rest}
                      size="medium"
                      error={errors?.merknad?.message}
                    />
                  )
                }}
              />

              <Button variant="danger" onClick={avbryt} loading={isPending(avbrytOppgaveMedMerknadResult)}>
                Avbryt oppgave
              </Button>
            </HStack>
          ) : (
            <Alert variant="warning">Du må tildele deg oppgaven for å avbryte oppgaven</Alert>
          )}
        </Box>
      )}
    </SidebarPanel>
  )
}
