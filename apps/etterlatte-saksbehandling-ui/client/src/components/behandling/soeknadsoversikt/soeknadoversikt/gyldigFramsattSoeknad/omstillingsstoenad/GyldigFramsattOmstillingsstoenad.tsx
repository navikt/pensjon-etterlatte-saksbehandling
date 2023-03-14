import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { Soeknadsvurdering } from '~components/behandling/soeknadsoversikt/soeknadoversikt/SoeknadsVurdering'
import {
  Beskrivelse,
  InfobokserWrapper,
  InfoWrapper,
  VurderingsContainerWrapper,
} from '~components/behandling/soeknadsoversikt/styled'
import { GyldigFramsattVurdering } from '~components/behandling/soeknadsoversikt/soeknadoversikt/gyldigFramsattSoeknad/omstillingsstoenad/GyldigFramsattVurdering'
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
    <Soeknadsvurdering
      tittel="Er søknad gyldig fremsatt?"
      hjemler={[
        {
          lenke: 'https://lovdata.no/dokument/NL/lov/1997-02-28-19/KAPITTEL_8-2#%C2%A722-13',
          tittel: 'Folketrygdloven § 22-13 første ledd',
        },
      ]}
      status={gyldigFremsattTilStatusIcon}
    >
      <div>
        <Beskrivelse>Gjenlevende selv må være innsender av søknad om omstillingsstønad.</Beskrivelse>
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
    </Soeknadsvurdering>
  )
}
