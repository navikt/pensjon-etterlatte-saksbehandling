import { sjekkDodsfallMerEnn3AarSiden } from '../utils'
import { PropsOmSoeknad } from '../props'
import { SoeknadGyldigFremsatt } from './SoeknadGyldigFremsatt'
import styled from 'styled-components'
import { AlertVarsel } from '../AlertVarsel'
import { Foreldreansvar } from './soeknadinfo/Foreldreansvar'
import { Adresse } from './soeknadinfo/Adresse'
import { Innsender } from './soeknadinfo/Innsender'
import { Virkningstidspunkt } from './soeknadinfo/Virkningstidspunkt'
import { Soeknadsdato } from './soeknadinfo/Soeknadsdato'
import { DoedsfallDato } from './soeknadinfo/Doedsfalldato'

export const SoeknadOversikt: React.FC<PropsOmSoeknad> = ({
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
    <SoeknadOversiktWrapper>
      <InfoWrapper>
        <DoedsfallDato avdoedPersonPdl={avdoedPersonPdl} dodsfallMerEnn3AarSiden={dodsfallMerEnn3AarSiden} />
        <Soeknadsdato mottattDato={mottattDato} />
        <Virkningstidspunkt
          avdoedPersonPdl={avdoedPersonPdl}
          mottattDato={mottattDato}
          dodsfallMerEnn3AarSiden={dodsfallMerEnn3AarSiden}
        />
        <Innsender innsender={innsender} innsenderHarForeldreAnsvar={innsenderHarForeldreAnsvar} />
        <Foreldreansvar gjenlevendePdl={gjenlevendePdl} gjenlevendeHarForeldreansvar={gjenlevendeHarForeldreansvar} />
        <Adresse gjenlevendeOgSoekerLikAdresse={gjenlevendeOgSoekerLikAdresse} />
      </InfoWrapper>
      <div className="soeknadGyldigFremsatt">
        <div className="dodsfallMerEnn3AarSiden">
          {dodsfallMerEnn3AarSiden && <AlertVarsel varselType="dødsfall 3 år" />}
        </div>
        <SoeknadGyldigFremsatt gyldighet={gyldighet} />
      </div>
    </SoeknadOversiktWrapper>
  )
}

export const SoeknadOversiktWrapper = styled.div`
  display: flex;
  flex-wrap: wrap;
  margin-bottom: 2em;
  padding: 2em 5em;

  .soeknadGyldigFremsatt {
    .dodsfallMerEnn3AarSiden {
      font-size: 16px;
      min-width: 200px;
      height: 100px;
    }
  }
`

export const InfoWrapper = styled.div`
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  height: 220px;
  flex-grow: 1;
`
