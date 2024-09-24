import { BodyShort, Label, VStack } from '@navikt/ds-react'
import { JaNei, JaNeiRec } from '~shared/types/ISvar'
import { TidligereFamiliepleierValues } from '~components/behandling/soeknadsoversikt/tidligereFamiliepleier/TidligereFamiliepleierVurdering'
import { formaterDatoMedFallback } from '~utils/formatering/dato'

const TidligereFamiliepleierVisning = (props: { tidligereFamiliepleier: TidligereFamiliepleierValues | null }) => {
  const { tidligereFamiliepleier } = props
  return (
    <>
      {tidligereFamiliepleier && tidligereFamiliepleier.svar ? (
        <>
          <VStack gap="4">
            <Label as="p" size="small">
              {JaNeiRec[tidligereFamiliepleier.svar ? JaNei.JA : JaNei.NEI]}
            </Label>
            <div>
              <BodyShort spacing>Fødselsnummer for forpleiede</BodyShort>
              <Label as="p" size="small">
                {tidligereFamiliepleier.foedselsnummer}
              </Label>
            </div>
            <div>
              <BodyShort spacing>Pleieforholdet opphørte</BodyShort>
              <Label as="p" size="small">
                {formaterDatoMedFallback(tidligereFamiliepleier.opphoertPleieforhold!!, '-')}
              </Label>
            </div>
          </VStack>
        </>
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
