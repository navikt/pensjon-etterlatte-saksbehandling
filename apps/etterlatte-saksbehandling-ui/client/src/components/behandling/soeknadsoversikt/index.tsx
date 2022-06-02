import { useContext } from 'react'
import { AppContext } from '../../../store/AppContext'
import { Content, ContentHeader } from '../../../shared/styled'
import { SoeknadOversikt } from './soeknadoversikt/Soeknadsoversikt'
import { Familieforhold, FamilieforholdWrapper } from './familieforhold/Familieforhold'
import { usePersonInfoFromBehandling } from '../usePersonInfoFromBehandling'
import { GyldigFramsattType, IGyldighetproving, VurderingsResultat } from '../../../store/reducers/BehandlingReducer'
import { Border, HeadingWrapper } from './styled'
import { BehandlingsStatusSmall, IBehandlingsStatus } from '../behandlings-status'
import { BehandlingsTypeSmall, IBehandlingsType } from '../behandlings-type'
import { Heading } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { Start } from '../handlinger/start'
import { Soeknadsdato } from './soeknadoversikt/Soeknadsdato'
import { GjenlevendeForelder } from './familieforhold/personer/GjenlevendeForelder'

export const Soeknadsoversikt = () => {
  const { mottattDato } = usePersonInfoFromBehandling()

  const ctx = useContext(AppContext)
  const gyldigFramsatt = ctx.state.behandlingReducer.gyldighetsprøving
  const kommerSoekerTilgode = ctx.state.behandlingReducer.kommerSoekerTilgode
  const innsenderErGjenlevende =
    gyldigFramsatt.vurderinger.find((g: IGyldighetproving) => g.navn === GyldigFramsattType.INNSENDER_ER_FORELDER)
      ?.resultat === VurderingsResultat.OPPFYLT

  console.log('kommerSoekerTilgode', kommerSoekerTilgode)

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading spacing size="xlarge" level="5">
            Søknadsoversikt
          </Heading>
          <div className="details">
            <BehandlingsStatusSmall status={IBehandlingsStatus.FORSTEGANG} />
            <BehandlingsTypeSmall type={IBehandlingsType.BARNEPENSJON} />
          </div>
        </HeadingWrapper>
        <Soeknadsdato mottattDato={mottattDato} />
      </ContentHeader>
      <SoeknadOversikt gyldigFramsatt={gyldigFramsatt} kommerSoekerTilgode={kommerSoekerTilgode} />
      <Border />
      {gyldigFramsatt.resultat === VurderingsResultat.OPPFYLT ? (
        <Familieforhold kommerSoekerTilgode={kommerSoekerTilgode} gyldigFramsatt={gyldigFramsatt} />
      ) : (
        { innsenderErGjenlevende } && (
          <>
            <FamilieforholdWrapper>
              <GjenlevendeForelder
                person={kommerSoekerTilgode.familieforhold.gjenlevendeForelder}
                innsenderErGjenlevendeForelder={innsenderErGjenlevende}
              />
            </FamilieforholdWrapper>
            <Border />
          </>
        )
      )}

      <BehandlingHandlingKnapper>
        <Start soeknadGyldigFremsatt={gyldigFramsatt.resultat === VurderingsResultat.OPPFYLT} />
      </BehandlingHandlingKnapper>
    </Content>
  )
}
