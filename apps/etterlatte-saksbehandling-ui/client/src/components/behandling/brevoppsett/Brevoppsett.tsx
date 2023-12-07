import styled from 'styled-components'
import {
  Alert,
  BodyLong,
  BodyShort,
  Button,
  ErrorSummary,
  Heading,
  HelpText,
  HStack,
  Label,
  Radio,
  RadioGroup,
  VStack,
} from '@navikt/ds-react'
import React, { useState } from 'react'
import MaanedVelger from '~components/behandling/beregningsgrunnlag/MaanedVelger'
import { SakType } from '~shared/types/sak'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreBrevoppsett } from '~shared/api/behandling'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { isFailure, isPending } from '~shared/api/apiUtils'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { formaterDato } from '~utils/formattering'

export interface Brevoppsett {
  etterbetaling?: Etterbetaling | null
  aldersgruppe?: Aldersgruppe | null
}

export enum Aldersgruppe {
  OVER_18 = 'OVER_18',
  UNDER_18 = 'UNDER_18',
}

export interface Etterbetaling {
  fom?: Date | null
  tom?: Date | null
}

function aldersgruppeToString(aldersgruppe: Aldersgruppe) {
  switch (aldersgruppe) {
    case Aldersgruppe.OVER_18:
      return 'Over 18 år'
    case Aldersgruppe.UNDER_18:
      return 'Under 18 år'
  }
}

export const Brevoppsett = (props: { behandling: IDetaljertBehandling }) => {
  const behandling = props.behandling
  const redigerbar = behandlingErRedigerbar(behandling.status)
  const [brevoppsett, setBrevoppsett] = useState<Brevoppsett>({ etterbetaling: undefined, aldersgruppe: undefined })
  const [harEtterbetaling, setHarEtterbetaling] = useState<boolean | undefined>(undefined)
  const [lagreBrevoppsettStatus, lagreBrevoppsettRequest, reset] = useApiCall(lagreBrevoppsett)
  const [valideringsfeil, setValideringsfeil] = useState<Array<string>>([])
  const [visSkjema, setVisSkjema] = useState(redigerbar)

  console.log(brevoppsett)

  const lagreBrevoppsettApi = () => {
    reset()
    setValideringsfeil([])

    const valideringsfeil = valider()
    if (valideringsfeil.length > 0) {
      setValideringsfeil(valideringsfeil)
      return
    }

    setVisSkjema(false)

    lagreBrevoppsettRequest({ behandlingId: behandling.id, brevoppsett: brevoppsett })
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

  console.log(valideringsfeil)

  return (
    <BrevoppsettContent>
      <Heading size="medium" spacing>
        Valg av utfall i brev
      </Heading>
      <BodyLong spacing>
        Her velger du hvilket utfall som gjelder slik at det blir riktig informasjon i brevet.
      </BodyLong>

      <VStack gap="8">
        {visSkjema ? (
          <VStack gap="8">
            <VStack gap="4">
              <RadioGroup
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
                className="radioGroup"
                value={harEtterbetaling}
                onChange={(event) => {
                  const svar = event as boolean
                  setHarEtterbetaling(svar)
                  if (!svar) {
                    setBrevoppsett({ ...brevoppsett, etterbetaling: null })
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
                        Velg her gjeldende alternativ for barnet, slik at riktig informasjon kommer med i vedlegg 2. For
                        barn under 18 år skal det stå &quot;Informasjon til deg som handler på vegne av barnet&quot;,
                        mens for barn over 18 år skal det stå &quot;Informasjon til deg som mottar barnepensjon&quot;.
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
              <Button
                size="small"
                type="submit"
                loading={isPending(lagreBrevoppsettStatus)}
                onClick={lagreBrevoppsettApi}
              >
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
          </VStack>
        ) : (
          <VStack gap="8">
            <VStack gap="2">
              <Label>Skal det etterbetales?</Label>
              {brevoppsett.etterbetaling ? (
                <HStack gap="8">
                  <HStack gap="4">
                    <Label>Fra og med</Label>
                    <BodyShort>{formaterDato(brevoppsett.etterbetaling.fom!!)}</BodyShort>
                  </HStack>
                  <HStack gap="4">
                    <Label>Til og med</Label>
                    <BodyShort>{formaterDato(brevoppsett.etterbetaling.fom!!)}</BodyShort>
                  </HStack>
                </HStack>
              ) : (
                <BodyShort>{brevoppsett.etterbetaling ? 'Ja' : 'Nei'}</BodyShort>
              )}
            </VStack>
            <VStack gap="2">
              <Label>Gjelder brevet under eller over 18 år?</Label>
              <BodyShort>{brevoppsett.aldersgruppe ? aldersgruppeToString(brevoppsett.aldersgruppe) : ''}</BodyShort>
            </VStack>
            {redigerbar}
            <HStack>
              <Button size="small" onClick={() => setVisSkjema(true)}>
                Rediger
              </Button>
            </HStack>
          </VStack>
        )}

        {isFailure(lagreBrevoppsettStatus) && <Alert variant="error">{lagreBrevoppsettStatus.error.detail}</Alert>}
      </VStack>
    </BrevoppsettContent>
  )
}

const HelpTextWrapper = styled.div`
  display: flex;
  gap: 0.5em;
`

const BrevoppsettContent = styled.div`
  margin-bottom: 2em;
`
