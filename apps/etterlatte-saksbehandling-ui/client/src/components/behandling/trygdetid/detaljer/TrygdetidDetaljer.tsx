import React from 'react'
import { IBeregnetTrygdetidAarMaaneder, IDetaljertBeregnetTrygdetid } from '~shared/api/trygdetid'
import { BeregnetFaktiskTrygdetid } from '~components/behandling/trygdetid/detaljer/BeregnetFaktiskTrygdetid'
import { BeregnetFremtidigTrygdetid } from '~components/behandling/trygdetid/detaljer/BeregnetFremtidigTrygdetid'
import { FlexHeader, IconWrapper } from '~components/behandling/soeknadsoversikt/familieforhold/styled'
import { CalendarIcon } from '@navikt/aksel-icons'
import { IconSize } from '~shared/types/Icon'
import { Heading } from '@navikt/ds-react'
import styled from 'styled-components'
import { BeregnetSamletTrygdetid } from '~components/behandling/trygdetid/detaljer/BeregnetSamletTrygdetid'

type Props = {
  beregnetTrygdetid: IDetaljertBeregnetTrygdetid
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

export const formaterBeregnetTrygdetid = (beregnetTT: IBeregnetTrygdetidAarMaaneder) => {
  return `${beregnetTT.aar} år${beregnetTT.maaneder ? ` ${beregnetTT.maaneder} måneder` : ''}`
}

export const TrygdetidTabell = styled.div`
  margin-top: 2em;
`
const TrygdetidBeregnet = styled.div`
  padding: 2em 0 4em 0;
`
