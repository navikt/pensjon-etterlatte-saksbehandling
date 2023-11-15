import { IBoddEllerArbeidetUtlandet } from '~shared/types/IDetaljertBehandling'
import { BodyShort, Heading, Label, VStack } from '@navikt/ds-react'
import { JaNei, JaNeiRec } from '~shared/types/ISvar'
import styled from 'styled-components'

const BoddEllerArbeidetIUtlandetVisning = (props: { boddEllerArbeidetUtlandet: IBoddEllerArbeidetUtlandet | null }) => {
  const { boddEllerArbeidetUtlandet } = props
  return (
    <>
      {boddEllerArbeidetUtlandet && boddEllerArbeidetUtlandet.boddEllerArbeidetUtlandet ? (
        <>
          <SpoersmaalSvarWrapper>
            <Label as="p" size="small">
              {JaNeiRec[boddEllerArbeidetUtlandet.boddEllerArbeidetUtlandet ? JaNei.JA : JaNei.NEI]}
            </Label>
          </SpoersmaalSvarWrapper>
          <SpoersmaalSvarWrapper>
            <Heading level="3" size="xsmall">
              Vurdering av utlandsopphold
            </Heading>
            <div>
              <BodyShort spacing>Avdøde har bodd/arbeidet i utlandet (Ikke EØS/avtaleland)</BodyShort>
              <Label as="p" size="small">
                {JaNeiRec[boddEllerArbeidetUtlandet.boddArbeidetIkkeEosEllerAvtaleland ? JaNei.JA : JaNei.NEI]}
              </Label>
            </div>
            <div>
              <BodyShort spacing>Avdøde har bodd/arbeidet i utlandet (EØS/nordisk konvensjon)</BodyShort>
              <Label as="p" size="small">
                {JaNeiRec[boddEllerArbeidetUtlandet.boddArbeidetEosNordiskKonvensjon ? JaNei.JA : JaNei.NEI]}
              </Label>
            </div>
            <div>
              <BodyShort spacing>Avdøde har bodd/arbeidet i utlandet (Avtaleland)</BodyShort>
              <Label as="p" size="small">
                {JaNeiRec[boddEllerArbeidetUtlandet.boddArbeidetAvtaleland ? JaNei.JA : JaNei.NEI]}
              </Label>
            </div>
          </SpoersmaalSvarWrapper>
          <SpoersmaalSvarWrapper>
            <Heading level="3" size="xsmall">
              Huk av hvis aktuelt
            </Heading>
            <div>
              <BodyShort spacing>Vurdere avdødes trygdeavtale</BodyShort>
              <Label as="p" size="small">
                {JaNeiRec[boddEllerArbeidetUtlandet.vurdereAvoededsTrygdeavtale ? JaNei.JA : JaNei.NEI]}
              </Label>
            </div>
            <div>
              <BodyShort spacing>Det skal sendes kravpakke</BodyShort>
              <Label as="p" size="small">
                {JaNeiRec[boddEllerArbeidetUtlandet.skalSendeKravpakke ? JaNei.JA : JaNei.NEI]}
              </Label>
            </div>
          </SpoersmaalSvarWrapper>
        </>
      ) : (
        <Label as="p" size="small" style={{ marginBottom: '32px' }}>
          Nei
        </Label>
      )}
    </>
  )
}

export default BoddEllerArbeidetIUtlandetVisning

const SpoersmaalSvarWrapper = styled(VStack).attrs({ gap: '6' })`
  margin-bottom: 3em;
`
