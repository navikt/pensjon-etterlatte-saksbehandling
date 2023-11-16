import { Content, ContentHeader } from '~shared/styled'
import { Familieforhold } from './familieforhold/Familieforhold'
import { Border, HeadingWrapper, InnholdPadding } from './styled'
import { Heading } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { Soeknadsdato } from './Soeknadsdato'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { behandlingErUtfylt, hentBehandlesFraStatus } from '../felles/utils'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { OversiktGyldigFramsatt } from '~components/behandling/soeknadsoversikt/gyldigFramsattSoeknad/OversiktGyldigFramsatt'
import { Utenlandstilknytning } from '~components/behandling/soeknadsoversikt/utenlandstilknytning/Utenlandstilknytning'
import { SakType } from '~shared/types/sak'
import { OversiktKommerBarnetTilgode } from '~components/behandling/soeknadsoversikt/kommerBarnetTilgode/OversiktKommerBarnetTilgode'
import { Start } from '~components/behandling/handlinger/start'
import { IDetaljertBehandling, UtenlandstilknytningType } from '~shared/types/IDetaljertBehandling'
import { BoddEllerArbeidetUtlandet } from '~components/behandling/soeknadsoversikt/boddEllerArbeidetUtlandet/BoddEllerArbeidetUtlandet'
import OppdaterGrunnlagModal from '~components/behandling/handlinger/OppdaterGrunnlagModal'
import { SkalViseBosattUtland } from '~components/behandling/soeknadsoversikt/bosattUtland/SkalViseBosattUtland'
import {
  BOSATT_UTLAND_FELLES_BESKRIVELSE,
  BP_FOERSTEGANGSBEHANDLING_BESKRIVELSE,
  BP_FOERSTEGANGSBEHANDLING_HJEMLER,
  OMS_FOERSTEGANGSBEHANDLING_BESKRIVELSE,
} from '~components/behandling/virkningstidspunkt/utils'
import Virkningstidspunkt from '~components/behandling/virkningstidspunkt/Virkningstidspunkt'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { formaterStringDato } from '~utils/formattering'
import { formaterKildePdl } from '~components/behandling/soeknadsoversikt/utils'

export const Soeknadsoversikt = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const behandles = hentBehandlesFraStatus(behandling.status)
  const erGyldigFremsatt = behandling.gyldighetsprøving?.resultat === VurderingsResultat.OPPFYLT
  const avdoedDoedsdato = behandling.familieforhold?.avdoede?.opplysning?.doedsdato
  const avdoedDoedsdatoKilde = behandling.familieforhold?.avdoede?.kilde
  const erBosattUtland = behandling.utenlandstilknytning?.type === UtenlandstilknytningType.BOSATT_UTLAND

  let hjemler = BP_FOERSTEGANGSBEHANDLING_HJEMLER
  if (erBosattUtland) {
    hjemler = hjemler.concat([
      { lenke: 'https://lovdata.no/pro/eu/32004r0883/ARTIKKEL_81', tittel: 'EØS forordning 883/2004 art 81"' },
    ])
  }

  let beskrivelse =
    behandling.sakType === SakType.BARNEPENSJON
      ? BP_FOERSTEGANGSBEHANDLING_BESKRIVELSE
      : OMS_FOERSTEGANGSBEHANDLING_BESKRIVELSE
  if (erBosattUtland) {
    beskrivelse = BOSATT_UTLAND_FELLES_BESKRIVELSE
  }

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
      <InnholdPadding>
        <OppdaterGrunnlagModal behandlingId={behandling.id} behandlingStatus={behandling.status} />
        <Utenlandstilknytning behandling={behandling} redigerbar={behandles} />
        <OversiktGyldigFramsatt behandling={behandling} />
        {behandling.gyldighetsprøving?.resultat === VurderingsResultat.OPPFYLT && (
          <>
            {behandling.sakType == SakType.BARNEPENSJON && (
              <OversiktKommerBarnetTilgode
                kommerBarnetTilgode={behandling.kommerBarnetTilgode}
                redigerbar={behandles}
                soeker={behandling.søker}
                gjenlevendeForelder={behandling.familieforhold?.gjenlevende}
                behandlingId={behandling.id}
              />
            )}
            <Virkningstidspunkt
              erBosattUtland={erBosattUtland}
              redigerbar={behandles}
              behandling={behandling}
              hjemler={hjemler}
              beskrivelse={beskrivelse}
            >
              {{
                info: (
                  <>
                    <Info
                      label="Dødsdato"
                      tekst={avdoedDoedsdato ? formaterStringDato(avdoedDoedsdato) : 'Ikke registrert!'}
                      undertekst={formaterKildePdl(avdoedDoedsdatoKilde)}
                    />
                    <Info label="Søknad mottatt" tekst={formaterStringDato(behandling.soeknadMottattDato)} />
                  </>
                ),
              }}
            </Virkningstidspunkt>{' '}
            <BoddEllerArbeidetUtlandet behandling={behandling} redigerbar={behandles} />
          </>
        )}
        <SkalViseBosattUtland behandling={behandling} />
      </InnholdPadding>
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
