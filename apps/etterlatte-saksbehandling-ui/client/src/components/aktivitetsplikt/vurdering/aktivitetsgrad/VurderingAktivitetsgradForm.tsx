import {
  AktivitetspliktOppgaveVurderingType,
  AktivitetspliktSkjoennsmessigVurdering,
  AktivitetspliktVurderingType,
  IAktivitetspliktAktivitetsgrad,
  IAktivitetspliktVurderingNyDto,
  IOpprettAktivitetspliktAktivitetsgrad,
  tekstAktivitetspliktVurderingType,
  teksterAktivitetspliktSkjoennsmessigVurdering,
} from '~shared/types/Aktivitetsplikt'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettAktivitetspliktAktivitetsgrad } from '~shared/api/aktivitetsplikt'
import { useForm } from 'react-hook-form'
import { Alert, Box, Button, ErrorMessage, HStack, Radio, Textarea, VStack } from '@navikt/ds-react'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { isFailure, isPending } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import React, { useEffect, useState } from 'react'
import { JaNei } from '~shared/types/ISvar'

interface RedigerAktivitetsgrad {
  typeVurdering: AktivitetspliktOppgaveVurderingType
  vurderingAvAktivitet: IOpprettAktivitetspliktAktivitetsgrad
  harUnntak?: JaNei
}

export function maanederForVurdering(typeVurdering: AktivitetspliktOppgaveVurderingType): number {
  if (typeVurdering === AktivitetspliktOppgaveVurderingType.SEKS_MAANEDER) {
    return 6
  } else {
    return 12
  }
}
//TODO: formet her kunne kanskje vært kombinert med den som legger til  ny med unntak med formprovider + children men usikker på om det er verdt det
export function VurderingAktivitetsgradForm(props: {
  aktivitet: IAktivitetspliktAktivitetsgrad
  onAvbryt: () => void
  onSuccess: (data: IAktivitetspliktVurderingNyDto) => void
}) {
  const { aktivitet, onSuccess, onAvbryt } = props
  const { oppgave } = useAktivitetspliktOppgaveVurdering()
  const [feilmelding, setFeilmelding] = useState('')
  const typeVurdering =
    oppgave.type === 'AKTIVITETSPLIKT'
      ? AktivitetspliktOppgaveVurderingType.SEKS_MAANEDER
      : AktivitetspliktOppgaveVurderingType.TOLV_MAANEDER

  const [lagreStatus, lagreVurdering, reset] = useApiCall(opprettAktivitetspliktAktivitetsgrad)

  const { handleSubmit, register, watch, control } = useForm<RedigerAktivitetsgrad>({
    defaultValues: {
      typeVurdering: typeVurdering,
      vurderingAvAktivitet: aktivitet,
    },
  })

  useEffect(() => {
    reset()
  }, [aktivitet])

  function lagreOgOppdater(formdata: RedigerAktivitetsgrad) {
    setFeilmelding('')
    if (!formdata.vurderingAvAktivitet?.aktivitetsgrad || !formdata.vurderingAvAktivitet.fom) {
      setFeilmelding('Du må fylle ut vurderingen av aktivitetsgraden.')
      return
    }

    lagreVurdering(
      {
        sakId: oppgave.sakId,
        oppgaveId: oppgave.id,
        request: {
          id: aktivitet?.id,
          vurdertFra12Mnd: formdata.typeVurdering === AktivitetspliktOppgaveVurderingType.TOLV_MAANEDER,
          skjoennsmessigVurdering: formdata.vurderingAvAktivitet.skjoennsmessigVurdering,
          aktivitetsgrad: formdata.vurderingAvAktivitet.aktivitetsgrad,
          fom: formdata.vurderingAvAktivitet.fom,
          beskrivelse: formdata.vurderingAvAktivitet.beskrivelse || '',
        },
      },
      onSuccess
    )
  }

  const svarAktivitetsgrad = watch('vurderingAvAktivitet.aktivitetsgrad')

  return (
    <form onSubmit={handleSubmit(lagreOgOppdater)}>
      <VStack gap="6">
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
        <HStack gap="6">
          <ControlledDatoVelger
            name="vurderingAvAktivitet.fom"
            label="Fra og med"
            control={control}
            description="Fra dato oppgitt"
            errorVedTomInput="Du må velge fra og med dato"
          />
          <ControlledDatoVelger
            name="vurderingAvAktivitet.tom"
            label="Til og med"
            control={control}
            required={false}
            description="Hvis det er oppgitt sluttdato"
          />
        </HStack>

        {watch('typeVurdering') === AktivitetspliktOppgaveVurderingType.TOLV_MAANEDER &&
          svarAktivitetsgrad === AktivitetspliktVurderingType.AKTIVITET_OVER_50 && (
            <ControlledRadioGruppe
              control={control}
              name="vurderingAvAktivitet.skjoennsmessigVurdering"
              legend="Vil bruker være selvforsørget etter stønaden utløper?"
              errorVedTomInput="Du må vurdere om bruker vil være selvforsørget ved 50-99 % grad aktivitet"
              description="Vurderingen her blir ikke grunnlag for innhold i infobrev, men styrer hvilke oppgaver Gjenny lager for oppfølging av aktiviteten til bruker."
              radios={
                <>
                  <Radio
                    value={AktivitetspliktSkjoennsmessigVurdering.JA}
                    description="Gjenny lager ingen oppfølgingsoppgave"
                  >
                    {teksterAktivitetspliktSkjoennsmessigVurdering[AktivitetspliktSkjoennsmessigVurdering.JA]}
                  </Radio>
                  <Radio
                    value={AktivitetspliktSkjoennsmessigVurdering.MED_OPPFOELGING}
                    description="Bruker skal ha oppfølging. Gjenny oppretter oppfølgingsoppgave. Send oppgave til lokalkontor hvis det ikke er gjort."
                  >
                    {
                      teksterAktivitetspliktSkjoennsmessigVurdering[
                        AktivitetspliktSkjoennsmessigVurdering.MED_OPPFOELGING
                      ]
                    }
                  </Radio>
                  <Radio
                    value={AktivitetspliktSkjoennsmessigVurdering.NEI}
                    description="Gjenny lager en revurdering neste måned med mulighet for å varsle og fatte sanksjon"
                  >
                    {teksterAktivitetspliktSkjoennsmessigVurdering[AktivitetspliktSkjoennsmessigVurdering.NEI]}
                  </Radio>
                </>
              }
            />
          )}
        {svarAktivitetsgrad === AktivitetspliktVurderingType.AKTIVITET_UNDER_50 && watch('harUnntak') !== JaNei.JA && (
          <Box maxWidth="50rem">
            <Alert variant="info">
              Basert på vurderingen du har gjort vil det bli opprettet en revurdering for mulig sanksjon.
            </Alert>
          </Box>
        )}
        <Box maxWidth="60rem">
          <Textarea
            {...register('vurderingAvAktivitet.beskrivelse')}
            label="Beskrivelse"
            description="Beskriv hvordan du har vurdert brukers situasjon"
          />
        </Box>

        {isFailure(lagreStatus) && (
          <ApiErrorAlert>Kunne ikke lagre vurdering: {lagreStatus.error.detail} </ApiErrorAlert>
        )}
        {feilmelding.length > 0 && <ErrorMessage>{feilmelding}</ErrorMessage>}

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
