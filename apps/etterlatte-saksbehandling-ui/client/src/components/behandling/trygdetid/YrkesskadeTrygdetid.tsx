import { FlexHeader } from '~components/behandling/soeknadsoversikt/familieforhold/styled'
import { BodyShort, Checkbox, Heading } from '@navikt/ds-react'
import styled from 'styled-components'
import { ITrygdetid } from '~shared/api/trygdetid'
import { useEffect, useState } from 'react'

interface Props {
  redigerbar: boolean
  trygdetid: ITrygdetid
  oppdaterYrkesskade: (yrkesskade: boolean) => void
}

export const YrkesskadeTrygdetid = ({ redigerbar, trygdetid, oppdaterYrkesskade }: Props) => {
  const [yrkesskade, setYrkesskade] = useState<boolean | undefined>(undefined)

  useEffect(() => {
    setYrkesskade(trygdetid.beregnetTrygdetid?.resultat.yrkesskade ?? undefined)
  }, [])

  return (
    <Yrkesskade>
      <FlexHeader>
        <Heading size="small" level="4">
          {redigerbar ? (
            <>
              Kryss av her hvis dødsfallet skyldtes en godkjent yrkesskade/sykdom. Dette gir automatisk 40 års
              trygdetid.
            </>
          ) : (
            <>Hvis dødsfallet skyldtes en godkjent yrkesskade/sykdom gir dette automatisk 40 års trygdetid.</>
          )}
        </Heading>
      </FlexHeader>
      {!redigerbar && (
        <>
          <YrkesskadeVerdi>{yrkesskade ?? false ? <>Yrkesskade</> : <>Ikke yrkesskade</>}</YrkesskadeVerdi>
        </>
      )}
      {redigerbar && (
        <YrkesskadeFelt
          checked={yrkesskade}
          onChange={() => {
            setYrkesskade(!yrkesskade)
            oppdaterYrkesskade(yrkesskade ?? false)
          }}
        >
          Godkjent yrkesskade/sykdom
        </YrkesskadeFelt>
      )}
    </Yrkesskade>
  )
}

const Yrkesskade = styled.div`
  padding: 2em 0 0 0;
`

const YrkesskadeVerdi = styled(BodyShort)`
  padding: 1em 0 0 0;
`

const YrkesskadeFelt = styled(Checkbox)`
  padding: 1em 0 0 0;
  width: 10em;
`
