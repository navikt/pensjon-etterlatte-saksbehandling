import { Content, ContentHeader } from '~shared/styled'
import { Familieforhold } from './familieforhold/Familieforhold'
import { Border, HeadingWrapper, Innhold } from './styled'
import { Heading } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { Soeknadsdato } from './soeknadoversikt/Soeknadsdato'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { behandlingErUtfylt, hentBehandlesFraStatus } from '../felles/utils'
import { useAppSelector } from '~store/Store'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { OversiktGyldigFramsatt } from '~components/behandling/soeknadsoversikt/soeknadoversikt/gyldigFramsattSoeknad/OversiktGyldigFramsatt'
import Virkningstidspunkt from '~components/behandling/soeknadsoversikt/soeknadoversikt/virkningstidspunkt/Virkningstidspunkt'
import { ISaksType } from '~components/behandling/fargetags/saksType'
import { OversiktKommerBarnetTilgode } from '~components/behandling/soeknadsoversikt/soeknadoversikt/kommerBarnetTilgode/OversiktKommerBarnetTilgode'
import { Start } from '~components/behandling/handlinger/start'

export const Soeknadsoversikt = () => {
  const behandling = useAppSelector((state) => state.behandlingReducer.behandling)
  const behandles = hentBehandlesFraStatus(behandling.status)

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading spacing size="large" level="1">
            Søknadsoversikt
          </Heading>
        </HeadingWrapper>
        <Soeknadsdato mottattDato={behandling.soeknadMottattDato} />
      </ContentHeader>
      <Innhold>
        <OversiktGyldigFramsatt gyldigFramsatt={behandling.gyldighetsprøving} sakType={behandling.sakType} />
        {behandling.gyldighetsprøving?.resultat === VurderingsResultat.OPPFYLT && (
          <>
            {behandling.sakType == ISaksType.BARNEPENSJON && (
              <OversiktKommerBarnetTilgode
                kommerBarnetTilgode={behandling.kommerBarnetTilgode}
                redigerbar={behandles}
                søker={behandling.søker}
                forelder={behandling.familieforhold?.gjenlevende}
              />
            )}
            <Virkningstidspunkt
              redigerbar={behandles}
              virkningstidspunkt={behandling.virkningstidspunkt}
              avdoedDoedsdato={behandling.familieforhold?.avdoede?.opplysning?.doedsdato}
              avdoedDoedsdatoKilde={behandling.familieforhold?.avdoede?.kilde}
              soeknadMottattDato={behandling.soeknadMottattDato}
              behandlingId={behandling.id}
            />
          </>
        )}
      </Innhold>
      <Border />
      <Familieforhold behandling={behandling} />
      {behandles ? (
        <BehandlingHandlingKnapper>
          {behandlingErUtfylt(behandling) && (
            <Start soeknadGyldigFremsatt={behandling.gyldighetsprøving?.resultat === VurderingsResultat.OPPFYLT} />
          )}
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </Content>
  )
}
