import {
  AktivitetspliktOppgaveVurderingType,
  AktivitetspliktUnntakType,
  IAktivitetspliktUnntak,
  IAktivitetspliktVurderingNyDto,
  tekstAktivitetspliktUnntakType,
} from '~shared/types/Aktivitetsplikt'
import { FormProvider, useForm, useFormContext } from 'react-hook-form'
import { Box, Button, HStack, Select, Textarea, VStack } from '@navikt/ds-react'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { FloppydiskIcon } from '@navikt/aksel-icons'
import React from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { redigerAktivitetspliktUnntakForOppgave } from '~shared/api/aktivitetsplikt'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/AktivitetspliktOppgaveVurderingRoutes'
import { isFailure, isPending, Result } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { startOfMonth } from 'date-fns'

//TODO: flytte til domain fil
export interface IOpprettAktivitetspliktUnntak {
  id: string | undefined
  unntak: AktivitetspliktUnntakType
  fom: string
  tom?: string
  beskrivelse: string
}

export function VelgOgLagreUnntakAktivitetspliktOppgave(props: {
  unntak: IAktivitetspliktUnntak
  oppdaterStateEtterRedigertUnntak: (data: IAktivitetspliktVurderingNyDto) => void
  onAvbryt: () => void
}) {
  const { oppgave, vurderingType } = useAktivitetspliktOppgaveVurdering()
  const [lagreUnntakStatus, lagreUnntak] = useApiCall(redigerAktivitetspliktUnntakForOppgave)
  const sendInn = (formdata: Partial<IOpprettAktivitetspliktUnntak>) => {
    lagreUnntak(
      {
        sakId: oppgave.sakId,
        oppgaveId: oppgave.id,
        request: {
          id: formdata.id,
          unntak: formdata.unntak!!,
          fom: formdata.fom!!,
          tom: formdata.tom,
          beskrivelse: formdata.beskrivelse || '',
        },
      },
      (data) => {
        props.oppdaterStateEtterRedigertUnntak(data)
      }
    )
  }

  return (
    <LagreUnntakForm
      lagreUnntakStatus={lagreUnntakStatus}
      sendInn={sendInn}
      unntak={props.unntak}
      vurderingType={vurderingType}
      onAvbryt={props.onAvbryt}
    />
  )
}

//TODO: flytte ut?
export const LagreUnntakForm = ({
  unntak,
  vurderingType,
  sendInn,
  onAvbryt,
  lagreUnntakStatus,
}: {
  unntak: IAktivitetspliktUnntak
  vurderingType: AktivitetspliktOppgaveVurderingType
  sendInn: (formdata: Partial<IOpprettAktivitetspliktUnntak>) => void
  onAvbryt: () => void
  lagreUnntakStatus: Result<IAktivitetspliktVurderingNyDto>
}) => {
  const methods = useForm<IOpprettAktivitetspliktUnntak>({
    defaultValues: unntak ?? { fom: startOfMonth(new Date()).toISOString() },
  })
  const { handleSubmit } = methods

  return (
    <FormProvider {...methods}>
      <form onSubmit={handleSubmit(sendInn)}>
        <UnntakAktivitetspliktOppgave vurderingType={vurderingType} />
        <HStack gap="4">
          {!!onAvbryt && (
            <Button variant="secondary" onClick={onAvbryt}>
              Avbryt
            </Button>
          )}
          <Button
            variant="primary"
            type="submit"
            icon={<FloppydiskIcon aria-hidden />}
            loading={isPending(lagreUnntakStatus)}
          >
            Lagre
          </Button>
        </HStack>
        {isFailure(lagreUnntakStatus) && (
          <ApiErrorAlert>Kunne ikke lagre unntak: {lagreUnntakStatus.error.detail}</ApiErrorAlert>
        )}
      </form>
    </FormProvider>
  )
}

function UnntakAktivitetspliktOppgave({ vurderingType }: { vurderingType: AktivitetspliktOppgaveVurderingType }) {
  const er6mndVurdering = vurderingType === AktivitetspliktOppgaveVurderingType.SEKS_MAANEDER
  const { register, control } = useFormContext()
  return (
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
              message: 'Du må velge type unntak.',
            },
          })}
          label="Hvilket unntak er det?"
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
          {er6mndVurdering && (
            <option value={AktivitetspliktUnntakType.GRADERT_UFOERETRYGD}>
              {tekstAktivitetspliktUnntakType[AktivitetspliktUnntakType.GRADERT_UFOERETRYGD]}
            </option>
          )}
        </Select>

        <Textarea {...register('beskrivelse')} label="Vurdering av unntak" />
      </VStack>
    </Box>
  )
}
