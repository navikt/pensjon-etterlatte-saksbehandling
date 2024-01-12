import styled from 'styled-components'
import { Alert, Button, ErrorSummary, HelpText, HStack, Radio, RadioGroup, VStack } from '@navikt/ds-react'
import React, { useState } from 'react'
import MaanedVelger from '~components/behandling/beregningsgrunnlag/MaanedVelger'
import { SakType } from '~shared/types/sak'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreBrevutfallApi } from '~shared/api/behandling'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { isFailure, isPending } from '~shared/api/apiUtils'
import { Aldersgruppe, BrevutfallOgEtterbetaling } from '~components/behandling/brevutfall/Brevutfall'
import { add, formatISO, lastDayOfMonth, startOfDay, startOfMonth } from 'date-fns'
import { updateBrevutfallOgEtterbetaling } from '~store/reducers/BehandlingReducer'
import { useAppDispatch } from '~store/Store'

enum HarEtterbetaling {
  JA = 'JA',
  NEI = 'NEI',
  IKKE_VALGT = 'IKKE_VALGT',
}

export const BrevutfallSkjema = (props: {
  behandling: IDetaljertBehandling
  brevutfallOgEtterbetaling: BrevutfallOgEtterbetaling
  setBrevutfallOgEtterbetaling: (brevutfall: BrevutfallOgEtterbetaling) => void
  setVisSkjema: (visSkjema: boolean) => void
  resetBrevutfallvalidering: () => void
  onAvbryt: () => void
}) => {
  const {
    behandling,
    brevutfallOgEtterbetaling,
    setBrevutfallOgEtterbetaling,
    setVisSkjema,
    resetBrevutfallvalidering,
    onAvbryt,
  } = props
  const [harEtterbetaling, setHarEtterbetaling] = useState<HarEtterbetaling>(
    brevutfallOgEtterbetaling.etterbetaling === undefined
      ? HarEtterbetaling.IKKE_VALGT
      : brevutfallOgEtterbetaling.etterbetaling
      ? HarEtterbetaling.JA
      : HarEtterbetaling.NEI
  )
  const [lagreBrevutfallResultat, lagreBrevutfallRequest, lagreBrevutfallReset] = useApiCall(lagreBrevutfallApi)
  const [valideringsfeil, setValideringsfeil] = useState<Array<string>>([])
  const dispatch = useAppDispatch()

  const submitBrevutfall = () => {
    lagreBrevutfallReset()
    setValideringsfeil([])

    const valideringsfeil = valider()
    if (valideringsfeil.length > 0) {
      setValideringsfeil(valideringsfeil)
      return
    }

    lagreBrevutfallRequest(
      { behandlingId: behandling.id, brevutfall: brevutfallOgEtterbetaling },
      (brevutfall: BrevutfallOgEtterbetaling) => {
        resetBrevutfallvalidering()
        setBrevutfallOgEtterbetaling(brevutfall)
        dispatch(updateBrevutfallOgEtterbetaling(brevutfall))
        setVisSkjema(false)
      }
    )
  }

  const valider = () => {
    const feilmeldinger = []
    if (brevutfallOgEtterbetaling.etterbetaling || harEtterbetaling === HarEtterbetaling.JA) {
      const fom = brevutfallOgEtterbetaling.etterbetaling?.datoFom
      const tom = brevutfallOgEtterbetaling.etterbetaling?.datoTom

      if (!fom || !tom) {
        feilmeldinger.push('Både fra- og til-måned for etterbetaling må fylles ut.')
        return feilmeldinger
      }

      const fra = startOfDay(new Date(fom))
      const til = startOfDay(new Date(tom))

      if (fra > til) {
        feilmeldinger.push('Fra-måned kan ikke være etter til-måned.')
        return feilmeldinger
      }

      const virkningstidspunkt = behandling.virkningstidspunkt?.dato
      if (virkningstidspunkt && fra < startOfDay(new Date(virkningstidspunkt))) {
        feilmeldinger.push('Fra-måned kan ikke være før virkningstidspunkt.')
      }

      // Til og med kan ikke settes lenger frem enn inneværende måned. Inneværende måned vil måtte etterbetales
      // dersom utbetaling allerede er kjørt.
      const sisteDagIMnd = startOfDay(lastDayOfMonth(new Date()))
      if (til > sisteDagIMnd) {
        feilmeldinger.push('Til-måned kan ikke være etter inneværende måned.')
      }
    }
    if (harEtterbetaling === undefined) {
      feilmeldinger.push('Det må angis om det er etterbetaling eller ikke i saken.')
    }
    if (behandling.sakType == SakType.BARNEPENSJON && !brevutfallOgEtterbetaling.brevutfall?.aldersgruppe) {
      feilmeldinger.push('Over eller under 18 år må angis i barnepensjonssaker.')
    }
    return feilmeldinger
  }

  return (
    <VStack gap="8">
      <VStack gap="4">
        <RadioGroup
          legend={
            <HelpTextWrapper>
              Skal det etterbetales?
              <HelpText strategy="fixed">
                Velg ja hvis ytelsen er innvilget tilbake i tid og det blir utbetalt mer enn ett månedsbeløp. Da skal du
                registrere perioden fra innvilgelsesmåned til og med måneden som er klar for utbetaling. Vedlegg om
                etterbetaling skal da bli med i brevet.
              </HelpText>
            </HelpTextWrapper>
          }
          className="radioGroup"
          value={harEtterbetaling}
          onChange={(event) => {
            const svar = event as HarEtterbetaling
            setHarEtterbetaling(svar)
            if (svar === HarEtterbetaling.NEI) {
              setBrevutfallOgEtterbetaling({ ...brevutfallOgEtterbetaling, etterbetaling: undefined })
            }
          }}
        >
          <Radio size="small" value={HarEtterbetaling.JA}>
            Ja
          </Radio>
          <Radio size="small" value={HarEtterbetaling.NEI}>
            Nei
          </Radio>
        </RadioGroup>

        {harEtterbetaling == HarEtterbetaling.JA && (
          <HStack gap="4">
            <MaanedVelger
              fromDate={new Date(behandling.virkningstidspunkt?.dato ?? new Date())}
              toDate={new Date()}
              value={
                brevutfallOgEtterbetaling.etterbetaling?.datoFom
                  ? new Date(brevutfallOgEtterbetaling.etterbetaling?.datoFom)
                  : undefined
              }
              onChange={(date) =>
                setBrevutfallOgEtterbetaling({
                  ...brevutfallOgEtterbetaling,
                  etterbetaling: {
                    ...brevutfallOgEtterbetaling.etterbetaling,
                    datoFom: date ? formatISO(startOfMonth(date), { representation: 'date' }) : undefined,
                  },
                })
              }
              label="Fra og med"
            />
            <MaanedVelger
              fromDate={new Date(behandling.virkningstidspunkt?.dato ?? new Date())}
              toDate={add(new Date(), { months: 1 })}
              value={
                brevutfallOgEtterbetaling.etterbetaling?.datoTom
                  ? new Date(brevutfallOgEtterbetaling.etterbetaling?.datoTom)
                  : undefined
              }
              onChange={(date) =>
                setBrevutfallOgEtterbetaling({
                  ...brevutfallOgEtterbetaling,
                  etterbetaling: {
                    ...brevutfallOgEtterbetaling.etterbetaling,
                    datoTom: date ? formatISO(lastDayOfMonth(date), { representation: 'date' }) : undefined,
                  },
                })
              }
              label="Til og med"
            />
          </HStack>
        )}
      </VStack>

      {behandling.sakType == SakType.BARNEPENSJON && (
        <VStack gap="4">
          <RadioGroup
            legend={
              <HelpTextWrapper>
                Gjelder brevet under eller over 18 år?
                <HelpText strategy="fixed">
                  Velg her gjeldende alternativ for barnet, slik at riktig informasjon kommer med i vedlegg 2. For barn
                  under 18 år skal det stå &quot;Informasjon til deg som handler på vegne av barnet&quot;, mens for barn
                  over 18 år skal det stå &quot;Informasjon til deg som mottar barnepensjon&quot;.
                </HelpText>
              </HelpTextWrapper>
            }
            className="radioGroup"
            value={
              brevutfallOgEtterbetaling.brevutfall.aldersgruppe === undefined
                ? Aldersgruppe.IKKE_VALGT
                : brevutfallOgEtterbetaling.brevutfall.aldersgruppe
            }
            onChange={(e) =>
              setBrevutfallOgEtterbetaling({
                ...brevutfallOgEtterbetaling,
                brevutfall: { ...brevutfallOgEtterbetaling.brevutfall, aldersgruppe: e },
              })
            }
          >
            <Radio size="small" value={Aldersgruppe.UNDER_18}>
              Under 18 år
            </Radio>
            <Radio size="small" value={Aldersgruppe.OVER_18}>
              Over 18 år
            </Radio>
          </RadioGroup>
        </VStack>
      )}

      <HStack gap="4">
        <Button size="small" type="submit" loading={isPending(lagreBrevutfallResultat)} onClick={submitBrevutfall}>
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

      {valideringsfeil?.length > 0 && (
        <ErrorSummary heading="Feil ved lagring av brevutfall">
          {valideringsfeil.map((feilmelding, index) => (
            <ErrorSummary.Item key={`${index}`} href="#brevutfall">
              {feilmelding}
            </ErrorSummary.Item>
          ))}
        </ErrorSummary>
      )}

      {isFailure(lagreBrevutfallResultat) && <Alert variant="error">{lagreBrevutfallResultat.error.detail}</Alert>}
    </VStack>
  )
}

const HelpTextWrapper = styled.div`
  display: flex;
  gap: 0.5em;
`
