import styled from 'styled-components'
import { GjenlevendeForelder } from './../personer/GjenlevendeForelder'
import { Barn } from '../personer/Barn'
import { Border, DashedBorder } from '../../styled'
import { SoeskenListe } from './../personer/Soesken'
import { GyldigFramsattType, IDetaljertBehandling, IGyldighetproving } from '~shared/types/IDetaljertBehandling'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { Foreldre } from '~components/behandling/soeknadsoversikt/familieforhold/personer/Foreldre'

export interface PropsFamilieforhold {
  behandling: IDetaljertBehandling
}

export const FamilieforholdBarnepensjon: React.FC<PropsFamilieforhold> = ({ behandling }) => {
  const innsenderErGjenlevende =
    behandling.gyldighetsprøving?.vurderinger.find(
      (g: IGyldighetproving) => g.navn === GyldigFramsattType.INNSENDER_ER_FORELDER
    )?.resultat === VurderingsResultat.OPPFYLT

  if (behandling.familieforhold == null || behandling.søker == null) {
    return <div style={{ color: 'red' }}>Familieforhold kan ikke hentes ut</div>
  }

  const doedsdato = behandling.familieforhold.avdoede.opplysning.doedsdato

  return (
    <>
      {behandling.gyldighetsprøving?.resultat === VurderingsResultat.OPPFYLT ? (
        <>
          <FamilieforholdWrapper>
            <Barn person={behandling.søker} doedsdato={doedsdato} />
            <DashedBorder />
            <Foreldre
              gjenlevende={behandling.familieforhold.gjenlevende.opplysning}
              innsenderErGjenlevende={innsenderErGjenlevende}
              doedsdato={doedsdato}
              avdoed={behandling.familieforhold.avdoede.opplysning}
            />
            <DashedBorder />
            <SoeskenListe soekerFnr={behandling.søker.foedselsnummer} familieforhold={behandling.familieforhold!!} />
          </FamilieforholdWrapper>
        </>
      ) : (
        <>
          <FamilieforholdWrapper>
            <GjenlevendeForelder
              person={behandling.familieforhold?.gjenlevende.opplysning}
              innsenderErGjenlevendeForelder={innsenderErGjenlevende}
              doedsdato={doedsdato}
            />
          </FamilieforholdWrapper>
        </>
      )}
      <Border />
    </>
  )
}

export const FamilieforholdWrapper = styled.div`
  padding: 0em 5em;
`
