import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { LovtekstMedLenke } from '~components/behandling/soeknadsoversikt/LovtekstMedLenke'
import {
  Beskrivelse,
  InfobokserWrapper,
  InfoWrapper,
  VurderingsContainerWrapper,
} from '~components/behandling/soeknadsoversikt/styled'
import { GyldigFramsattVurdering } from '~components/behandling/soeknadsoversikt/gyldigFramsattSoeknad/omstillingsstoenad/GyldigFramsattVurdering'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { formaterGrunnlagKilde } from '~components/behandling/soeknadsoversikt/utils'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { StatusIconProps } from '~shared/icons/statusIcon'
import { Personopplysning } from '~shared/types/grunnlag'
import { Verger } from '~components/behandling/soeknadsoversikt/gyldigFramsattSoeknad/Verger'
import { useAppSelector } from '~store/Store'

export const GyldigFramsattOmstillingsstoenad = ({
  behandling,
  innsender,
  gyldigFremsattTilStatusIcon,
}: {
  gyldigFremsattTilStatusIcon: StatusIconProps
  behandling: IDetaljertBehandling
  innsender?: Personopplysning
}) => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  const redigerbar = behandlingErRedigerbar(behandling.status) && innloggetSaksbehandler.skriveTilgang
  const navn = innsender ? `${innsender.opplysning.fornavn} ${innsender.opplysning.etternavn}` : 'Ukjent'
  const undertekst = formaterGrunnlagKilde(innsender?.kilde)

  return (
    <LovtekstMedLenke
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
        <Beskrivelse>
          Den som har rett til ytelsen må sette frem krav (verge hvis aktuelt). Om annet må fullmakt ligge i saken.
          Søknaden må være signert og vise hva det søkes om, og den må settes fram i bostedslandet eller i det landet
          vedkommende sist var medlem.
        </Beskrivelse>
        <InfobokserWrapper>
          <InfoWrapper>
            <Info tekst={navn} undertekst={undertekst} label="Innsender" />
          </InfoWrapper>
          <Verger behandlingId={behandling.id} sakId={behandling.sakId} />
        </InfobokserWrapper>
      </div>
      <VurderingsContainerWrapper>
        <GyldigFramsattVurdering
          behandlingId={behandling.id}
          gyldigFramsatt={behandling.gyldighetsprøving}
          redigerbar={redigerbar}
        />
      </VurderingsContainerWrapper>
    </LovtekstMedLenke>
  )
}
