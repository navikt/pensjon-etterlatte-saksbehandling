import {
  AktivitetspliktOppgaveVurderingType,
  AktivitetspliktSkjoennsmessigVurdering,
  AktivitetspliktVurderingType,
  IAktivitetspliktVurderingNyDto,
  IOpprettAktivitetspliktAktivitetsgrad,
  tekstAktivitetspliktVurderingType,
  teksterAktivitetspliktSkjoennsmessigVurdering,
} from '~shared/types/Aktivitetsplikt'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'
import { addMonths, startOfMonth } from 'date-fns'
import { maanederForVurdering } from '~components/aktivitetsplikt/vurdering/aktivitetsgrad/VurderingAktivitetsgradForm'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettAktivitetspliktAktivitetsgradOgUnntak } from '~shared/api/aktivitetsplikt'
import { JaNei } from '~shared/types/ISvar'
import {
  IOpprettAktivitetspliktUnntak,
  UnntakAktivitetspliktOppgave,
} from '~components/aktivitetsplikt/vurdering/unntak/UnntakAktivitetspliktOppgave'
import { FormProvider, useForm } from 'react-hook-form'
import { Alert, Box, Button, HStack, Radio, Textarea, VStack } from '@navikt/ds-react'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { isFailure, isPending } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import React from 'react'

export interface NyVurderingAktivitetsgradOgUnntak {
  typeVurdering: AktivitetspliktOppgaveVurderingType
  vurderingAvAktivitet: IOpprettAktivitetspliktAktivitetsgrad
  harUnntak?: JaNei
  unntak?: IOpprettAktivitetspliktUnntak
}

export function VurderingAktivitetsgradOgUnntak(props: {
  doedsdato?: Date
  onAvbryt?: () => void
  onSuccess: (data: IAktivitetspliktVurderingNyDto) => void
}) {
  const { onSuccess, onAvbryt, doedsdato } = props
  const { oppgave } = useAktivitetspliktOppgaveVurdering()
  const typeVurdering =
    oppgave.type === 'AKTIVITETSPLIKT'
      ? AktivitetspliktOppgaveVurderingType.SEKS_MAANEDER
      : AktivitetspliktOppgaveVurderingType.TOLV_MAANEDER

  const defaultFom = doedsdato
    ? startOfMonth(addMonths(doedsdato, maanederForVurdering(typeVurdering)))
    : startOfMonth(new Date())

  const [lagreStatus, lagreVurdering] = useApiCall(opprettAktivitetspliktAktivitetsgradOgUnntak)

  const methods = useForm<NyVurderingAktivitetsgradOgUnntak>({
    defaultValues: {
      typeVurdering: typeVurdering,
      vurderingAvAktivitet: { fom: defaultFom.toISOString() },
    },
  })

  const { handleSubmit, register, watch, control } = methods

  const svarAktivitetsgrad = watch('vurderingAvAktivitet.aktivitetsgrad')
  const harKanskjeUnntak =
    svarAktivitetsgrad === AktivitetspliktVurderingType.AKTIVITET_OVER_50 ||
    svarAktivitetsgrad === AktivitetspliktVurderingType.AKTIVITET_UNDER_50

  function lagreOgOppdater(formdata: NyVurderingAktivitetsgradOgUnntak) {
    lagreVurdering(
      {
        sakId: oppgave.sakId,
        oppgaveId: oppgave.id,
        request: {
          aktivitetsgrad: {
            id: undefined,
            vurdertFra12Mnd: formdata.typeVurdering === AktivitetspliktOppgaveVurderingType.TOLV_MAANEDER,
            skjoennsmessigVurdering: formdata.vurderingAvAktivitet.skjoennsmessigVurdering,
            aktivitetsgrad: formdata.vurderingAvAktivitet.aktivitetsgrad,
            fom: formdata.vurderingAvAktivitet.fom,
            beskrivelse: formdata.vurderingAvAktivitet.beskrivelse || '',
          },
          unntak: formdata.unntak,
        },
      },
      onSuccess
    )
  }

  return (
    <FormProvider {...methods}>
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

          {watch('harUnntak') === JaNei.JA && <UnntakAktivitetspliktOppgave formPrefix="unntak." />}

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
          {svarAktivitetsgrad === AktivitetspliktVurderingType.AKTIVITET_UNDER_50 &&
            watch('harUnntak') !== JaNei.JA && (
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
    </FormProvider>
  )
}
