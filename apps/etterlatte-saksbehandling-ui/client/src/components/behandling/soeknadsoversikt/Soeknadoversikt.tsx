import { Content, ContentHeader } from '~shared/styled'
import { Familieforhold } from './familieforhold/Familieforhold'
import { Border, HeadingWrapper, InnholdPadding } from './styled'
import { Heading } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { Soeknadsdato } from './Soeknadsdato'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { behandlingErRedigerbar, behandlingErUtfylt } from '../felles/utils'
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
  BP_FOERSTEGANGSBEHANDLING_BESKRIVELSE,
  BP_FOERSTEGANGSBEHANDLING_BOSATT_UTLAND_BESKRIVELSE,
  BP_FOERSTEGANGSBEHANDLING_BOSATT_UTLAND_HJEMLER,
  BP_FOERSTEGANGSBEHANDLING_HJEMLER,
  OMS_FOERSTEGANGSBEHANDLING_BESKRIVELSE,
  OMS_FOERSTEGANGSBEHANDLING_BOSATT_UTLAND_BESKRIVELSE,
  OMS_FOERSTEGANGSBEHANDLING_BOSATT_UTLAND_HJEMLER,
  OMS_FOERSTEGANGSBEHANDLING_HJEMLER,
} from '~components/behandling/virkningstidspunkt/utils'
import Virkningstidspunkt from '~components/behandling/virkningstidspunkt/Virkningstidspunkt'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { formaterStringDato } from '~utils/formattering'
import { formaterKildePdl } from '~components/behandling/soeknadsoversikt/utils'

export const Soeknadsoversikt = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const redigerbar = behandlingErRedigerbar(behandling.status)
  const erGyldigFremsatt = behandling.gyldighetsprøving?.resultat === VurderingsResultat.OPPFYLT
  const avdoedDoedsdato = behandling.familieforhold?.avdoede?.opplysning?.doedsdato
  const avdoedDoedsdatoKilde = behandling.familieforhold?.avdoede?.kilde
  const erBosattUtland = behandling.utenlandstilknytning?.type === UtenlandstilknytningType.BOSATT_UTLAND

  const hjemlerVirkningstidspunkt = (sakType: SakType, erBosattUtland: boolean) => {
    switch (sakType) {
      case SakType.BARNEPENSJON:
        return erBosattUtland ? BP_FOERSTEGANGSBEHANDLING_BOSATT_UTLAND_HJEMLER : BP_FOERSTEGANGSBEHANDLING_HJEMLER
      case SakType.OMSTILLINGSSTOENAD:
        return erBosattUtland ? OMS_FOERSTEGANGSBEHANDLING_BOSATT_UTLAND_HJEMLER : OMS_FOERSTEGANGSBEHANDLING_HJEMLER
    }
  }

  const beskrivelseVirkningstidspunkt = (sakType: SakType, erBosattUtland: boolean) => {
    switch (sakType) {
      case SakType.BARNEPENSJON:
        return erBosattUtland
          ? BP_FOERSTEGANGSBEHANDLING_BOSATT_UTLAND_BESKRIVELSE
          : BP_FOERSTEGANGSBEHANDLING_BESKRIVELSE
      case SakType.OMSTILLINGSSTOENAD:
        return erBosattUtland
          ? OMS_FOERSTEGANGSBEHANDLING_BOSATT_UTLAND_BESKRIVELSE
          : OMS_FOERSTEGANGSBEHANDLING_BESKRIVELSE
    }
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
        <Utenlandstilknytning behandling={behandling} redigerbar={redigerbar} />
        <OversiktGyldigFramsatt behandling={behandling} />
        {behandling.gyldighetsprøving?.resultat === VurderingsResultat.OPPFYLT && (
          <>
            {behandling.sakType == SakType.BARNEPENSJON && (
              <OversiktKommerBarnetTilgode
                kommerBarnetTilgode={behandling.kommerBarnetTilgode}
                redigerbar={redigerbar}
                soeker={behandling.søker}
                gjenlevendeForelder={behandling.familieforhold?.gjenlevende}
                behandlingId={behandling.id}
              />
            )}
            <Virkningstidspunkt
              erBosattUtland={erBosattUtland}
              redigerbar={redigerbar}
              behandling={behandling}
              hjemler={hjemlerVirkningstidspunkt(behandling.sakType, erBosattUtland)}
              beskrivelse={beskrivelseVirkningstidspunkt(behandling.sakType, erBosattUtland)}
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
            <BoddEllerArbeidetUtlandet behandling={behandling} redigerbar={redigerbar} />
          </>
        )}
        <SkalViseBosattUtland behandling={behandling} redigerbar={redigerbar} />
      </InnholdPadding>
      <Border />
      <Familieforhold behandling={behandling} />
      {redigerbar ? (
        <BehandlingHandlingKnapper>
          {behandlingErUtfylt(behandling) && <Start disabled={!erGyldigFremsatt} />}
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </Content>
  )
}
