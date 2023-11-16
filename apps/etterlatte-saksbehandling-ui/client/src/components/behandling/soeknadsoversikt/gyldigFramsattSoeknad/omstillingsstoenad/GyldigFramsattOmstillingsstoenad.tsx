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
import { formaterKildePdl } from '~components/behandling/soeknadsoversikt/utils'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'

export const GyldigFramsattOmstillingsstoenad = ({
  behandling,
  gyldigFremsattTilStatusIcon,
}: {
  gyldigFremsattTilStatusIcon: 'success' | 'error' | 'warning'
  behandling: IDetaljertBehandling
}) => {
  const behandles = hentBehandlesFraStatus(behandling.status)
  const innsender = behandling.familieforhold?.gjenlevende
  const navn = innsender ? `${innsender.opplysning.fornavn} ${innsender.opplysning.etternavn}` : 'Ukjent'
  const undertekst = formaterKildePdl(innsender?.kilde)

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
        </InfobokserWrapper>
      </div>
      <VurderingsContainerWrapper>
        <GyldigFramsattVurdering
          behandlingId={behandling.id}
          gyldigFramsatt={behandling.gyldighetsprøving}
          redigerbar={behandles}
        />
      </VurderingsContainerWrapper>
    </LovtekstMedLenke>
  )
}
