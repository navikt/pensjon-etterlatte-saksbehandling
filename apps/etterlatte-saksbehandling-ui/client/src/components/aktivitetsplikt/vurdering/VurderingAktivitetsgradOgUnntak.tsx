import {
  AktivitetspliktOppgaveVurderingType,
  AktivitetspliktSkjoennsmessigVurdering,
  AktivitetspliktUnntakType,
  AktivitetspliktVurderingType,
  IAktivitetspliktVurderingNyDto,
  IOpprettAktivitetspliktAktivitetsgrad,
  IOpprettAktivitetspliktUnntak,
  tekstAktivitetspliktUnntakType,
  tekstAktivitetspliktVurderingType,
  teksterAktivitetspliktSkjoennsmessigVurdering,
} from '~shared/types/Aktivitetsplikt'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/AktivitetspliktOppgaveVurderingRoutes'
import { addMonths, startOfMonth } from 'date-fns'
import { maanederForVurdering } from '~components/aktivitetsplikt/vurdering/aktivitetsgrad/VurderingAktivitetsgradWrapperOppgave'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettAktivitetspliktAktivitetsgradOgUnntakForOppgave } from '~shared/api/aktivitetsplikt'
import { JaNei } from '~shared/types/ISvar'
import { FormProvider, useForm, useFormContext } from 'react-hook-form'
import { Alert, Box, Button, HStack, Radio, Select, Textarea, VStack } from '@navikt/ds-react'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { isFailure, isPending, Result } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import React from 'react'

export interface NyVurderingAktivitetsgradOgUnntak {
  typeVurdering: AktivitetspliktOppgaveVurderingType
  vurderingAvAktivitet: IOpprettAktivitetspliktAktivitetsgrad
  harUnntak?: JaNei
  unntak?: IOpprettAktivitetspliktUnntak
}

export function VurderAktivitetspliktWrapper(props: {
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

  const [lagreStatus, lagreVurdering] = useApiCall(opprettAktivitetspliktAktivitetsgradOgUnntakForOppgave)

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
    <VurderingAktivitetsgradOgUnntak
      onSubmit={lagreOgOppdater}
      doedsdato={doedsdato}
      typeVurdering={typeVurdering}
      onAvbryt={onAvbryt}
      lagreStatus={lagreStatus}
      vurderingKilde={VurderingKilde.OPPGAVE}
    />
  )
}

export enum VurderingKilde {
  OPPGAVE = 'OPPGAVE',
  FOERSTEGANGSBEHANDLING = 'FOERSTEGANGSBEHANDLING',
  REVURDERING = 'REVURDERING',
}

function aktivitetsgradTekstFraKilde(
  vurderingKilde: VurderingKilde,
  vurderingType: AktivitetspliktOppgaveVurderingType
) {
  switch (vurderingKilde) {
    case VurderingKilde.FOERSTEGANGSBEHANDLING:
    case VurderingKilde.REVURDERING:
      return 'Hva er aktivitetsgraden til bruker?'
    case VurderingKilde.OPPGAVE:
      return `Hva er aktivitetsgraden til bruker ${vurderingType === 'SEKS_MAANEDER' ? 'seks måneder' : 'tolv måneder'} etter dødsfall?`
  }
}

export function VurderingAktivitetsgradOgUnntak(props: {
  doedsdato?: Date
  onAvbryt?: () => void
  onSubmit: (data: NyVurderingAktivitetsgradOgUnntak) => void
  typeVurdering: AktivitetspliktOppgaveVurderingType
  lagreStatus: Result<IAktivitetspliktVurderingNyDto>
  vurderingKilde: VurderingKilde
}) {
  const { onSubmit, onAvbryt, doedsdato, typeVurdering, lagreStatus, vurderingKilde } = props

  const defaultFom =
    vurderingKilde === VurderingKilde.OPPGAVE
      ? doedsdato
        ? startOfMonth(addMonths(doedsdato, maanederForVurdering(typeVurdering)))
        : startOfMonth(new Date())
      : new Date()

  const methods = useForm<NyVurderingAktivitetsgradOgUnntak>({
    defaultValues: {
      typeVurdering: typeVurdering,
      vurderingAvAktivitet: { fom: defaultFom.toISOString() },
    },
  })

  const { handleSubmit, register, watch, control } = methods

  const svarAktivitetsgrad = watch('vurderingAvAktivitet.aktivitetsgrad')
  // 12 mnd krever unntak for alt under 100% mens 6 mnd krever unntak for kun under 50 % aktivitetsgrad
  const skalViseUnntak =
    typeVurdering === AktivitetspliktOppgaveVurderingType.TOLV_MAANEDER
      ? svarAktivitetsgrad === AktivitetspliktVurderingType.AKTIVITET_OVER_50 ||
        svarAktivitetsgrad === AktivitetspliktVurderingType.AKTIVITET_UNDER_50
      : svarAktivitetsgrad === AktivitetspliktVurderingType.AKTIVITET_UNDER_50

  return (
    <FormProvider {...methods}>
      <form onSubmit={handleSubmit(onSubmit)}>
        <VStack gap="space-6">
          <ControlledRadioGruppe
            control={control}
            name="vurderingAvAktivitet.aktivitetsgrad"
            legend={aktivitetsgradTekstFraKilde(vurderingKilde, typeVurdering)}
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
          {vurderingKilde === VurderingKilde.FOERSTEGANGSBEHANDLING && (
            <Box maxWidth="42.5rem">
              <Alert variant="info">
                Vurder situasjonen som gjelder i dag, og om det er noen unntak. Denne vurderingen er for å dokumentere
                senere oppfølging.
              </Alert>
            </Box>
          )}
          <HStack gap="space-6">
            <ControlledDatoVelger
              name="vurderingAvAktivitet.fom"
              label="Fra og med"
              control={control}
              description={`Fra ${vurderingKilde === VurderingKilde.OPPGAVE ? 'kravet inntreffer' : 'dato oppgitt'}`}
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
          {skalViseUnntak && (
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
          {watch('harUnntak') === JaNei.JA && <UnntakValg vurderingType={typeVurdering} />}
          {typeVurdering === AktivitetspliktOppgaveVurderingType.TOLV_MAANEDER &&
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
          {vurderingKilde === VurderingKilde.OPPGAVE &&
            svarAktivitetsgrad === AktivitetspliktVurderingType.AKTIVITET_UNDER_50 &&
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

          <HStack gap="space-4">
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

const UnntakValg = (props: { vurderingType: AktivitetspliktOppgaveVurderingType }) => {
  const { register, control } = useFormContext()
  const { vurderingType } = props
  const er6mndVurdering = vurderingType === AktivitetspliktOppgaveVurderingType.SEKS_MAANEDER
  return (
    <Box maxWidth="40rem">
      <VStack gap="space-4">
        <HStack gap="space-4">
          <ControlledDatoVelger name="unntak.fom" label="Unntak fra og med" control={control} />
          <ControlledDatoVelger name="unntak.tom" label="Unntak til og med" required={false} control={control} />
        </HStack>
        <Select
          {...register('unntak.unntak', {
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
        <Textarea {...register('unntak.beskrivelse')} label="Vurdering av unntak" />
      </VStack>
    </Box>
  )
}
