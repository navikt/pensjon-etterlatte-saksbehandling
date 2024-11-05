import {
  AktivitetspliktOppgaveVurderingType,
  AktivitetspliktSkjoennsmessigVurdering,
  AktivitetspliktVurderingType,
  IAktivitetspliktAktivitetsgrad,
  IOpprettAktivitetspliktAktivitetsgrad,
  tekstAktivitetspliktVurderingType,
} from '~shared/types/Aktivitetsplikt'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettAktivitetspliktAktivitetsgrad } from '~shared/api/aktivitetsplikt'
import { useForm } from 'react-hook-form'
import { Box, Button, HStack, Radio, Textarea, VStack } from '@navikt/ds-react'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { isFailure, isPending } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import React, { useEffect } from 'react'
import { addMonths, startOfMonth } from 'date-fns'

interface NyVurdering {
  typeVurdering: AktivitetspliktOppgaveVurderingType
  vurderingAvAktivitet: IOpprettAktivitetspliktAktivitetsgrad
}

function maanederForVurdering(typeVurdering: AktivitetspliktOppgaveVurderingType): number {
  if (typeVurdering === AktivitetspliktOppgaveVurderingType.SEKS_MAANEDER) {
    return 6
  } else {
    return 12
  }
}

export function VurderingAktivitetsgradForm(props: {
  aktivitet?: IAktivitetspliktAktivitetsgrad
  doedsmaaned?: Date
  onAvbryt?: () => void
  onSuccess: () => void
}) {
  const { aktivitet, onSuccess, onAvbryt, doedsmaaned } = props
  const { oppgave } = useAktivitetspliktOppgaveVurdering()
  const typeVurdering =
    oppgave.type === 'AKTIVITETSPLIKT'
      ? AktivitetspliktOppgaveVurderingType.SEKS_MAANEDER
      : AktivitetspliktOppgaveVurderingType.TOLV_MAANEDER

  const defaultFom = doedsmaaned
    ? addMonths(doedsmaaned, maanederForVurdering(typeVurdering))
    : startOfMonth(new Date())

  const [lagreStatus, lagreVurdering, reset] = useApiCall(opprettAktivitetspliktAktivitetsgrad)

  const { handleSubmit, register, watch, control } = useForm<Partial<NyVurdering>>({
    defaultValues: {
      typeVurdering: typeVurdering,
      vurderingAvAktivitet: !!aktivitet ? aktivitet : { fom: defaultFom.toISOString() },
    },
  })

  useEffect(() => {
    reset()
  }, [aktivitet])

  function lagreOgOppdater(formdata: Partial<NyVurdering>) {
    if (!formdata.vurderingAvAktivitet?.aktivitetsgrad || !formdata.vurderingAvAktivitet.fom) {
      return
    }

    lagreVurdering(
      {
        sakId: oppgave.sakId,
        oppgaveId: oppgave.id,
        request: {
          id: aktivitet?.id,
          vurdertFra12Mnd: formdata.typeVurdering === 'TOLV_MAANEDER',
          skjoennsmessigVurdering: formdata.vurderingAvAktivitet.skjoennsmessigVurdering,
          aktivitetsgrad: formdata.vurderingAvAktivitet.aktivitetsgrad,
          fom: formdata.vurderingAvAktivitet.fom,
          beskrivelse: formdata.vurderingAvAktivitet.beskrivelse || '',
        },
      },
      onSuccess
    )
  }

  return (
    <form onSubmit={handleSubmit(lagreOgOppdater)}>
      <VStack gap="6">
        <HStack gap="6">
          <ControlledDatoVelger name="vurderingAvAktivitet.fom" label="Fra og med" control={control} />
          <ControlledDatoVelger name="vurderingAvAktivitet.tom" label="Til og med" control={control} required={false} />
        </HStack>

        <ControlledRadioGruppe
          control={control}
          name="vurderingAvAktivitet.aktivitetsgrad"
          legend="Hva er aktivitetsgraden til bruker?"
          errorVedTomInput="Du må velge en aktivitetsgrad"
          radios={
            <>
              <Radio value={AktivitetspliktVurderingType.AKTIVITET_100}>
                {tekstAktivitetspliktVurderingType[AktivitetspliktVurderingType.AKTIVITET_100]}
              </Radio>

              <Radio value={AktivitetspliktVurderingType.AKTIVITET_OVER_50}>
                {tekstAktivitetspliktVurderingType[AktivitetspliktVurderingType.AKTIVITET_OVER_50]}
              </Radio>

              <Radio value={AktivitetspliktVurderingType.AKTIVITET_UNDER_50}>
                {tekstAktivitetspliktVurderingType[AktivitetspliktVurderingType.AKTIVITET_UNDER_50]}
              </Radio>
            </>
          }
        />

        {watch('typeVurdering') === AktivitetspliktOppgaveVurderingType.TOLV_MAANEDER &&
          watch('vurderingAvAktivitet.aktivitetsgrad') === AktivitetspliktVurderingType.AKTIVITET_OVER_50 && (
            <ControlledRadioGruppe
              control={control}
              name="vurderingAvAktivitet.skjoennsmessigVurdering"
              legend="Vil bruker være selvforsørget etter stønaden utløper?"
              errorVedTomInput="Du må vurdere om bruker vil være selvforsørget ved 50-99 % grad aktivitet"
              radios={
                <>
                  <Radio value={AktivitetspliktSkjoennsmessigVurdering.JA}>Ja</Radio>
                  <Radio value={AktivitetspliktSkjoennsmessigVurdering.MED_OPPFOELGING}>Med oppfølging</Radio>
                  <Radio value={AktivitetspliktSkjoennsmessigVurdering.NEI}>Nei</Radio>
                </>
              }
            />
          )}
        <Box maxWidth="60rem">
          <Textarea {...register('vurderingAvAktivitet.beskrivelse')} label="Beskrivelse" />
        </Box>

        {isFailure(lagreStatus) && (
          <ApiErrorAlert>Kunne ikke lagre vurdering: {lagreStatus.error.detail} </ApiErrorAlert>
        )}
        <HStack gap="4">
          <Button type="submit" loading={isPending(lagreStatus)}>
            Lagre
          </Button>
          {onAvbryt && (
            <Button variant="secondary" onClick={onAvbryt}>
              Avbryt
            </Button>
          )}
        </HStack>
      </VStack>
    </form>
  )
}
