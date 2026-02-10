import { IBoddEllerArbeidetUtlandet, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { SoeknadVurdering } from '../SoeknadVurdering'
import { BoddEllerArbeidetUtlandetVurdering } from './BoddEllerArbeidetUtlandetVurdering'
import { BodyShort, Box, VStack } from '@navikt/ds-react'

const statusIkon = (boddEllerArbeidetUtlandet: IBoddEllerArbeidetUtlandet | null) => {
  if (boddEllerArbeidetUtlandet) {
    return 'success'
  }
  return 'warning'
}

export const BoddEllerArbeidetUtlandet = ({
  behandling,
  redigerbar,
}: {
  behandling: IDetaljertBehandling
  redigerbar: boolean
}) => {
  return (
    <SoeknadVurdering tittel="Utlandsopphold" hjemler={[]} status={statusIkon(behandling.boddEllerArbeidetUtlandet)}>
      <VStack gap="space-6" marginBlock="space-2" marginInline="space-0" maxWidth="41rem">
        <BodyShort>
          Hvis avdøde har bodd og/eller arbeidet i utlandet, kan dette ha innvirkning på rettighetene til og størrelsen
          på etterlatteytelser.
        </BodyShort>
        <BodyShort>
          Har avdøde utlandsopphold må det hukes av for de alternativ som gjelder. Velg aktuelle alternativ, og noter
          gjeldende land for de ulike alternativene i begrunnelsesfeltet.
        </BodyShort>
        <BodyShort>
          Vurdere avdødes trygdeavtale: huk av kun hvis det er bosatt utland-sak. Dette vurderes først i sluttbehandling
          i bosatt Norge-saker, etter opplysninger fra utland er mottatt. Unntak: hvis avdødes trygdeavtale er vurdert i
          uføretrygd/alderspensjon kan du også huke av.
        </BodyShort>
        <BodyShort>
          Det skal sendes kravpakke: huk av kun hvis det er bosatt Norge-sak og det skal sendes krav til involverte
          avtaleland det ikke er sendt krav til tidligere i annen sak til avdød. Se spørsmålstegn for mer informasjon.
        </BodyShort>
      </VStack>
      <Box
        paddingInline="space-2 space-0"
        minWidth="18.75rem"
        width="10rem"
        borderWidth="0 0 0 2"
        borderColor="neutral-subtle"
      >
        <BoddEllerArbeidetUtlandetVurdering redigerbar={redigerbar} behandlingId={behandling.id} />
      </Box>
    </SoeknadVurdering>
  )
}
