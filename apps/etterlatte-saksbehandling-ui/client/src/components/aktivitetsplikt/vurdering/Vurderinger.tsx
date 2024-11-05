import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'
import React, { useEffect } from 'react'
import { erOppgaveRedigerbar } from '~shared/types/oppgave'
import { Heading } from '@navikt/ds-react'
import { BrevAktivitetsplikt } from '~components/person/aktivitet/vurderingAvAktivitetsplikt/BrevAktivitetsplikt'

import { AktivitetsgradIOppgave } from '~components/aktivitetsplikt/vurdering/aktivitetsgrad/AktivitetsgradIOppgave'
import { LeggTilUnntak } from '~components/aktivitetsplikt/vurdering/unntak/LeggTilUnntak'
import { LeggTilNyVurdering } from '~components/aktivitetsplikt/vurdering/aktivitetsgrad/LeggTilNyVurdering'
import { UnntakIOppgave } from '~components/aktivitetsplikt/vurdering/unntak/UnntakIOppgave'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentFamilieOpplysninger } from '~shared/api/pdltjenester'
import { mapSuccess } from '~shared/api/apiUtils'

export function Vurderinger() {
  const { oppgave, sak } = useAktivitetspliktOppgaveVurdering()

  const [familieOpplysningerResult, hentOpplysninger] = useApiCall(hentFamilieOpplysninger)
  useEffect(() => {
    hentOpplysninger({
      ident: sak.ident,
      sakType: oppgave.sakType,
    })
  }, [])

  const doedsdato =
    mapSuccess(
      familieOpplysningerResult,
      (opplysninger) => opplysninger.avdoede?.find((avdoed) => avdoed)?.doedsdato
    ) ?? undefined

  const oppgaveErRedigerbar = erOppgaveRedigerbar(oppgave.status)

  return (
    <>
      <Heading size="small">Vurderinger i oppgave</Heading>
      <AktivitetsgradIOppgave doedsdato={doedsdato} />
      {oppgaveErRedigerbar && <LeggTilNyVurdering doedsdato={doedsdato} />}
      <UnntakIOppgave />
      {oppgaveErRedigerbar && <LeggTilUnntak />}
      <BrevAktivitetsplikt />
    </>
  )
}
