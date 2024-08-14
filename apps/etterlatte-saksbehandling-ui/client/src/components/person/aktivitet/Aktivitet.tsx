import { isSuccess, Result } from '~shared/api/apiUtils'
import { SakMedBehandlinger } from '~components/person/typer'
import React, { ReactNode, useEffect, useState } from 'react'
import { Box, Heading } from '@navikt/ds-react'
import { AktivitetspliktVurderingVisning } from '~components/behandling/aktivitetsplikt/AktivitetspliktVurderingVisning'
import { IAktivitetspliktVurdering } from '~shared/types/Aktivitetsplikt'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAktivitspliktVurderingForSak } from '~shared/api/aktivitetsplikt'

export const Aktivitet = ({ sakResult }: { sakResult: Result<SakMedBehandlinger> }): ReactNode => {
  const [, hent] = useApiCall(hentAktivitspliktVurderingForSak)
  const [vurdering, setVurdering] = useState<IAktivitetspliktVurdering>()

  useEffect(() => {
    if (isSuccess(sakResult)) {
      console.log(sakResult.data)
      hent({ sakId: sakResult.data.sak.id }, (aktivitetspliktVurdering) => {
        setVurdering(aktivitetspliktVurdering)
      })
    }
  }, [])

  return (
    <Box padding="8">
      <Heading size="medium" spacing>
        Aktivitet
      </Heading>

      <Heading size="small">Nyeste aktivitet</Heading>

      <AktivitetspliktVurderingVisning vurdering={vurdering} visForm={() => {}} erRedigerbar={false} />
    </Box>
  )
}
