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
        <Heading size="small" level="3">
          Beregnet trygdetid
        </Heading>
      </FlexHeader>
      <BeregnetFaktiskTrygdetid beregnetTrygdetid={beregnetTrygdetid} />
      <BeregnetFremtidigTrygdetid beregnetTrygdetid={beregnetTrygdetid} />
      <BeregnetSamletTrygdetid beregnetTrygdetid={beregnetTrygdetid} />
    </TrygdetidBeregnet>
  )
}

export const formaterBeregnetTrygdetid = (periode?: string) => {
  if (!periode) {
    return ''
  }

  // Legger til 0 år eksplisitt dersom perioden er under ett år
  const periodeMedAntallAar = periode.includes('Y') ? periode : 'P0Y' + periode.slice(1)

  // formatet på periode matcher /\d+Y(\d+M)?/, så en split på Y|M vil gi en array med
  // 1. år-strengen og 2. tom streng eller måned-strengen
  const [aar, maaneder] = periodeMedAntallAar.slice(1).split(/Y|M/)
  return `${aar} år${maaneder ? ` ${maaneder} måneder` : ''}`
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
