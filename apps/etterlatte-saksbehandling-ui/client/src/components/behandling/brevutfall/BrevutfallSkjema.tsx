import styled from 'styled-components'
import { Alert, Button, ErrorSummary, HelpText, HStack, Radio, RadioGroup, VStack } from '@navikt/ds-react'
import React, { useState } from 'react'
import MaanedVelger from '~components/behandling/beregningsgrunnlag/MaanedVelger'
import { SakType } from '~shared/types/sak'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreBrevutfallApi } from '~shared/api/behandling'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { isFailure, isPending } from '~shared/api/apiUtils'
import { Aldersgruppe, Brevutfall } from '~components/behandling/brevutfall/Brevutfall'

enum HarEtterbetaling {
  JA = 'JA',
  NEI = 'NEI',
  IKKE_VALGT = 'IKKE_VALGT',
}

export const BrevutfallSkjema = (props: {
  behandling: IDetaljertBehandling
  brevutfall: Brevutfall
  setBrevutfall: (brevutfall: Brevutfall) => void
  setVisSkjema: (visSkjema: boolean) => void
  onAvbryt: () => void
}) => {
  const { behandling, brevutfall, setBrevutfall, setVisSkjema, onAvbryt } = props
  const [harEtterbetaling, setHarEtterbetaling] = useState<HarEtterbetaling>(
    brevutfall.etterbetaling === undefined
      ? HarEtterbetaling.IKKE_VALGT
      : brevutfall.etterbetaling
      ? HarEtterbetaling.JA
      : HarEtterbetaling.NEI
  )
  const [lagreBrevutfallResultat, lagreBrevutfallRequest, lagreBrevutfallReset] = useApiCall(lagreBrevutfallApi)
  const [valideringsfeil, setValideringsfeil] = useState<Array<string>>([])

  const submitBrevutfall = () => {
    lagreBrevutfallReset()
    setValideringsfeil([])

    const valideringsfeil = valider()
    if (valideringsfeil.length > 0) {
      setValideringsfeil(valideringsfeil)
      return
    }

    lagreBrevutfallRequest({ behandlingId: behandling.id, brevutfall: brevutfall }, (brevutfall: Brevutfall) => {
      setBrevutfall(brevutfall)
      setVisSkjema(false)
    })
  }

  const valider = () => {
    const feilmeldinger = []
    if (brevutfall.etterbetaling || harEtterbetaling === HarEtterbetaling.JA) {
      const fom = brevutfall.etterbetaling?.datoFom
      const tom = brevutfall.etterbetaling?.datoTom
      if (!fom || !tom) {
        feilmeldinger.push('Både fra- og til-måned for etterbetaling må fylles ut.')
        return feilmeldinger
      }

      const fra = new Date(fom)
      const til = new Date(tom)
      if (fra > til) {
        feilmeldinger.push('Fra-måned kan ikke være etter til-måned.')
        return feilmeldinger
      }

      const virkningstidspunkt = behandling.virkningstidspunkt?.dato
      if (virkningstidspunkt && fra < new Date(virkningstidspunkt)) {
        feilmeldinger.push('Fra-måned kan ikke være før virkningstidspunkt.')
      }

      if (til.getMonth() > new Date().getMonth()) {
        feilmeldinger.push('Til-måned kan ikke være i framtida.')
      }
    }
    if (harEtterbetaling === undefined) {
      feilmeldinger.push('Det må angis om det er etterbetaling eller ikke i saken.')
    }
    if (behandling.sakType == SakType.BARNEPENSJON && !brevutfall.aldersgruppe) {
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
              setBrevutfall({ ...brevutfall, etterbetaling: undefined })
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
              value={brevutfall.etterbetaling?.datoFom ? new Date(brevutfall.etterbetaling?.datoFom) : undefined}
              onChange={(e) =>
                setBrevutfall({
                  ...brevutfall,
                  etterbetaling: { ...brevutfall.etterbetaling, datoFom: e ? e.toISOString() : undefined },
                })
              }
              label="Fra og med"
            />
            <MaanedVelger
              value={brevutfall.etterbetaling?.datoTom ? new Date(brevutfall.etterbetaling?.datoTom) : undefined}
              onChange={(e) =>
                setBrevutfall({
                  ...brevutfall,
                  etterbetaling: { ...brevutfall.etterbetaling, datoTom: e ? e.toISOString() : undefined },
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
            value={brevutfall.aldersgruppe === undefined ? Aldersgruppe.IKKE_VALGT : brevutfall.aldersgruppe}
            onChange={(e) => setBrevutfall({ ...brevutfall, aldersgruppe: e })}
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
