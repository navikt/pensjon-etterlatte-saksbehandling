import { Detail } from '@navikt/ds-react'
import { InfoWrapper, DetailWrapper } from '../styled'
import { format } from 'date-fns'
import { sjekkDodsfallMerEnn3AarSiden, hentVirkningstidspunkt } from './utils'
import { PropsOmSoeknad } from '../props'
import { SoeknadGyldigFremsatt } from './SoeknadGyldigFremsatt'
import styled from 'styled-components'
import { AlertVarsel } from './AlertVarsel'
import { GyldighetVurderingsResultat } from '../../../../store/reducers/BehandlingReducer'

export const OmSoeknad: React.FC<PropsOmSoeknad> = ({
  gyldighet,
  avdoedPersonPdl,
  innsender,
  mottattDato,
  gjenlevendePdl,
  gjenlevendeHarForeldreansvar,
  gjenlevendeOgSoekerLikAdresse,
  innsenderHarForeldreAnsvar,
}) => {
  const dodsfallMerEnn3AarSiden = sjekkDodsfallMerEnn3AarSiden(avdoedPersonPdl?.doedsdato, mottattDato)

  return (
    <SoeknadOversikt>
      <InfoWrapper>
        <DetailWrapper>
          <Detail size="medium">Dato for dødsfall</Detail>
          <span className={dodsfallMerEnn3AarSiden ? 'warningText' : ''}>
            {format(new Date(avdoedPersonPdl?.doedsdato), 'dd.MM.yyyy')}
          </span>
        </DetailWrapper>
        <DetailWrapper>
          <Detail size="medium">Søknadsdato</Detail>
          {format(new Date(mottattDato), 'dd.MM.yyyy')}
        </DetailWrapper>
        <DetailWrapper>
          <Detail size="medium" className="text">
            Første mulig virkningstidspunkt
          </Detail>
          <span className={dodsfallMerEnn3AarSiden ? 'warningText' : ''}>
            {format(new Date(hentVirkningstidspunkt(avdoedPersonPdl?.doedsdato, mottattDato)), 'dd.MM.yyyy')}
          </span>
        </DetailWrapper>
        <DetailWrapper>
          <Detail size="medium">Innsender</Detail>
          <div>
            {innsender?.fornavn} {innsender?.etternavn}
            <div>
              {innsenderHarForeldreAnsvar?.resultat === GyldighetVurderingsResultat.OPPFYLT && '(gjenlevende forelder)'}
            </div>
          </div>
        </DetailWrapper>
        <DetailWrapper>
          <Detail size="medium">Foreldreansvar</Detail>
          {gjenlevendeHarForeldreansvar?.resultat === GyldighetVurderingsResultat.OPPFYLT ? (
            <div>
              {gjenlevendePdl?.fornavn} {gjenlevendePdl?.etternavn}
              <div>(gjenlevende forelder)</div>
            </div>
          ) : (
            <span className="warningText">Mangler info</span>
          )}
        </DetailWrapper>
        <DetailWrapper>
          <Detail size="medium">Adresse</Detail>
          {gjenlevendeOgSoekerLikAdresse?.resultat === GyldighetVurderingsResultat.OPPFYLT ? (
            <div className="text">Barnet bor på samme adresse som gjenlevende forelder</div>
          ) : (
            <span className="warningText">Barnet bor ikke på samme adresse som gjenlevende forelder</span>
          )}
        </DetailWrapper>
      </InfoWrapper>
      <div className="soeknadGyldigFremsatt">
        <DetailWrapper>{dodsfallMerEnn3AarSiden && <AlertVarsel varselType="dødsfall 3 år" />}</DetailWrapper>
        <SoeknadGyldigFremsatt gyldighet={gyldighet} />
      </div>
    </SoeknadOversikt>
  )
}

export const SoeknadOversikt = styled.div`
  display: flex;
  flex-wrap: wrap;
  margin-bottom: 5em;
  border-top: 1px solid #b0b0b0;
  padding-top: 2em;
  .soeknadGyldigFremsatt {
    height: 200px;
    margin-bottom: 0.5em;
  }
`
