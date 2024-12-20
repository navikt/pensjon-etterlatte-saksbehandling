import { Alert, Button, HStack, Radio, Textarea, VStack } from '@navikt/ds-react'
import React, { ReactNode } from 'react'
import { SakType } from '~shared/types/sak'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreBrevutfallApi } from '~shared/api/behandling'
import { IBehandlingsType, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { isFailure, isPending } from '~shared/api/apiUtils'
import { Aldersgruppe, Brevutfall, FeilutbetalingValg } from '~components/behandling/brevutfall/Brevutfall'
import { updateBrevutfall } from '~store/reducers/BehandlingReducer'
import { useAppDispatch } from '~store/Store'
import { Controller, useForm } from 'react-hook-form'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { EtterbetalingHjelpeTekst } from '~components/behandling/brevutfall/hjelpeTekster/EtterbetalingHjelpeTekst'
import { AldersgruppeHjelpeTekst } from '~components/behandling/brevutfall/hjelpeTekster/AldersgruppeHjelpeTekst'
import { FeilutbetalingHjelpeTekst } from '~components/behandling/brevutfall/hjelpeTekster/FeilutbetalingHjelpeTekst'
import { feilutbetalingToString } from '~components/behandling/brevutfall/BrevutfallVisning'
import { ISvar } from '~shared/types/ISvar'

interface BrevutfallSkjemaData {
  harEtterbetaling: ISvar | null
  frivilligSkattetrekk: ISvar | null
  aldersgruppe?: Aldersgruppe | null
  feilutbetalingValg?: FeilutbetalingValg | null
  feilutbetalingKommentar: string | null
}

interface Props {
  behandlingErOpphoer: boolean
  behandling: IDetaljertBehandling
  brevutfall: Brevutfall
  setBrevutfall: (brevutfall: Brevutfall) => void
  setVisSkjema: (visSkjema: boolean) => void
  resetBrevutfallvalidering: () => void
  onAvbryt: () => void
}

export const BrevutfallSkjema = ({
  behandlingErOpphoer,
  behandling,
  brevutfall,
  setBrevutfall,
  setVisSkjema,
  resetBrevutfallvalidering,
  onAvbryt,
}: Props): ReactNode => {
  const [lagreBrevutfallResultat, lagreBrevutfallRequest, lagreBrevutfallReset] = useApiCall(lagreBrevutfallApi)
  const dispatch = useAppDispatch()

  const { handleSubmit, control, watch } = useForm<BrevutfallSkjemaData>({
    defaultValues: {
      harEtterbetaling: brevutfall === undefined ? undefined : brevutfall.harEtterbetaling ? ISvar.JA : ISvar.NEI,
      frivilligSkattetrekk:
        brevutfall?.frivilligSkattetrekk === undefined
          ? undefined
          : brevutfall?.frivilligSkattetrekk
            ? ISvar.JA
            : ISvar.NEI,
      aldersgruppe: brevutfall.aldersgruppe,
      feilutbetalingValg: brevutfall.feilutbetaling?.valg,
      feilutbetalingKommentar: brevutfall.feilutbetaling?.kommentar ?? '',
    },
  })

  const submitBrevutfall = (data: BrevutfallSkjemaData) => {
    lagreBrevutfallReset()

    const brevutfall: Brevutfall = {
      opphoer: behandlingErOpphoer,
      aldersgruppe: data.aldersgruppe,
      feilutbetaling: data.feilutbetalingValg
        ? { valg: data.feilutbetalingValg, kommentar: data.feilutbetalingKommentar }
        : null,
      frivilligSkattetrekk: data.frivilligSkattetrekk ? data.frivilligSkattetrekk === ISvar.JA : null,
      harEtterbetaling: data.harEtterbetaling ? data.harEtterbetaling === ISvar.JA : null,
    }

    lagreBrevutfallRequest(
      {
        behandlingId: behandling.id,
        brevutfall,
      },
      (brevutfall: Brevutfall) => {
        resetBrevutfallvalidering()
        setBrevutfall(brevutfall)
        dispatch(updateBrevutfall(brevutfall))
        setVisSkjema(false)
      }
    )
  }

  return (
    <form onSubmit={handleSubmit((data) => submitBrevutfall(data))}>
      <VStack gap="8">
        {!behandlingErOpphoer && (
          <VStack gap="4">
            <ControlledRadioGruppe
              name="harEtterbetaling"
              control={control}
              errorVedTomInput="Du må velge om det skal være etterbetaling eller ikke"
              legend={<EtterbetalingHjelpeTekst />}
              radios={
                <>
                  <Radio size="small" value={ISvar.JA}>
                    Ja
                  </Radio>
                  <Radio size="small" value={ISvar.NEI}>
                    Nei
                  </Radio>
                </>
              }
            />

            {behandling.sakType == SakType.BARNEPENSJON && (
              <VStack gap="4">
                <HStack gap="4">
                  <ControlledRadioGruppe
                    name="frivilligSkattetrekk"
                    control={control}
                    errorVedTomInput="Du må velge om bruker har meldt inn frivillig skattetrekk utover 17%"
                    legend={<HStack gap="2">Har bruker meldt inn frivillig skattetrekk utover 17%?</HStack>}
                    radios={
                      <>
                        <Radio size="small" value={ISvar.JA}>
                          Ja
                        </Radio>
                        <Radio size="small" value={ISvar.NEI}>
                          Nei
                        </Radio>
                      </>
                    }
                  />
                </HStack>

                <ControlledRadioGruppe
                  name="aldersgruppe"
                  control={control}
                  errorVedTomInput="Du må velge om brevet gjelder under eller over 18 år"
                  legend={<AldersgruppeHjelpeTekst />}
                  radios={
                    <>
                      <Radio size="small" value={Aldersgruppe.UNDER_18}>
                        Under 18 år
                      </Radio>
                      <Radio size="small" value={Aldersgruppe.OVER_18}>
                        Over 18 år
                      </Radio>
                    </>
                  }
                />
              </VStack>
            )}
          </VStack>
        )}

        {IBehandlingsType.REVURDERING == behandling.behandlingType && (
          <>
            <VStack gap="4">
              <ControlledRadioGruppe
                name="feilutbetalingValg"
                control={control}
                errorVedTomInput="Du må velge om det er feilutbetaling eller ikke"
                legend={<FeilutbetalingHjelpeTekst />}
                radios={
                  <>
                    {Object.keys(FeilutbetalingValg).map((option) => (
                      <Radio key={option} size="small" value={option}>
                        {feilutbetalingToString(option as FeilutbetalingValg)}
                      </Radio>
                    ))}
                  </>
                }
              />
            </VStack>

            {watch('feilutbetalingValg') && (
              <Controller
                name="feilutbetalingKommentar"
                render={(props) => (
                  <Textarea
                    label="Kommentar"
                    value={watch('feilutbetalingKommentar') ?? ''}
                    style={{ width: '100%' }}
                    {...props}
                    onChange={(e) => {
                      props.field.onChange(e.target.value)
                    }}
                  />
                )}
                control={control}
              />
            )}
          </>
        )}

        <HStack gap="4">
          <Button size="small" type="submit" loading={isPending(lagreBrevutfallResultat)}>
            Lagre valg
          </Button>
          <Button
            variant="secondary"
            size="small"
            onClick={() => {
              onAvbryt()
              setVisSkjema(false)
            }}
          >
            Avbryt
          </Button>
        </HStack>

        {isFailure(lagreBrevutfallResultat) && <Alert variant="error">{lagreBrevutfallResultat.error.detail}</Alert>}
      </VStack>
    </form>
  )
}
