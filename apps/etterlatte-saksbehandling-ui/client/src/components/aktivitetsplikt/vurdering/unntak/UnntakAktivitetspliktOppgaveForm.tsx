import {
  AktivitetspliktUnntakType,
  IAktivitetspliktUnntak,
  tekstAktivitetspliktUnntakType,
} from '~shared/types/Aktivitetsplikt'
import { useForm } from 'react-hook-form'
import { Box, Button, HStack, Select, Textarea, VStack } from '@navikt/ds-react'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { FloppydiskIcon } from '@navikt/aksel-icons'
import React from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettAktivitetspliktUnntak } from '~shared/api/aktivitetsplikt'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'
import { isFailure, isPending } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { startOfMonth } from 'date-fns'

export interface IOpprettAktivitetspliktUnntak {
  id: string | undefined
  unntak: AktivitetspliktUnntakType
  fom: string
  tom?: string
  beskrivelse: string
}

function formdataErUtfylt(formdata: Partial<IOpprettAktivitetspliktUnntak>): formdata is IOpprettAktivitetspliktUnntak {
  return !!formdata.fom && !!formdata.unntak
}

export function UnntakAktivitetspliktOppgaveForm(props: {
  unntak?: IAktivitetspliktUnntak
  onSuccess: () => void
  onAvbryt?: () => void
}) {
  const { oppgave } = useAktivitetspliktOppgaveVurdering()
  const [lagreUnntakStatus, lagreUnntak] = useApiCall(opprettAktivitetspliktUnntak)

  const { control, register, handleSubmit } = useForm<Partial<IOpprettAktivitetspliktUnntak>>({
    defaultValues: props.unntak ?? { fom: startOfMonth(new Date()).toISOString() },
  })

  function sendInn(formdata: Partial<IOpprettAktivitetspliktUnntak>) {
    if (!formdataErUtfylt(formdata)) {
      return
    }

    lagreUnntak(
      {
        oppgaveId: oppgave.id,
        request: {
          id: formdata.id,
          unntak: formdata.unntak,
          fom: formdata.fom,
          beskrivelse: formdata.beskrivelse || '',
        },
        sakId: oppgave.sakId,
      },
      props.onSuccess
    )
  }

  return (
    <form onSubmit={handleSubmit(sendInn)}>
      <Box maxWidth="40rem">
        <VStack gap="4">
          <HStack gap="4">
            <ControlledDatoVelger name="fom" label="Unntak fra og med" control={control} />
            <ControlledDatoVelger name="tom" label="Unntak til og med" required={false} control={control} />
          </HStack>
          <Select
            {...register('unntak', {
              required: {
                value: true,
                message: 'Du må velge typen unntak.',
              },
            })}
            label="Type unntak"
          >
            <option value={AktivitetspliktUnntakType.MIDLERTIDIG_SYKDOM}>
              {tekstAktivitetspliktUnntakType[AktivitetspliktUnntakType.MIDLERTIDIG_SYKDOM]}
            </option>
            <option value={AktivitetspliktUnntakType.MANGLENDE_TILSYNSORDNING_SYKDOM}>
              {tekstAktivitetspliktUnntakType[AktivitetspliktUnntakType.MANGLENDE_TILSYNSORDNING_SYKDOM]}
            </option>
            <option value={AktivitetspliktUnntakType.OMSORG_BARN_SYKDOM}>
              {tekstAktivitetspliktUnntakType[AktivitetspliktUnntakType.OMSORG_BARN_SYKDOM]}
            </option>
            <option value={AktivitetspliktUnntakType.OMSORG_BARN_UNDER_ETT_AAR}>
              {tekstAktivitetspliktUnntakType[AktivitetspliktUnntakType.OMSORG_BARN_UNDER_ETT_AAR]}
            </option>
            <option value={AktivitetspliktUnntakType.SYKDOM_ELLER_REDUSERT_ARBEIDSEVNE}>
              {tekstAktivitetspliktUnntakType[AktivitetspliktUnntakType.SYKDOM_ELLER_REDUSERT_ARBEIDSEVNE]}
            </option>
          </Select>

          <Textarea {...register('beskrivelse')} label="Beskrivelse" />
          <HStack gap="4">
            {!!props.onAvbryt && (
              <Button variant="secondary" onClick={props.onAvbryt}>
                Avbryt
              </Button>
            )}
            <Button variant="primary" type="submit" icon={<FloppydiskIcon />} loading={isPending(lagreUnntakStatus)}>
              Lagre
            </Button>
          </HStack>
          {isFailure(lagreUnntakStatus) && (
            <ApiErrorAlert>Kunne ikke lagre unntak: {lagreUnntakStatus.error.detail}</ApiErrorAlert>
          )}
        </VStack>
      </Box>
    </form>
  )
}
