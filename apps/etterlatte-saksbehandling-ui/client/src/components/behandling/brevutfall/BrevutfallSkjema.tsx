import { Alert, Button, HStack, Radio, Textarea, VStack } from '@navikt/ds-react'
import React, { ReactNode } from 'react'
import { SakType } from '~shared/types/sak'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreBrevutfallApi } from '~shared/api/behandling'
import { IBehandlingsType, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { isFailure, isPending } from '~shared/api/apiUtils'
import {
  Aldersgruppe,
  BrevutfallOgEtterbetaling,
  EtterbetalingPeriodeValg,
  FeilutbetalingValg,
} from '~components/behandling/brevutfall/Brevutfall'
import { add, formatISO, lastDayOfMonth, startOfDay } from 'date-fns'
import { updateBrevutfallOgEtterbetaling } from '~store/reducers/BehandlingReducer'
import { useAppDispatch } from '~store/Store'
import { Controller, useForm } from 'react-hook-form'
import { ControlledMaanedVelger } from '~shared/components/maanedVelger/ControlledMaanedVelger'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { EtterbetalingHjelpeTekst } from '~components/behandling/brevutfall/hjelpeTekster/EtterbetalingHjelpeTekst'
import { AldersgruppeHjelpeTekst } from '~components/behandling/brevutfall/hjelpeTekster/AldersgruppeHjelpeTekst'
import { FeilutbetalingHjelpeTekst } from '~components/behandling/brevutfall/hjelpeTekster/FeilutbetalingHjelpeTekst'
import { feilutbetalingToString } from '~components/behandling/brevutfall/BrevutfallVisning'
import { ISvar } from '~shared/types/ISvar'

interface BrevutfallSkjemaData {
  harEtterbetaling: ISvar | null
  datoFom?: Date | null
  datoTom?: Date | null
  kravIEtterbetaling: ISvar | null
  frivilligSkattetrekk: ISvar | null
  etterbetalingPeriodeValg: EtterbetalingPeriodeValg | null
  skatteTrekkFomTomDatoSatt: ISvar | null
  aldersgruppe?: Aldersgruppe | null
  feilutbetalingValg?: FeilutbetalingValg | null
  feilutbetalingKommentar: string | null
}

interface Props {
  behandlingErOpphoer: boolean
  behandling: IDetaljertBehandling
  brevutfallOgEtterbetaling: BrevutfallOgEtterbetaling
  setBrevutfallOgEtterbetaling: (brevutfall: BrevutfallOgEtterbetaling) => void
  setVisSkjema: (visSkjema: boolean) => void
  resetBrevutfallvalidering: () => void
  onAvbryt: () => void
}

export const BrevutfallSkjema = ({
  behandlingErOpphoer,
  behandling,
  brevutfallOgEtterbetaling,
  setBrevutfallOgEtterbetaling,
  setVisSkjema,
  resetBrevutfallvalidering,
  onAvbryt,
}: Props): ReactNode => {
  const [lagreBrevutfallResultat, lagreBrevutfallRequest, lagreBrevutfallReset] = useApiCall(lagreBrevutfallApi)
  const dispatch = useAppDispatch()

  const { handleSubmit, control, getValues, watch } = useForm<BrevutfallSkjemaData>({
    defaultValues: {
      harEtterbetaling:
        brevutfallOgEtterbetaling.etterbetaling === undefined
          ? undefined
          : brevutfallOgEtterbetaling.etterbetaling
            ? ISvar.JA
            : ISvar.NEI,
      kravIEtterbetaling:
        brevutfallOgEtterbetaling.etterbetaling?.inneholderKrav === undefined
          ? undefined
          : brevutfallOgEtterbetaling.etterbetaling?.inneholderKrav
            ? ISvar.JA
            : ISvar.NEI,
      frivilligSkattetrekk:
        brevutfallOgEtterbetaling.etterbetaling?.frivilligSkattetrekk === undefined
          ? undefined
          : brevutfallOgEtterbetaling.etterbetaling?.frivilligSkattetrekk
            ? ISvar.JA
            : ISvar.NEI,
      etterbetalingPeriodeValg: brevutfallOgEtterbetaling.etterbetaling?.etterbetalingPeriodeValg,
      skatteTrekkFomTomDatoSatt:
        brevutfallOgEtterbetaling.etterbetaling?.skatteTrekkFomTomDatoSatt === undefined
          ? undefined
          : brevutfallOgEtterbetaling.etterbetaling?.skatteTrekkFomTomDatoSatt
            ? ISvar.JA
            : ISvar.NEI,
      datoFom: brevutfallOgEtterbetaling.etterbetaling?.datoFom
        ? new Date(brevutfallOgEtterbetaling.etterbetaling?.datoFom)
        : undefined,
      datoTom: brevutfallOgEtterbetaling.etterbetaling?.datoFom
        ? new Date(brevutfallOgEtterbetaling.etterbetaling?.datoFom)
        : undefined,
      aldersgruppe: brevutfallOgEtterbetaling.brevutfall.aldersgruppe,
      feilutbetalingValg: brevutfallOgEtterbetaling.brevutfall.feilutbetaling?.valg,
      feilutbetalingKommentar: brevutfallOgEtterbetaling.brevutfall.feilutbetaling?.kommentar ?? '',
    },
  })

  const submitBrevutfall = (data: BrevutfallSkjemaData) => {
    lagreBrevutfallReset()

    const brevutfall: BrevutfallOgEtterbetaling = {
      opphoer: behandlingErOpphoer,
      brevutfall: {
        aldersgruppe: data.aldersgruppe,
        feilutbetaling: data.feilutbetalingValg
          ? { valg: data.feilutbetalingValg, kommentar: data.feilutbetalingKommentar }
          : null,
      },
      etterbetaling:
        data.harEtterbetaling === ISvar.JA
          ? {
              datoFom: formatISO(data.datoFom!, { representation: 'date' }),
              datoTom: formatISO(data.datoTom!, { representation: 'date' }),
              inneholderKrav: data.kravIEtterbetaling === ISvar.JA,
              frivilligSkattetrekk: data.frivilligSkattetrekk === ISvar.JA,
              etterbetalingPeriodeValg: data.etterbetalingPeriodeValg,
              skatteTrekkFomTomDatoSatt:
                data.frivilligSkattetrekk === ISvar.JA ? data.skatteTrekkFomTomDatoSatt === ISvar.JA : null,
            }
          : null,
    }

    lagreBrevutfallRequest(
      {
        behandlingId: behandling.id,
        brevutfall,
      },
      (brevutfall: BrevutfallOgEtterbetaling) => {
        resetBrevutfallvalidering()
        setBrevutfallOgEtterbetaling(brevutfall)
        dispatch(updateBrevutfallOgEtterbetaling(brevutfall))
        setVisSkjema(false)
      }
    )
  }

  const validerFom = (value: Date): string | undefined => {
    const fom = startOfDay(new Date(value))
    const tom = startOfDay(new Date(getValues().datoTom!))

    if (fom > tom) {
      return 'Fra-måned kan ikke være etter til-måned.'
    }
    // Til og med kan ikke settes lenger frem enn inneværende måned. Inneværende måned vil måtte etterbetales
    // dersom utbetaling allerede er kjørt.
    else if (behandling.virkningstidspunkt?.dato && fom < startOfDay(new Date(behandling.virkningstidspunkt.dato))) {
      return 'Fra-måned før virkningstidspunkt.'
    }
    return undefined
  }

  const validerTom = (value: Date): string | undefined => {
    const tom = startOfDay(new Date(value))

    if (tom > startOfDay(lastDayOfMonth(new Date()))) {
      return 'Til-måned etter inneværende måned.'
    }
    return undefined
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

            {watch().harEtterbetaling == ISvar.JA && (
              <HStack gap="4">
                <ControlledMaanedVelger
                  fromDate={new Date(behandling.virkningstidspunkt?.dato ?? new Date())}
                  toDate={new Date()}
                  name="datoFom"
                  label="Fra og med"
                  control={control}
                  validate={validerFom}
                  required
                />

                <ControlledMaanedVelger
                  name="datoTom"
                  label="Til og med"
                  fromDate={new Date(behandling.virkningstidspunkt?.dato ?? new Date())}
                  toDate={add(new Date(), { months: 1 })}
                  control={control}
                  validate={validerTom}
                  required
                />
                {behandling.sakType == SakType.BARNEPENSJON && (
                  <>
                    <ControlledRadioGruppe
                      name="kravIEtterbetaling"
                      control={control}
                      errorVedTomInput="Du må velge om det er krav i etterbetalingen"
                      legend={<HStack gap="2">Er det krav i etterbetalingen?</HStack>}
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
                    <ControlledRadioGruppe
                      name="frivilligSkattetrekk"
                      control={control}
                      errorVedTomInput="Du må velge om bruker har meldt inn frivillig skattetrekk"
                      legend={<HStack gap="2">Har bruker meldt inn frivillig skattetrekk?</HStack>}
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
                    <ControlledRadioGruppe
                      name="etterbetalingPeriodeValg"
                      control={control}
                      errorVedTomInput="Velg hvor lang etterbetalingsperiode det er"
                      legend={<HStack gap="2">Hvor mange måneder etterbetales det for?</HStack>}
                      radios={
                        <>
                          <Radio size="small" value={EtterbetalingPeriodeValg.UNDER_3_MND}>
                            Etterbetaling 1 - 2 måneder
                          </Radio>
                          <Radio size="small" value={EtterbetalingPeriodeValg.FRA_3_MND}>
                            Etterbetaling fra 3 måneder
                          </Radio>
                        </>
                      }
                    />
                    {watch().frivilligSkattetrekk == ISvar.JA && (
                      <ControlledRadioGruppe
                        name="skatteTrekkFomTomDatoSatt"
                        control={control}
                        errorVedTomInput="Du må velge om det er lagt inn til og med dato på skattetrekk"
                        legend={<HStack gap="2">Er det lagt inn til og med dato på skattetrekk?</HStack>}
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
                    )}
                  </>
                )}
              </HStack>
            )}
          </VStack>
        )}

        {behandling.sakType == SakType.BARNEPENSJON && (
          <VStack gap="4">
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

            {watch().feilutbetalingValg && (
              <Controller
                name="feilutbetalingKommentar"
                render={(props) => (
                  <Textarea
                    label="Kommentar"
                    value={watch().feilutbetalingKommentar ?? ''}
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
