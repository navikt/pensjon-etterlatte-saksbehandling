import { Content, ContentHeader } from '~shared/styled'
import { Familieforhold } from './familieforhold/Familieforhold'
import { Border, HeadingWrapper, Innhold } from './styled'
import { ISaksType } from '../fargetags/saksType'
import { Heading, Tag } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { Start } from '../handlinger/start'
import { Soeknadsdato } from './soeknadoversikt/Soeknadsdato'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { behandlingErUtfylt, hentBehandlesFraStatus } from '../felles/utils'
import { useAppSelector } from '~store/Store'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { JaNei } from '~shared/types/ISvar'
import { tagColors, TagList } from '~shared/Tags'
import { formaterEnumTilLesbarString } from '~utils/formattering'
import { OversiktGyldigFramsatt } from '~components/behandling/soeknadsoversikt/soeknadoversikt/gyldigFramsattSoeknad/OversiktGyldigFramsatt'
import { OversiktKommerBarnetTilgode } from '~components/behandling/soeknadsoversikt/soeknadoversikt/kommerBarnetTilgode/OversiktKommerBarnetTilgode'
import Virkningstidspunkt from '~components/behandling/soeknadsoversikt/soeknadoversikt/virkningstidspunkt/Virkningstidspunkt'

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
          <div className="details">
            <TagList>
              <li>
                <Tag variant={tagColors[behandling.behandlingType]} size={'small'}>
                  {formaterEnumTilLesbarString(behandling.behandlingType)}
                </Tag>
              </li>
              <li>
                <Tag variant={tagColors[ISaksType.BARNEPENSJON]} size={'small'}>
                  {formaterEnumTilLesbarString(ISaksType.BARNEPENSJON)}
                </Tag>
              </li>
            </TagList>
          </div>
        </HeadingWrapper>
        <Soeknadsdato mottattDato={behandling.soeknadMottattDato} />
      </ContentHeader>
      <Innhold>
        <OversiktGyldigFramsatt gyldigFramsatt={behandling.gyldighetsprøving} />
        {behandling.gyldighetsprøving?.resultat === VurderingsResultat.OPPFYLT && (
          <>
            <OversiktKommerBarnetTilgode kommerBarnetTilgode={behandling.kommerBarnetTilgode} redigerbar={behandles} />
            <Virkningstidspunkt
              behandling={behandling}
              redigerbar={behandles}
              virkningstidspunkt={behandling.virkningstidspunkt}
              avdoedDoedsdato={behandling.familieforhold?.avdoede?.opplysning?.doedsdato}
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
          {behandlingErUtfylt(behandling) && behandling.kommerBarnetTilgode?.svar === JaNei.JA && (
            <Start soeknadGyldigFremsatt={behandling.gyldighetsprøving?.resultat === VurderingsResultat.OPPFYLT} />
          )}
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </Content>
  )
}
