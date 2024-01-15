import styled from 'styled-components'
import { Alert, Button, HelpText, HStack, Radio, RadioGroup, VStack } from '@navikt/ds-react'
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
import { Controller, useForm } from 'react-hook-form'
import { ControlledMaanedVelger } from '~shared/components/maanedVelger/ControlledMaanedVelger'

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
  brevutfallOgEtterbetaling: BrevutfallOgEtterbetaling
  setBrevutfallOgEtterbetaling: (brevutfall: BrevutfallOgEtterbetaling) => void
  setVisSkjema: (visSkjema: boolean) => void
  resetBrevutfallvalidering: () => void
  onAvbryt: () => void
}

export const BrevutfallSkjema = ({
  behandling,
  brevutfallOgEtterbetaling,
  setBrevutfallOgEtterbetaling,
  setVisSkjema,
  resetBrevutfallvalidering,
  onAvbryt,
}: Props): ReactNode => {
  const [lagreBrevutfallResultat, lagreBrevutfallRequest, lagreBrevutfallReset] = useApiCall(lagreBrevutfallApi)

  const dispatch = useAppDispatch()

  const {
    handleSubmit,
    control,
    formState: { errors },
    getValues,
    watch,
  } = useForm<BrevutfallSkjemaData>({
    defaultValues: {
      harEtterbetaling: brevutfallOgEtterbetaling.etterbetaling
        ? brevutfallOgEtterbetaling.etterbetaling
          ? HarEtterbetaling.JA
          : HarEtterbetaling.NEI
        : undefined,
    },
  })

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
          <Controller
            name="harEtterbetaling"
            rules={{
              required: {
                value: true,
                message: 'Du må velge om det skal være etterbetaling eller ikke',
              },
            }}
            control={control}
            render={({ field: { onChange } }) => (
              <RadioGroup
                onChange={onChange}
                className="radioGroup"
                error={errors.harEtterbetaling?.message}
                legend={
                  <HelpTextWrapper>
                    Skal det etterbetales?
                    <HelpText strategy="fixed">
                      Velg ja hvis ytelsen er innvilget tilbake i tid og det blir utbetalt mer enn ett månedsbeløp. Da
                      skal du registrere perioden fra innvilgelsesmåned til og med måneden som er klar for utbetaling.
                      Vedlegg om etterbetaling skal da bli med i brevet.
                    </HelpText>
                  </HelpTextWrapper>
                }
              >
                <Radio size="small" value={HarEtterbetaling.JA}>
                  Ja
                </Radio>
                <Radio size="small" value={HarEtterbetaling.NEI}>
                  Nei
                </Radio>
              </RadioGroup>
            )}
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
            <Controller
              name="aldersgruppe"
              control={control}
              rules={{
                required: {
                  value: true,
                  message: 'Du må velge om brevet gjelder under eller over 18 år',
                },
              }}
              render={({ field: { onChange } }) => (
                <RadioGroup
                  className="radioGroup"
                  onChange={onChange}
                  error={errors.aldersgruppe?.message}
                  legend={
                    <HelpTextWrapper>
                      Gjelder brevet under eller over 18 år?
                      <HelpText strategy="fixed">
                        Velg her gjeldende alternativ for barnet, slik at riktig informasjon kommer med i vedlegg 2. For
                        barn under 18 år skal det stå &quot;Informasjon til deg som handler på vegne av barnet&quot;,
                        mens for barn over 18 år skal det stå &quot;Informasjon til deg som mottar barnepensjon&quot;.
                      </HelpText>
                    </HelpTextWrapper>
                  }
                >
                  <Radio size="small" value={Aldersgruppe.UNDER_18}>
                    Under 18 år
                  </Radio>
                  <Radio size="small" value={Aldersgruppe.OVER_18}>
                    Over 18 år
                  </Radio>
                </RadioGroup>
              )}
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

const HelpTextWrapper = styled.div`
  display: flex;
  gap: 0.5em;
`
