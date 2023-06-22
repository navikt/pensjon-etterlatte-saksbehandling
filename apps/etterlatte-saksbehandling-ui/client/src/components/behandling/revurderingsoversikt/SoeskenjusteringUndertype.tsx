import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import {
  BarnepensjonSoeskenjusteringGrunn,
  RevurderingInfo,
  SOESKENJUSTERING_GRUNNER,
  SoeskenjusteringInfo,
  tekstSoeskenjustering,
} from '~shared/types/RevurderingInfo'
import { FormEvent, useState } from 'react'
import { BodyShort, Button, Heading, Select } from '@navikt/ds-react'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'
import { isPending, useApiCall } from '~shared/hooks/useApiCall'
import { lagreRevurderingInfo } from '~shared/api/revurdering'
import { ApiErrorAlert } from '~ErrorBoundary'
import { oppdaterRevurderingInfo } from '~store/reducers/BehandlingReducer'
import styled from 'styled-components'

function hentUndertypeFraBehandling(behandling?: IDetaljertBehandling): SoeskenjusteringInfo | null {
  const revurderinginfo = behandling?.revurderinginfo
  if (revurderinginfo?.type == 'SOESKENJUSTERING') {
    return revurderinginfo
  } else {
    return null
  }
}

export const SoeskenjusteringUndertype = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const soeskenjusteringInfo = hentUndertypeFraBehandling(behandling)
  const [valgtSoeskenjustering, setValgtSoeskenjustering] = useState<BarnepensjonSoeskenjusteringGrunn | undefined>(
    soeskenjusteringInfo?.grunnForSoeskenjustering
  )
  const [feilmelding, setFeilmelding] = useState<string | undefined>(undefined)
  const [lagrestatus, lagre] = useApiCall(lagreRevurderingInfo)
  const redigerbar = hentBehandlesFraStatus(behandling.status)
  const handlesubmit = (e: FormEvent) => {
    e.stopPropagation()
    setFeilmelding(undefined)
    if (valgtSoeskenjustering === undefined) {
      setFeilmelding('Du må velge hvorfor du gjennomfører en søskenjustering')
      return
    }
    const revurderingInfo: RevurderingInfo = {
      type: 'SOESKENJUSTERING',
      grunnForSoeskenjustering: valgtSoeskenjustering,
    }
    lagre(
      {
        behandlingId: behandling.id,
        revurderingInfo,
      },
      () => oppdaterRevurderingInfo(revurderingInfo)
    )
  }

  return (
    <MarginTop>
      <Heading size="medium" level="2">
        Velg brevmal for søskenjusteringen
      </Heading>
      {redigerbar ? (
        <form onSubmit={handlesubmit}>
          <Select
            label="Hvorfor søskenjusteres det?"
            onChange={(e) => setValgtSoeskenjustering(e.target.value as BarnepensjonSoeskenjusteringGrunn)}
          >
            <option value={undefined}>Velg grunn</option>
            {SOESKENJUSTERING_GRUNNER.map((grunn) => (
              <option key={grunn} value={grunn}>
                {tekstSoeskenjustering[grunn]}
              </option>
            ))}
          </Select>
          <Button type="submit" loading={isPending(lagrestatus)}>
            Lagre
          </Button>
          {feilmelding ? <ApiErrorAlert>{feilmelding}</ApiErrorAlert> : null}
        </form>
      ) : (
        <BodyShort>
          Grunn for søskenjustering:{' '}
          <strong>{valgtSoeskenjustering ? tekstSoeskenjustering[valgtSoeskenjustering] : 'Ikke angitt'}</strong>
        </BodyShort>
      )}
    </MarginTop>
  )
}

const MarginTop = styled.div`
  margin-top: 3em;
`
