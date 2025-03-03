import React, { useEffect, useState } from 'react'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { Button, HStack } from '@navikt/ds-react'
import { PlusIcon } from '@navikt/aksel-icons'
import { IAktivitetHendelse, IAktivitetPeriode } from '~shared/types/Aktivitetsplikt'
import { NyHendelse } from '~components/behandling/aktivitetsplikt/NyHendelse'
import { NyAktivitet } from '~components/behandling/aktivitetsplikt/NyAktivitet'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'

enum Skjemavisning {
  INGEN = 'INGEN',
  AKTIVITET = 'AKTIVITET',
  HENDELSE = 'HENDELSE',
}

export const AktivitetOgHendelse = ({
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
  behandling?: IDetaljertBehandling
}) => {
  const [visForm, setVisForm] = useState<Skjemavisning>(Skjemavisning.INGEN)
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const redigerbar = behandling
    ? behandlingErRedigerbar(behandling.status, behandling.sakEnhetId, innloggetSaksbehandler.skriveEnheter)
    : true

  useEffect(() => {
    if (redigerAktivitet) {
      setVisForm(Skjemavisning.AKTIVITET)
    }
    if (redigerHendelse) {
      setVisForm(Skjemavisning.HENDELSE)
    }
  }, [redigerAktivitet, redigerHendelse])

  function avbryt() {
    avbrytRedigering()
    setVisForm(Skjemavisning.INGEN)
  }

  function oppdaterHendelser(hendelser: IAktivitetHendelse[]) {
    setVisForm(Skjemavisning.INGEN)
    setHendelser(hendelser)
  }

  function oppdaterAktiviteterOgSkjema(aktiviteter: IAktivitetPeriode[]) {
    setVisForm(Skjemavisning.INGEN)
    oppdaterAktiviteter(aktiviteter)
  }

  return (
    <>
      {visForm === Skjemavisning.AKTIVITET && (
        <NyAktivitet
          oppdaterAktiviteter={oppdaterAktiviteterOgSkjema}
          aktivitetTilRedigering={redigerAktivitet}
          sakId={sakId}
          avbryt={avbryt}
          behandling={behandling}
        />
      )}
      {visForm === Skjemavisning.HENDELSE && (
        <NyHendelse
          redigerHendelse={redigerHendelse}
          sakId={sakId}
          avbryt={avbryt}
          oppdaterHendelser={oppdaterHendelser}
          behandling={behandling}
        />
      )}
      {visForm === Skjemavisning.INGEN && redigerbar && (
        <HStack gap="4">
          <Button
            size="small"
            variant="secondary"
            icon={<PlusIcon aria-hidden fontSize="1.5rem" />}
            onClick={(e) => {
              e.preventDefault()
              setVisForm(Skjemavisning.AKTIVITET)
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
              setVisForm(Skjemavisning.HENDELSE)
            }}
          >
            Legg til Hendelse
          </Button>
        </HStack>
      )}
    </>
  )
}
