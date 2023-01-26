import { KommerBarnetTilGodeVurdering } from './KommerBarnetTilGodeVurdering'
import { Beskrivelse, VurderingsContainerWrapper } from '../../styled'
import { IKommerBarnetTilgode } from '~shared/types/IDetaljertBehandling'
import { svarTilVurderingsstatus } from '../../utils'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { JaNei } from '~shared/types/ISvar'
import { Soeknadsvurdering } from '../SoeknadsVurdering'
import { useState } from 'react'
import { LeggTilVurderingButton } from '~components/behandling/soeknadsoversikt/soeknadoversikt/LeggTilVurderingButton'

interface Props {
  kommerBarnetTilgode: IKommerBarnetTilgode | null
  redigerbar: boolean
}

export const OversiktKommerBarnetTilgode = ({ kommerBarnetTilgode, redigerbar }: Props) => {
  const [vurder, setVurder] = useState(kommerBarnetTilgode !== null)

  return (
    <Soeknadsvurdering
      tittel="Kommer pensjonen barnet tilgode?"
      vurderingsResultat={
        kommerBarnetTilgode?.svar
          ? svarTilVurderingsstatus(kommerBarnetTilgode.svar)
          : VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
      }
      hjemler={[{ lenke: '#', tittel: 'Avventer riktig lenke fra fagpersonene' }]}
      status={kommerBarnetTilgode?.svar === JaNei.JA ? 'success' : 'warning'}
    >
      <div>
        <Beskrivelse>
          Undersøk om boforholdet er avklart og det er sannsynlig at pensjonen kommer barnet til gode.
        </Beskrivelse>
      </div>

      <VurderingsContainerWrapper>
        {vurder ? (
          <KommerBarnetTilGodeVurdering
            kommerBarnetTilgode={kommerBarnetTilgode}
            redigerbar={redigerbar}
            setVurder={(visVurderingKnapp: boolean) => setVurder(visVurderingKnapp)}
          />
        ) : (
          <LeggTilVurderingButton onClick={() => setVurder(true)}>Legg til vurdering</LeggTilVurderingButton>
        )}
      </VurderingsContainerWrapper>
    </Soeknadsvurdering>
  )
}
