import { IBoddEllerArbeidetUtlandet } from '~shared/types/IDetaljertBehandling'
import { BodyShort, Heading, Label, VStack } from '@navikt/ds-react'
import { JaNei, JaNeiRec } from '~shared/types/ISvar'

const BoddEllerArbeidetIUtlandetVisning = (props: { boddEllerArbeidetUtlandet: IBoddEllerArbeidetUtlandet | null }) => {
  const { boddEllerArbeidetUtlandet } = props
  return (
    <>
      {boddEllerArbeidetUtlandet && boddEllerArbeidetUtlandet.boddEllerArbeidetUtlandet ? (
        <>
          <VStack gap="space-6" marginBlock="space-0 space-12">
            <Label as="p" size="small">
              {JaNeiRec[boddEllerArbeidetUtlandet.boddEllerArbeidetUtlandet ? JaNei.JA : JaNei.NEI]}
            </Label>
          </VStack>

          <VStack gap="space-6" marginBlock="space-0 space-12">
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
          </VStack>

          <VStack gap="space-6" marginBlock="space-0 space-12">
            <Heading level="3" size="xsmall">
              Huk av hvis aktuelt
            </Heading>
            <div>
              <BodyShort spacing>Vurdere avdødes trygdeavtale</BodyShort>
              <Label as="p" size="small">
                {JaNeiRec[boddEllerArbeidetUtlandet.vurdereAvdoedesTrygdeavtale ? JaNei.JA : JaNei.NEI]}
              </Label>
            </div>
            <div>
              <BodyShort spacing>Det skal sendes kravpakke</BodyShort>
              <Label as="p" size="small">
                {JaNeiRec[boddEllerArbeidetUtlandet.skalSendeKravpakke ? JaNei.JA : JaNei.NEI]}
              </Label>
            </div>
          </VStack>
        </>
      ) : (
        <VStack gap="space-6" marginBlock="space-0 space-6">
          <BodyShort size="small">Nei</BodyShort>

          <BodyShort size="small">Ved avslag på helnasjonal sak vil ikke trygdetidsbildet vises</BodyShort>
        </VStack>
      )}
    </>
  )
}

export default BoddEllerArbeidetIUtlandetVisning
