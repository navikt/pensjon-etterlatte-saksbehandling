import { ViderefoertOpphoer } from '~shared/types/IDetaljertBehandling'
import { BodyShort, Label, VStack } from '@navikt/ds-react'
import { JaNei, JaNeiRec } from '~shared/types/ISvar'
import { formaterDatoMedFallback } from '~utils/formatering/dato'
import { Vilkaartype } from '~shared/api/vilkaarsvurdering'

export const SammendragAvViderefoereOpphoerVurdering = ({
  viderefoereOpphoer,
  vilkaartyper,
}: {
  viderefoereOpphoer: ViderefoertOpphoer | null
  vilkaartyper: Vilkaartype[]
}) => {
  return (
    viderefoereOpphoer !== null && (
      <VStack gap="4" paddingBlock="0 3">
        <BodyShort size="small">{JaNeiRec[viderefoereOpphoer.skalViderefoere]}</BodyShort>

        {viderefoereOpphoer.skalViderefoere === JaNei.JA && (
          <>
            <div>
              <Label size="small">Opphørstidspunkt</Label>
              <BodyShort size="small">{formaterDatoMedFallback(viderefoereOpphoer.dato, '-')}</BodyShort>
            </div>
            <div>
              <Label size="small">Vilkår som ikke lenger er oppfylt</Label>
              <BodyShort size="small">
                {vilkaartyper.find((type) => type.name === viderefoereOpphoer.vilkaar)?.tittel}
              </BodyShort>
            </div>
          </>
        )}
      </VStack>
    )
  )
}
