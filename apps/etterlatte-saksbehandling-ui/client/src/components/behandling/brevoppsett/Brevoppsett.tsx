import styled from 'styled-components'
import { BodyLong, Heading, HelpText, HStack, Radio, RadioGroup, VStack } from '@navikt/ds-react'
import React, { useState } from 'react'
import MaanedVelger from '~components/behandling/beregningsgrunnlag/MaanedVelger'

interface Brevoppsett {
  etterbetaling: Etterbetaling | null
}

interface Etterbetaling {
  fom: Date | null
  tom: Date | null
}

export const Brevoppsett = () => {
  const [brevoppsett, setBrevoppsett] = useState<Brevoppsett>({ etterbetaling: { fom: null, tom: null } })
  const [harEtterbetaling, setHarEtterbetaling] = useState(false)

  return (
    <>
      <Heading size="medium" spacing>
        Valg av utfall i brev
      </Heading>
      <BodyLong spacing>
        Her velger du hvilket utfall som gjelder slik at det blir riktig informasjon i brevet.
      </BodyLong>

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
          onChange={(event) => setHarEtterbetaling(event as boolean)}
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
                setBrevoppsett({ ...brevoppsett, etterbetaling: { ...brevoppsett.etterbetaling, fom: e } })
              }
              label="Til og med"
            />
          </HStack>
        )}
      </VStack>
    </>
  )
}

const HelpTextWrapper = styled.div`
  display: flex;
  gap: 0.5em;
`
