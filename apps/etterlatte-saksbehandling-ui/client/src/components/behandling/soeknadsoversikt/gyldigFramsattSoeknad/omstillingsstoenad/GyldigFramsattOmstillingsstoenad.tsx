/*
TODO: Aksel Box migration:
Could not migrate the following:
  - borderColor=border-neutral-subtle
*/

import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { SoeknadVurdering } from '~components/behandling/soeknadsoversikt/SoeknadVurdering'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { formaterGrunnlagKilde } from '~components/behandling/soeknadsoversikt/utils'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { StatusIconProps } from '~shared/icons/statusIcon'
import { Personopplysning } from '~shared/types/grunnlag'
import { Verger } from '~components/behandling/soeknadsoversikt/gyldigFramsattSoeknad/Verger'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { Box, HStack } from '@navikt/ds-react'
import { GyldigFramsattVurdering } from '~components/behandling/soeknadsoversikt/gyldigFramsattSoeknad/GyldigFramsattVurdering'

export const GyldigFramsattOmstillingsstoenad = ({
  behandling,
  innsender,
  gyldigFremsattTilStatusIcon,
}: {
  gyldigFremsattTilStatusIcon: StatusIconProps
  behandling: IDetaljertBehandling
  innsender?: Personopplysning
}) => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )
  const navn = innsender ? `${innsender.opplysning.fornavn} ${innsender.opplysning.etternavn}` : 'Ukjent'
  const undertekst = formaterGrunnlagKilde(innsender?.kilde)

  return (
    <SoeknadVurdering
      tittel="Vurdering - søknad gyldig fremsatt"
      hjemler={[
        {
          lenke: 'https://lovdata.no/dokument/NL/lov/1997-02-28-19/KAPITTEL_8-2#%C2%A722-13',
          tittel: 'Folketrygdloven § 22-13 første ledd',
        },
        {
          lenke: 'https://lovdata.no/pro/rundskriv/a45-01-02/ARTIKKEL_45',
          tittel: 'Forordning 987/2009 artikkel 45 nr. 4',
        },
      ]}
      status={gyldigFremsattTilStatusIcon}
    >
      <div>
        <Box marginBlock="space-2" marginInline="space-0" maxWidth="41rem">
          Den som har rett til ytelsen må sette frem krav (verge hvis aktuelt). Om annet må fullmakt ligge i saken.
          Søknaden må være signert og vise hva det søkes om, og den må settes fram i bostedslandet eller i det landet
          vedkommende sist var medlem.
        </Box>
        <HStack gap="space-4">
          <Info tekst={navn} undertekst={undertekst} label="Innsender" />
          <Verger behandlingId={behandling.id} sakId={behandling.sakId} />
        </HStack>
      </div>
      <Box
        paddingInline="space-2 space-0"
        minWidth="18.75rem"
        width="10rem"
        borderWidth="0 0 0 2"
        borderColor="border-neutral-subtle"
      >
        <GyldigFramsattVurdering
          behandlingId={behandling.id}
          gyldigFramsatt={behandling.gyldighetsprøving}
          redigerbar={redigerbar}
        />
      </Box>
    </SoeknadVurdering>
  )
}
