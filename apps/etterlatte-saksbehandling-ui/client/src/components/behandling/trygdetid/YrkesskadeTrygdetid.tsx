import { BodyShort, Checkbox, CheckboxGroup, Heading } from '@navikt/ds-react'
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

  return !redigerbar ? (
    <CheckboxGroup
      legend="Skyldtes dødsfallet en godkjent yrkesskade/sykdom"
      description={
        redigerbar &&
        'Kryss av her hvis dødsfallet skyldtes en godkjent yrkesskade/sykdom. Dette gir automatisk 40 års trygdetid'
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
  ) : (
    <div>
      <Heading size="small" level="4" spacing>
        Hvis dødsfallet skyldtes en godkjent yrkesskade/sykdom gir dette automatisk 40 års trygdetid.
      </Heading>
      <BodyShort>{yrkesskade ? 'Yrkesskade' : 'Ikke yrkesskade'}</BodyShort>
    </div>
  )
}
