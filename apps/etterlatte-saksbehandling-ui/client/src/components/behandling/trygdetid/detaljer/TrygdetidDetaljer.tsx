import React from 'react'
import { IDetaljertBeregnetTrygdetidResultat } from '~shared/api/trygdetid'
import { BeregnetFaktiskTrygdetid } from '~components/behandling/trygdetid/detaljer/BeregnetFaktiskTrygdetid'
import { BeregnetFremtidigTrygdetid } from '~components/behandling/trygdetid/detaljer/BeregnetFremtidigTrygdetid'
import { CalculatorIcon, ExclamationmarkTriangleIcon } from '@navikt/aksel-icons'
import { IconSize } from '~shared/types/Icon'
import { BodyShort, Heading, HStack, VStack } from '@navikt/ds-react'
import { BeregnetSamletTrygdetid } from '~components/behandling/trygdetid/detaljer/BeregnetSamletTrygdetid'

type Props = {
  beregnetTrygdetid: IDetaljertBeregnetTrygdetidResultat
}

export const TrygdetidDetaljer = ({ beregnetTrygdetid }: Props) => {
  return (
    <VStack gap="space-4" paddingBlock="space-0 space-8">
      <HStack gap="space-2" align="center">
        <CalculatorIcon fontSize={IconSize.DEFAULT} aria-hidden />
        <Heading size="small" level="3">
          Beregnet trygdetid
        </Heading>
      </HStack>
      {beregnetTrygdetid.overstyrt && (
        <HStack gap="space-2">
          <ExclamationmarkTriangleIcon fontSize={IconSize.DEFAULT} />
          <BodyShort>
            Beregnet trygdetid har blitt overstyrt ved migrering på grunn av manglende eller inkonsistent grunnlag i
            Pesys. Slå opp saken i Pesys for å se grunnlaget til tidligere vedtak.
          </BodyShort>
        </HStack>
      )}
      <VStack gap="space-8">
        <BeregnetFaktiskTrygdetid beregnetTrygdetid={beregnetTrygdetid} />
        <BeregnetFremtidigTrygdetid beregnetTrygdetid={beregnetTrygdetid} />
        <BeregnetSamletTrygdetid beregnetTrygdetid={beregnetTrygdetid} />
      </VStack>
    </VStack>
  )
}

export const formaterBeregnetTrygdetid = (periode?: string) => {
  if (!periode) {
    return ''
  }
  // ZERO-perioder representeres som P0D, så har spesialhåndtering for det tilfellet
  if (periode === 'P0D') {
    return `0 år 0 måneder`
  }

  // Legger til 0 år eksplisitt dersom perioden er under ett år
  const periodeMedAntallAar = periode.includes('Y') ? periode : 'P0Y' + periode.slice(1)

  // formatet på periode matcher /\d+Y(\d+M)?/, så en split på Y|M vil gi en array med
  // 1. år-strengen og 2. tom streng eller måned-strengen
  const [aar, maaneder] = periodeMedAntallAar.slice(1).split(/Y|M/)
  return `${aar} år${maaneder ? ` ${maaneder} måneder` : ''}`
}
