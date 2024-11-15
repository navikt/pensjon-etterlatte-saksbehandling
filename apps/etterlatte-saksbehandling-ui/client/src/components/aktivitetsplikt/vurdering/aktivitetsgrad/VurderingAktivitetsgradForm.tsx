import {
  AktivitetspliktOppgaveVurderingType,
  AktivitetspliktSkjoennsmessigVurdering,
  AktivitetspliktVurderingType,
  IAktivitetspliktAktivitetsgrad,
  IAktivitetspliktUnntak,
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
import { addMonths, startOfMonth } from 'date-fns'
import { JaNei } from '~shared/types/ISvar'

interface NyVurdering {
  typeVurdering: AktivitetspliktOppgaveVurderingType
  vurderingAvAktivitet: IOpprettAktivitetspliktAktivitetsgrad
  harUnntak?: JaNei
  unntak?: Partial<IAktivitetspliktUnntak>
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
  doedsdato?: Date
  onAvbryt?: () => void
  onSuccess: (data: IAktivitetspliktVurderingNyDto) => void
}) {
  const { aktivitet, onSuccess, onAvbryt, doedsdato } = props
  const { oppgave } = useAktivitetspliktOppgaveVurdering()
  const [feilmelding, setFeilmelding] = useState('')
  const typeVurdering =
    oppgave.type === 'AKTIVITETSPLIKT'
      ? AktivitetspliktOppgaveVurderingType.SEKS_MAANEDER
      : AktivitetspliktOppgaveVurderingType.TOLV_MAANEDER

  const defaultFom = doedsdato
    ? startOfMonth(addMonths(doedsdato, maanederForVurdering(typeVurdering)))
    : startOfMonth(new Date())

  const [lagreStatus, lagreVurdering, reset] = useApiCall(opprettAktivitetspliktAktivitetsgrad)

  const { handleSubmit, register, watch, control } = useForm<Partial<NyVurdering>>({
    defaultValues: {
      typeVurdering: typeVurdering,
      vurderingAvAktivitet: !!aktivitet ? aktivitet : { fom: defaultFom.toISOString() },
    },
  })

  const erNyVurdering = aktivitet === undefined

  useEffect(() => {
    reset()
  }, [aktivitet])

  function lagreOgOppdater(formdata: Partial<NyVurdering>) {
    setFeilmelding('')
    if (!formdata.vurderingAvAktivitet?.aktivitetsgrad || !formdata.vurderingAvAktivitet.fom) {
      setFeilmelding('Du må fylle ut vurderingen av aktivitetsgraden.')
      return
    }
    if (formdata.harUnntak === JaNei.JA) {
      setFeilmelding('Du kan ikke lagre ned unntak i vurderingen enda.')
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

  const svarAktivitetsgrad = watch('vurderingAvAktivitet.aktivitetsgrad')
  const harKanskjeUnntak =
    erNyVurdering &&
    (svarAktivitetsgrad === AktivitetspliktVurderingType.AKTIVITET_OVER_50 ||
      svarAktivitetsgrad === AktivitetspliktVurderingType.AKTIVITET_UNDER_50)

  return (
    <form onSubmit={handleSubmit(lagreOgOppdater)}>
      <VStack gap="6">
        <HStack gap="6">
          <ControlledDatoVelger
            name="vurderingAvAktivitet.fom"
            label="Fra og med"
            control={control}
            description="Fra dato oppgitt"
          />
          <ControlledDatoVelger
            name="vurderingAvAktivitet.tom"
            label="Til og med"
            control={control}
            required={false}
            description="Hvis det er oppgitt sluttdato"
          />
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

        {harKanskjeUnntak && (
          <ControlledRadioGruppe
            name="harUnntak"
            control={control}
            legend="Er det unntak for bruker?"
            radios={
              <>
                <Radio value={JaNei.JA}>Ja</Radio>
                <Radio value={JaNei.NEI}>Nei</Radio>
              </>
            }
            errorVedTomInput="Du må svare om bruker har unntak"
          />
        )}

        {watch('harUnntak') === JaNei.JA && harKanskjeUnntak && (
          <Box maxWidth="50rem">
            <Alert variant="info">
              Du kan ikke legge til unntak enda. Vi jobber med å få dette på plass. Du kan ta opp denne behandlingen
              igjen neste uke.
            </Alert>
          </Box>
        )}

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
                  <Radio value={AktivitetspliktSkjoennsmessigVurdering.JA}>
                    {teksterAktivitetspliktSkjoennsmessigVurdering[AktivitetspliktSkjoennsmessigVurdering.JA]}
                  </Radio>
                  <Radio value={AktivitetspliktSkjoennsmessigVurdering.MED_OPPFOELGING}>
                    {
                      teksterAktivitetspliktSkjoennsmessigVurdering[
                        AktivitetspliktSkjoennsmessigVurdering.MED_OPPFOELGING
                      ]
                    }
                  </Radio>
                  <Radio value={AktivitetspliktSkjoennsmessigVurdering.NEI}>
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
