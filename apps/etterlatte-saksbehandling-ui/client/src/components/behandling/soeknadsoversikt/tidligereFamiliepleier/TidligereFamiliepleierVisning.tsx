import React from 'react'
import { BodyShort, Box, Label, VStack } from '@navikt/ds-react'
import { JaNei, JaNeiRec } from '~shared/types/ISvar'
import { formaterDatoMedFallback } from '~utils/formatering/dato'
import { ITidligereFamiliepleier } from '~shared/types/IDetaljertBehandling'

const TidligereFamiliepleierVisning = (props: { tidligereFamiliepleier: ITidligereFamiliepleier | null }) => {
  const { tidligereFamiliepleier } = props
  return (
    <>
      {tidligereFamiliepleier && tidligereFamiliepleier.svar ? (
        <Box paddingBlock="space-0 space-4">
          <VStack gap="space-4">
            <BodyShort size="small">{JaNeiRec[tidligereFamiliepleier.svar ? JaNei.JA : JaNei.NEI]}</BodyShort>
            <div>
              <Label>Fødselsnummer for forpleiede</Label>
              <BodyShort size="small">{tidligereFamiliepleier.foedselsnummer}</BodyShort>
            </div>
            <div>
              <Label>Pleieforholdet startet</Label>
              <BodyShort size="small">
                {formaterDatoMedFallback(tidligereFamiliepleier.startPleieforhold!!, '-')}
              </BodyShort>
            </div>
            <div>
              <Label>Pleieforholdet opphørte</Label>
              <BodyShort size="small">
                {formaterDatoMedFallback(tidligereFamiliepleier.opphoertPleieforhold!!, '-')}
              </BodyShort>
            </div>
          </VStack>
        </Box>
      ) : (
        <>
          <BodyShort size="small" spacing>
            Nei
          </BodyShort>
        </>
      )}
    </>
  )
}

export default TidligereFamiliepleierVisning
