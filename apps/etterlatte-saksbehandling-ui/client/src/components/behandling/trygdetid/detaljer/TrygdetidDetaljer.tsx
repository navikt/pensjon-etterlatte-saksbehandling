import React from 'react'
import { IDetaljertBeregnetTrygdetidResultat } from '~shared/api/trygdetid'
import { BeregnetFaktiskTrygdetid } from '~components/behandling/trygdetid/detaljer/BeregnetFaktiskTrygdetid'
import { BeregnetFremtidigTrygdetid } from '~components/behandling/trygdetid/detaljer/BeregnetFremtidigTrygdetid'
import { FlexHeader, IconWrapper } from '~components/behandling/soeknadsoversikt/familieforhold/styled'
import { CalendarIcon } from '@navikt/aksel-icons'
import { IconSize } from '~shared/types/Icon'
import { Heading } from '@navikt/ds-react'
import styled from 'styled-components'
import { BeregnetSamletTrygdetid } from '~components/behandling/trygdetid/detaljer/BeregnetSamletTrygdetid'

type Props = {
  beregnetTrygdetid: IDetaljertBeregnetTrygdetidResultat
}

export const TrygdetidDetaljer: React.FC<Props> = ({ beregnetTrygdetid }) => {
  return (
    <TrygdetidBeregnet>
      <FlexHeader>
        <IconWrapper>
          <CalendarIcon fontSize={IconSize.DEFAULT} />
        </IconWrapper>
        <Heading size={'small'} level={'3'}>
          Beregnet trygdetid
        </Heading>
      </FlexHeader>
      <BeregnetFaktiskTrygdetid beregnetTrygdetid={beregnetTrygdetid} />
      <BeregnetFremtidigTrygdetid beregnetTrygdetid={beregnetTrygdetid} />
      <BeregnetSamletTrygdetid beregnetTrygdetid={beregnetTrygdetid} />
    </TrygdetidBeregnet>
  )
}

// formatet på periode er /P[aar:\d+Y][maaneder:\d+M]/, der aar og maaneder ikke er med
// hvis de er 0. Denne regex'en legger tallene før Y i første match-gruppe og
// tallene før M i andre match-gruppe. Siden matchene er optional (?) er de undefined
// hvis de ikke er med
const PERIODE_MATCHER = /P(?:(\d+)Y)?(?:(\d+)M)?/i // casing skal være uppercase, men legger på 'i' i tilfelle

export const formaterBeregnetTrygdetid = (periode?: string) => {
  const periodeMatch = periode?.match(PERIODE_MATCHER)
  if (!periodeMatch) {
    return ''
  }

  const [, aar, maaneder] = periodeMatch
  return `${aar || '0'} år${maaneder ? ` ${maaneder} måneder` : ''}`
}

export const TrygdetidTabell = styled.div`
  margin-top: 2em;

  thead {
    th {
      width: 200px;
    }

    th:first-child {
      width: 400px;
    }
  }
`
const TrygdetidBeregnet = styled.div`
  padding: 2em 0 4em 0;
`
