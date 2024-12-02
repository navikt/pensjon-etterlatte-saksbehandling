import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import React, { useEffect, useState } from 'react'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { Button, HStack } from '@navikt/ds-react'
import { PlusIcon } from '@navikt/aksel-icons'
import { IAktivitetHendelse, IAktivitetPeriode } from '~shared/types/Aktivitetsplikt'
import { NyHendelse } from '~components/behandling/aktivitetsplikt/NyHendelse'
import { NyAktivitet } from '~components/behandling/aktivitetsplikt/NyAktivitet'

export const NyAktivitetHendelse = ({
  oppdaterAktiviteter,
  redigerAktivitet,
  redigerHendelse,
  setHendelser,
  avbrytRedigering,
  sakId,
  behandling = undefined,
}: {
  oppdaterAktiviteter: (aktiviteter: IAktivitetPeriode[]) => void
  redigerAktivitet: IAktivitetPeriode | undefined
  redigerHendelse: IAktivitetHendelse | undefined
  setHendelser: (aktiviteter: IAktivitetHendelse[]) => void
  avbrytRedigering: () => void
  sakId: number
  behandling?: IBehandlingReducer
}) => {
  const [visForm, setVisForm] = useState<false | 'AKTIVITET' | 'HENDELSE'>(false)
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const redigerbar = behandling
    ? behandlingErRedigerbar(behandling.status, behandling.sakEnhetId, innloggetSaksbehandler.skriveEnheter)
    : true

  useEffect(() => {
    if (redigerAktivitet) {
      setVisForm('AKTIVITET')
    }
    if (redigerHendelse) {
      setVisForm('HENDELSE')
    }
  }, [redigerAktivitet, redigerHendelse])

  function avbryt() {
    avbrytRedigering()
    setVisForm(false)
  }

  function oppdaterHendelser(hendelser: IAktivitetHendelse[]) {
    setVisForm(false)
    setHendelser(hendelser)
  }

  function oppdaterAktiviteterOgSkjema(aktiviteter: IAktivitetPeriode[]) {
    setVisForm(false)
    oppdaterAktiviteter(aktiviteter)
  }

  return (
    <>
      {visForm === 'AKTIVITET' && (
        <NyAktivitet
          oppdaterAktiviteter={oppdaterAktiviteterOgSkjema}
          redigerAktivitet={redigerAktivitet}
          sakId={sakId}
          avbryt={avbryt}
          behandling={behandling}
        ></NyAktivitet>
      )}
      {visForm === 'HENDELSE' && (
        <NyHendelse
          redigerHendelse={redigerHendelse}
          sakId={sakId}
          avbryt={avbryt}
          oppdaterHendelser={oppdaterHendelser}
          behandling={behandling}
        />
      )}
      {!visForm && redigerbar && (
        <HStack gap="4">
          <Button
            size="small"
            variant="secondary"
            icon={<PlusIcon aria-hidden fontSize="1.5rem" />}
            onClick={(e) => {
              e.preventDefault()
              setVisForm('AKTIVITET')
            }}
          >
            Legg til aktivitet
          </Button>
          <Button
            size="small"
            variant="secondary"
            icon={<PlusIcon aria-hidden fontSize="1.5rem" />}
            onClick={(e) => {
              e.preventDefault()
              setVisForm('HENDELSE')
            }}
          >
            Legg til Hendelse
          </Button>
        </HStack>
      )}
    </>
  )
}
