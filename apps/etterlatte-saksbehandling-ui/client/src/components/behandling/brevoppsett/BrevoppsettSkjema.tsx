import styled from 'styled-components'
import { Alert, Button, ErrorSummary, HelpText, HStack, Radio, RadioGroup, VStack } from '@navikt/ds-react'
import React, { useState } from 'react'
import MaanedVelger from '~components/behandling/beregningsgrunnlag/MaanedVelger'
import { SakType } from '~shared/types/sak'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreBrevoppsett } from '~shared/api/behandling'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { isFailure, isPending } from '~shared/api/apiUtils'
import { Aldersgruppe, Brevoppsett } from '~components/behandling/brevoppsett/Brevoppsett'

export const BrevoppsettSkjema = (props: {
  behandling: IDetaljertBehandling
  brevoppsett: Brevoppsett
  setBrevoppsett: (brevoppsett: Brevoppsett) => void
  setVisSkjema: (visSkjema: boolean) => void
}) => {
  const { behandling, brevoppsett, setBrevoppsett, setVisSkjema } = props
  const [harEtterbetaling, setHarEtterbetaling] = useState<boolean | undefined>(
    brevoppsett.etterbetaling ? true : undefined
  )
  const [lagreBrevoppsettResultat, lagreBrevoppsettRequest, lagreBrevoppsettReset] = useApiCall(lagreBrevoppsett)
  const [valideringsfeil, setValideringsfeil] = useState<Array<string>>([])

  const submitBrevoppsett = () => {
    lagreBrevoppsettReset()
    setValideringsfeil([])

    const valideringsfeil = valider()
    if (valideringsfeil.length > 0) {
      setValideringsfeil(valideringsfeil)
      return
    }

    lagreBrevoppsettRequest({ behandlingId: behandling.id, brevoppsett: brevoppsett }, (brevoppsett: Brevoppsett) => {
      setBrevoppsett(brevoppsett)
      setVisSkjema(false)
    })
  }

  const valider = () => {
    const feilmeldinger = []
    if (brevoppsett.etterbetaling || harEtterbetaling) {
      const fom = brevoppsett.etterbetaling?.fom
      const tom = brevoppsett.etterbetaling?.tom
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

      if (til > new Date()) {
        feilmeldinger.push('Til-måned kan ikke være i framtida.')
      }
    }
    if (harEtterbetaling === undefined) {
      feilmeldinger.push('Det må angis om det er etterbetaling eller ikke i saken.')
    }
    if (behandling.sakType == SakType.BARNEPENSJON && !brevoppsett.aldersgruppe) {
      feilmeldinger.push('Over eller under 18 år må angis i barnepensjonssaker.')
    }
    return feilmeldinger
  }

  const harValideringsfeil = () => valideringsfeil?.length > 0

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
            const svar = event as boolean
            setHarEtterbetaling(svar)
            if (!svar) {
              setBrevoppsett({ ...brevoppsett, etterbetaling: undefined })
            }
          }}
        >
          <Radio size="small" value={true}>
            Ja
          </Radio>
          <Radio size="small" value={false}>
            Nei
          </Radio>
        </RadioGroup>

        {harEtterbetaling && (
          <HStack gap="4">
            <MaanedVelger
              value={brevoppsett.etterbetaling?.fom ? new Date(brevoppsett.etterbetaling?.fom) : undefined}
              onChange={(e) =>
                setBrevoppsett({ ...brevoppsett, etterbetaling: { ...brevoppsett.etterbetaling, fom: e } })
              }
              label="Fra og med"
            />
            <MaanedVelger
              value={brevoppsett.etterbetaling?.tom ? new Date(brevoppsett.etterbetaling?.tom) : undefined}
              onChange={(e) =>
                setBrevoppsett({ ...brevoppsett, etterbetaling: { ...brevoppsett.etterbetaling, tom: e } })
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
            value={brevoppsett.aldersgruppe}
            onChange={(e) => setBrevoppsett({ ...brevoppsett, aldersgruppe: e })}
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
        <Button size="small" type="submit" loading={isPending(lagreBrevoppsettResultat)} onClick={submitBrevoppsett}>
          Lagre valg
        </Button>
        <Button variant="secondary" size="small" onClick={() => setVisSkjema(false)}>
          Avbryt
        </Button>
      </HStack>

      {harValideringsfeil() && (
        <ErrorSummary heading="Feil ved lagring av brevoppsett">
          {valideringsfeil.map((feilmelding, index) => (
            <ErrorSummary.Item key={`${index}${feilmelding}`} href={`#brevoppsett.${index}`}>
              {feilmelding}
            </ErrorSummary.Item>
          ))}
        </ErrorSummary>
      )}

      {isFailure(lagreBrevoppsettResultat) && <Alert variant="error">{lagreBrevoppsettResultat.error.detail}</Alert>}
    </VStack>
  )
}

const HelpTextWrapper = styled.div`
  display: flex;
  gap: 0.5em;
`
