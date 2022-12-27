import { Content, ContentHeader } from '~shared/styled'
import { SoeknadOversikt } from './soeknadoversikt/Soeknadsoversikt'
import { Familieforhold } from './familieforhold/Familieforhold'
import { Border, HeadingWrapper } from './styled'
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

export const Soeknadsoversikt = () => {
  const behandling = useAppSelector((state) => state.behandlingReducer.behandling)
  const behandles = hentBehandlesFraStatus(behandling.status)

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading spacing size="xlarge" level="5">
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
      <SoeknadOversikt behandling={behandling} />
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


