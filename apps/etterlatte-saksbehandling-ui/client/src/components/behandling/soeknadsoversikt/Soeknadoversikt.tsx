import { Content, ContentHeader } from '~shared/styled'
import { Familieforhold } from './familieforhold/Familieforhold'
import { Border, HeadingWrapper, Innhold } from './styled'
import { Heading } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { Soeknadsdato } from './soeknadoversikt/Soeknadsdato'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { behandlingErUtfylt, hentBehandlesFraStatus } from '../felles/utils'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { OversiktGyldigFramsatt } from '~components/behandling/soeknadsoversikt/soeknadoversikt/gyldigFramsattSoeknad/OversiktGyldigFramsatt'
import { Utenlandstilsnitt } from '~components/behandling/soeknadsoversikt/soeknadoversikt/utenlandstilsnitt/Utenlandstilsnitt'
import { ISaksType } from '~components/behandling/fargetags/saksType'
import { OversiktKommerBarnetTilgode } from '~components/behandling/soeknadsoversikt/soeknadoversikt/kommerBarnetTilgode/OversiktKommerBarnetTilgode'
import { Start } from '~components/behandling/handlinger/start'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import {
  BP_FOERSTEGANGSBEHANDLING_BESKRIVELSE,
  BP_FOERSTEGANGSBEHANDLING_HJEMLER,
  OMS_FOERSTEGANGSBEHANDLING_BESKRIVELSE,
} from '~components/behandling/soeknadsoversikt/soeknadoversikt/virkningstidspunkt/utils'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { formaterKildePdl } from '~components/behandling/soeknadsoversikt/utils'
import { formaterStringDato } from '~utils/formattering'
import Virkningstidspunkt from '~components/behandling/soeknadsoversikt/soeknadoversikt/virkningstidspunkt/Virkningstidspunkt'

export const Soeknadsoversikt = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const behandles = hentBehandlesFraStatus(behandling.status)
  const erGyldigFremsatt = behandling.gyldighetsprøving?.resultat === VurderingsResultat.OPPFYLT

  const avdoedDoedsdato = behandling.familieforhold?.avdoede?.opplysning?.doedsdato
  const avdoedDoedsdatoKilde = behandling.familieforhold?.avdoede?.kilde

  const soeknadMottattDato = behandling.soeknadMottattDato

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
        <OversiktGyldigFramsatt behandling={behandling} />
        {behandling.gyldighetsprøving?.resultat === VurderingsResultat.OPPFYLT && (
          <>
            {behandling.sakType == ISaksType.BARNEPENSJON && (
              <OversiktKommerBarnetTilgode
                kommerBarnetTilgode={behandling.kommerBarnetTilgode}
                redigerbar={behandles}
                soeker={behandling.søker}
                gjenlevendeForelder={behandling.familieforhold?.gjenlevende}
                behandlingId={behandling.id}
              />
            )}
            <Virkningstidspunkt
              redigerbar={behandles}
              virkningstidspunkt={behandling.virkningstidspunkt}
              avdoedDoedsdato={behandling.familieforhold?.avdoede?.opplysning?.doedsdato}
              avdoedDoedsdatoKilde={behandling.familieforhold?.avdoede?.kilde}
              soeknadMottattDato={behandling.soeknadMottattDato}
              behandlingId={behandling.id}
              hjemmler={BP_FOERSTEGANGSBEHANDLING_HJEMLER}
              beskrivelse={
                behandling.sakType === 'BARNEPENSJON'
                  ? BP_FOERSTEGANGSBEHANDLING_BESKRIVELSE
                  : OMS_FOERSTEGANGSBEHANDLING_BESKRIVELSE
              }
            >
              {{
                info: (
                  <>
                    <Info
                      label="Dødsdato"
                      tekst={avdoedDoedsdato ? formaterStringDato(avdoedDoedsdato) : ''}
                      undertekst={formaterKildePdl(avdoedDoedsdatoKilde)}
                    />
                    <Info label="Søknad mottatt" tekst={formaterStringDato(soeknadMottattDato)} />
                  </>
                ),
              }}
            </Virkningstidspunkt>
            <Utenlandstilsnitt behandling={behandling} redigerbar={behandles} />
          </>
        )}
      </Innhold>
      <Border />
      <Familieforhold behandling={behandling} />
      {behandles ? (
        <BehandlingHandlingKnapper>
          {behandlingErUtfylt(behandling) && <Start disabled={!erGyldigFremsatt} />}
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </Content>
  )
}
