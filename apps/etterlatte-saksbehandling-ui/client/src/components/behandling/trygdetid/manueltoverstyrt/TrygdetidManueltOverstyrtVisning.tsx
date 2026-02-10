import React from 'react'
import { BodyLong, BodyShort, Box, Button, Heading, VStack } from '@navikt/ds-react'
import { IDetaljertBeregnetTrygdetid } from '~shared/api/trygdetid'
import { PencilIcon } from '@navikt/aksel-icons'

export const TrygdetidManueltOverstyrtVisning = ({
  beregnetTrygdetid,
  setVisSkjema,
  redigerbar,
}: {
  beregnetTrygdetid: IDetaljertBeregnetTrygdetid
  setVisSkjema: (visSkjema: boolean) => void
  redigerbar: boolean
}) => {
  const harProrata = !!beregnetTrygdetid.resultat.prorataBroek
  const trygdetidAar = harProrata
    ? beregnetTrygdetid.resultat.samletTrygdetidTeoretisk
    : beregnetTrygdetid.resultat.samletTrygdetidNorge

  return (
    <VStack gap="space-4">
      <Box>
        <Heading size="xsmall">Anvendt trygdetid</Heading>
        <BodyShort>{trygdetidAar} år</BodyShort>
      </Box>

      {harProrata && (
        <Box>
          <Heading size="xsmall">Prorata brøk</Heading>
          <BodyShort>
            {beregnetTrygdetid.resultat.prorataBroek?.teller} / {beregnetTrygdetid.resultat.prorataBroek?.nevner}
          </BodyShort>
        </Box>
      )}

      <Box>
        <Heading size="xsmall">Begrunnelse</Heading>
        <BodyLong>{beregnetTrygdetid.resultat.overstyrtBegrunnelse}</BodyLong>
      </Box>

      {redigerbar && (
        <Box>
          <Button icon={<PencilIcon />} size="small" variant="tertiary" onClick={() => setVisSkjema(true)}>
            Rediger
          </Button>
        </Box>
      )}
    </VStack>
  )
}
