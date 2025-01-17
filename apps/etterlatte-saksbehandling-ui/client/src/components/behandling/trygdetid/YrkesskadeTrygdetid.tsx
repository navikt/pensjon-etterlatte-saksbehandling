import { Checkbox, CheckboxGroup } from '@navikt/ds-react'
import { ITrygdetid } from '~shared/api/trygdetid'
import { useState } from 'react'

interface Props {
  redigerbar: boolean
  trygdetid: ITrygdetid
  oppdaterYrkesskade: (yrkesskade: boolean) => void
}

export const YrkesskadeTrygdetid = ({ redigerbar, trygdetid, oppdaterYrkesskade }: Props) => {
  const [yrkesskade, setYrkesskade] = useState<boolean | undefined>(
    trygdetid.beregnetTrygdetid?.resultat.yrkesskade ?? undefined
  )

  return (
    <CheckboxGroup
      legend="Skyldtes dødsfallet en godkjent yrkesskade/sykdom"
      description={
        redigerbar && (
          <>
            Kryss av her hvis dødsfallet skyldtes en godkjent yrkesskade/sykdom. Dette gir automatisk 40 års trygdetid.
            <br />
            Velg Nasjonal beregning under beregningsgrunnlag og &#34;Trygdetid i beregning&#34; for å støtte riktig
            brevutfall for yrkesskade.
          </>
        )
      }
      readOnly={!redigerbar}
    >
      <Checkbox
        checked={yrkesskade}
        onChange={() => {
          const oppdatertYrkesskade = !(yrkesskade ?? false)

          setYrkesskade(oppdatertYrkesskade)
          oppdaterYrkesskade(oppdatertYrkesskade)
        }}
      >
        Godkjent yrkesskade/sykdom
      </Checkbox>
    </CheckboxGroup>
  )
}
