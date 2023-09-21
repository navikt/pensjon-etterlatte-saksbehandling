import { IBoddEllerArbeidetUtlandet } from '~shared/types/IDetaljertBehandling'
import { BodyShort, Label } from '@navikt/ds-react'
import { JaNei, JaNeiRec } from '~shared/types/ISvar'

const BoddEllerArbeidetIUtlandetVisning = (props: { boddEllerArbeidetUtlandet: IBoddEllerArbeidetUtlandet | null }) => {
  const { boddEllerArbeidetUtlandet } = props
  return (
    <>
      {boddEllerArbeidetUtlandet && boddEllerArbeidetUtlandet.boddEllerArbeidetUtlandet ? (
        <>
          <BodyShort spacing>Har avdøde bodd eller arbeidet i utlandet?</BodyShort>
          <Label as="p" size="small">
            {JaNeiRec[boddEllerArbeidetUtlandet.boddEllerArbeidetUtlandet ? JaNei.JA : JaNei.NEI]}
          </Label>
          <BodyShort spacing>Avdøde har bodd/arbeidet i utlandet (Ikke EØS/avtaleland)</BodyShort>
          <Label as="p" size="small">
            {JaNeiRec[boddEllerArbeidetUtlandet.boddArbeidetIkkeEosEllerAvtaleland ? JaNei.JA : JaNei.NEI]}
          </Label>
          <BodyShort spacing>Avdøde har bodd/arbeidet i utlandet (EØS/nordisk konvensjon)</BodyShort>
          <Label as="p" size="small">
            {JaNeiRec[boddEllerArbeidetUtlandet.boddArbeidetEosNordiskKonvensjon ? JaNei.JA : JaNei.NEI]}
          </Label>
          <BodyShort spacing>Avdøde har bodd/arbeidet i utlandet (Avtaleland)</BodyShort>
          <Label as="p" size="small">
            {JaNeiRec[boddEllerArbeidetUtlandet.boddArbeidetAvtaleland ? JaNei.JA : JaNei.NEI]}
          </Label>
          <BodyShort spacing>Vurdere avdødes trygdeavtale</BodyShort>
          <Label as="p" size="small">
            {JaNeiRec[boddEllerArbeidetUtlandet.vurdereAvoededsTrygdeavtale ? JaNei.JA : JaNei.NEI]}
          </Label>
          <BodyShort spacing>Norge er behandlende land</BodyShort>
          <Label as="p" size="small">
            {JaNeiRec[boddEllerArbeidetUtlandet.norgeErBehandlendeland ? JaNei.JA : JaNei.NEI]}
          </Label>
          <BodyShort spacing>Det skal sendes kravpakke</BodyShort>
          <Label as="p" size="small">
            {JaNeiRec[boddEllerArbeidetUtlandet.skalSendeKravpakke ? JaNei.JA : JaNei.NEI]}
          </Label>
        </>
      ) : (
        <Label as="p" size="small" style={{ marginBottom: '32px' }}>
          Ikke vurdert
        </Label>
      )}
    </>
  )
}

export default BoddEllerArbeidetIUtlandetVisning
