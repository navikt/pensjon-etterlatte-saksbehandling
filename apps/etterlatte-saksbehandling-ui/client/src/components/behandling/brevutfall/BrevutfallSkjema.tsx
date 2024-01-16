import { Alert, Button, HStack, Radio, VStack } from '@navikt/ds-react'
import React, { ReactNode } from 'react'
import { SakType } from '~shared/types/sak'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreBrevutfallApi } from '~shared/api/behandling'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { isFailure, isPending } from '~shared/api/apiUtils'
import { Aldersgruppe, BrevutfallOgEtterbetaling } from '~components/behandling/brevutfall/Brevutfall'
import { add, formatISO, lastDayOfMonth, startOfDay } from 'date-fns'
import { updateBrevutfallOgEtterbetaling } from '~store/reducers/BehandlingReducer'
import { useAppDispatch } from '~store/Store'
import { useForm } from 'react-hook-form'
import { ControlledMaanedVelger } from '~shared/components/maanedVelger/ControlledMaanedVelger'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { EtterbetalingHjelpeTekst } from '~components/behandling/brevutfall/hjelpeTekster/EtterbetalingHjelpeTekst'
import { AldersgruppeHjelpeTekst } from '~components/behandling/brevutfall/hjelpeTekster/AldersgruppeHjelpeTekst'

enum HarEtterbetaling {
  JA = 'JA',
  NEI = 'NEI',
}

interface BrevutfallSkjemaData {
  harEtterbetaling: HarEtterbetaling
  datoFom?: Date
  datoTom?: Date
  aldersgruppe?: Aldersgruppe
}

interface Props {
  behandling: IDetaljertBehandling
  setBrevutfallOgEtterbetaling: (brevutfall: BrevutfallOgEtterbetaling) => void
  setVisSkjema: (visSkjema: boolean) => void
  resetBrevutfallvalidering: () => void
  onAvbryt: () => void
}

export const BrevutfallSkjema = ({
  behandling,
  setBrevutfallOgEtterbetaling,
  setVisSkjema,
  resetBrevutfallvalidering,
  onAvbryt,
}: Props): ReactNode => {
  const [lagreBrevutfallResultat, lagreBrevutfallRequest, lagreBrevutfallReset] = useApiCall(lagreBrevutfallApi)

  const dispatch = useAppDispatch()

  const { handleSubmit, control, getValues, watch } = useForm<BrevutfallSkjemaData>()

  const submitBrevutfall = (data: BrevutfallSkjemaData) => {
    lagreBrevutfallReset()

    const brevutfall: BrevutfallOgEtterbetaling = {
      brevutfall: {
        aldersgruppe: data.aldersgruppe,
      },
      etterbetaling:
        data.datoFom && data.datoTom
          ? {
              datoFom: formatISO(data.datoFom, { representation: 'date' }),
              datoTom: formatISO(data.datoTom, { representation: 'date' }),
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

    if (!value) {
      return 'Fra-måned må settes'
    } else if (fom > tom) {
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

    if (!value) {
      return 'Til-måned må settes'
    } else if (tom > startOfDay(lastDayOfMonth(new Date()))) {
      return 'Til-måned etter inneværende måned.'
    }
    return undefined
  }

  return (
    <form onSubmit={handleSubmit((data) => submitBrevutfall(data))}>
      <VStack gap="8">
        <VStack gap="4">
          <ControlledRadioGruppe
            name="harEtterbetaling"
            control={control}
            errorVedTomInput="Du må velge om det skal være etterbetaling eller ikke"
            legend={<EtterbetalingHjelpeTekst />}
            radios={
              <>
                <Radio size="small" value={HarEtterbetaling.JA}>
                  Ja
                </Radio>
                <Radio size="small" value={HarEtterbetaling.NEI}>
                  Nei
                </Radio>
              </>
            }
          />

          {watch().harEtterbetaling == HarEtterbetaling.JA && (
            <HStack gap="4">
              <ControlledMaanedVelger
                fromDate={new Date(behandling.virkningstidspunkt?.dato ?? new Date())}
                toDate={new Date()}
                name="datoFom"
                label="Fra og med"
                control={control}
                validate={validerFom}
              />

              <ControlledMaanedVelger
                name="datoTom"
                label="Til og med"
                fromDate={new Date(behandling.virkningstidspunkt?.dato ?? new Date())}
                toDate={add(new Date(), { months: 1 })}
                control={control}
                validate={validerTom}
              />
            </HStack>
          )}
        </VStack>

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
