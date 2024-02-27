import { Textarea } from '@navikt/ds-react'
import React, { useState } from 'react'
import { BeslutningWrapper, Text } from '../styled'
import { AttesterVedtak } from '~components/behandling/attestering/handinger/attesterVedtak'
import { behandlingSkalSendeBrev } from '~components/behandling/felles/utils'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { useBehandling } from '~components/behandling/useBehandling'
import { useKlage } from '~components/klage/useKlage'
import { useTilbakekreving } from '~components/tilbakekreving/useTilbakekreving'

export const Godkjenn = () => {
  const [tilbakemeldingFraAttestant, setTilbakemeldingFraAttestant] = useState('')

  const personopplysninger = usePersonopplysninger()
  const behandling = useBehandling()
  const klage = useKlage()
  const tilbakekreving = useTilbakekreving()
  type AttesterParams = {
    soekerIdent: string
    behandlingId: string
    skalFerdigstilleBrev: boolean
    skalSendeBrev: boolean
  }
  const params = getAttesterParams()
  return (
    <BeslutningWrapper>
      <div>
        <Text>Tilbakemelding fra attestant</Text>
        <Textarea
          style={{ padding: '10px' }}
          label="Kommentar fra attestant"
          hideLabel={true}
          placeholder="Beskriv etterarbeid som er gjort. f.eks. overført oppgave til NØP om kontonummer / skattetrekk."
          value={tilbakemeldingFraAttestant}
          onChange={(e) => setTilbakemeldingFraAttestant(e.target.value)}
          minRows={3}
          size="small"
          autoComplete="off"
        />
      </div>
      <br />
      <AttesterVedtak
        soekerIdent={params.soekerIdent}
        skalSendeBrev={params.skalSendeBrev}
        skalFerdigstilleBrev={params.skalFerdigstilleBrev}
        behandlingId={params.behandlingId}
        kommentar={tilbakemeldingFraAttestant}
      />
    </BeslutningWrapper>
  )

  function getAttesterParams(): AttesterParams {
    if (behandling) {
      return {
        behandlingId: behandling.id,
        soekerIdent: personopplysninger!!.soeker!!.opplysning.foedselsnummer,
        skalFerdigstilleBrev: true,
        skalSendeBrev: behandlingSkalSendeBrev(behandling.behandlingType, behandling.revurderingsaarsak),
      }
    }
    if (tilbakekreving) {
      return {
        behandlingId: tilbakekreving.id,
        soekerIdent: tilbakekreving.sak.ident,
        skalFerdigstilleBrev: false,
        skalSendeBrev: false,
      }
    }
    if (klage) {
      return {
        behandlingId: klage.id,
        soekerIdent: klage.sak.ident,
        skalFerdigstilleBrev: false,
        skalSendeBrev: false,
      }
    }
    throw Error('Mangler behandling')
  }
}
