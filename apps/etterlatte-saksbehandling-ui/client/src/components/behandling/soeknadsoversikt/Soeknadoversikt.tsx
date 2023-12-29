import { Content, ContentHeader } from '~shared/styled'
import { Familieforhold } from './familieforhold/Familieforhold'
import { Border, HeadingWrapper, InnholdPadding } from './styled'
import { Heading } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { Soeknadsdato } from './Soeknadsdato'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { behandlingErRedigerbar, soeknadsoversiktErFerdigUtfylt } from '../felles/utils'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { OversiktGyldigFramsatt } from '~components/behandling/soeknadsoversikt/gyldigFramsattSoeknad/OversiktGyldigFramsatt'
import { Utlandstilknytning } from '~components/behandling/soeknadsoversikt/utlandstilknytning/Utlandstilknytning'
import { SakType } from '~shared/types/sak'
import { OversiktKommerBarnetTilgode } from '~components/behandling/soeknadsoversikt/kommerBarnetTilgode/OversiktKommerBarnetTilgode'
import { Start } from '~components/behandling/handlinger/start'
import { IDetaljertBehandling, UtlandstilknytningType } from '~shared/types/IDetaljertBehandling'
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
import { formaterGrunnlagKilde } from '~components/behandling/soeknadsoversikt/utils'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { useAppSelector } from '~store/Store'
import { ReactNode } from 'react'

export const Soeknadsoversikt = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  const redigerbar = behandlingErRedigerbar(behandling.status) && innloggetSaksbehandler.skriveTilgang
  const erGyldigFremsatt = behandling.gyldighetsprøving?.resultat === VurderingsResultat.OPPFYLT
  const personopplysninger = usePersonopplysninger()
  const avdoede = personopplysninger?.avdoede || []
  const erBosattUtland = behandling.utlandstilknytning?.type === UtlandstilknytningType.BOSATT_UTLAND

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

  function doedsdatoOgSoknadMottatt(): { info: ReactNode } {
    const nodes = avdoede.map((avdod, index) => (
      <Info
        key={index}
        label="Dødsdato"
        tekst={avdod?.opplysning.doedsdato ? formaterStringDato(avdod?.opplysning.doedsdato) : 'Ikke registrert'}
        undertekst={formaterGrunnlagKilde(avdod?.kilde)}
      />
    ))
    if (!avdoede.find((it) => it.opplysning.doedsdato)) {
      nodes.push(<Info label="Dødsdato" tekst="Ikke registrert" />)
    }
    if (behandling.soeknadMottattDato) {
      nodes.push(<Info label="Søknad mottatt" tekst={formaterStringDato(behandling.soeknadMottattDato)} />)
    }
    return { info: nodes }
  }

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading spacing size="large" level="1">
            Søknadsoversikt
          </Heading>
        </HeadingWrapper>
        {behandling.soeknadMottattDato && <Soeknadsdato mottattDato={behandling.soeknadMottattDato} />}
      </ContentHeader>
      <InnholdPadding>
        <OppdaterGrunnlagModal behandlingId={behandling.id} behandlingStatus={behandling.status} />
        <Utlandstilknytning behandling={behandling} redigerbar={redigerbar} />
        {personopplysninger && (
          <OversiktGyldigFramsatt behandling={behandling} personopplysninger={personopplysninger} />
        )}
        {behandling.gyldighetsprøving?.resultat === VurderingsResultat.OPPFYLT && (
          <>
            {behandling.sakType == SakType.BARNEPENSJON && (
              <OversiktKommerBarnetTilgode
                kommerBarnetTilgode={behandling.kommerBarnetTilgode}
                redigerbar={redigerbar}
                soeker={personopplysninger?.soeker?.opplysning}
                gjenlevendeForelder={personopplysninger?.gjenlevende?.find((po) => po)}
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
              {doedsdatoOgSoknadMottatt()}
            </Virkningstidspunkt>{' '}
            <BoddEllerArbeidetUtlandet behandling={behandling} redigerbar={redigerbar} />
          </>
        )}
        <SkalViseBosattUtland behandling={behandling} redigerbar={redigerbar} />
      </InnholdPadding>
      <Border />
      <Familieforhold behandling={behandling} personopplysninger={personopplysninger} />
      {redigerbar ? (
        <BehandlingHandlingKnapper>
          {soeknadsoversiktErFerdigUtfylt(behandling) && <Start disabled={!erGyldigFremsatt} />}
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </Content>
  )
}
