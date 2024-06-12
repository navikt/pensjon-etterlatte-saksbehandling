import { BodyShort, Checkbox, Heading, HStack, VStack } from '@navikt/ds-react'
import styled from 'styled-components'
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
    <VStack gap="4">
      <HStack gap="2">
        <Heading size="small" level="4">
          {redigerbar
            ? 'Kryss av her hvis dødsfallet skyldtes en godkjent yrkesskade/sykdom. Dette gir automatisk 40 års trygdetid.'
            : 'Hvis dødsfallet skyldtes en godkjent yrkesskade/sykdom gir dette automatisk 40 års trygdetid.'}
        </Heading>
      </HStack>
      {!redigerbar && <YrkesskadeVerdi>{yrkesskade ? 'Yrkesskade' : 'Ikke yrkesskade'}</YrkesskadeVerdi>}
      {redigerbar && (
        <YrkesskadeFelt
          checked={yrkesskade}
          onChange={() => {
            const oppdatertYrkesskade = !(yrkesskade ?? false)

            setYrkesskade(oppdatertYrkesskade)
            oppdaterYrkesskade(oppdatertYrkesskade)
          }}
        >
          Godkjent yrkesskade/sykdom
        </YrkesskadeFelt>
      )}
    </VStack>
  )
}

const YrkesskadeVerdi = styled(BodyShort)`
  padding: 1em 0 0 0;
`

const YrkesskadeFelt = styled(Checkbox)`
  padding: 1em 0 0 0;
`
