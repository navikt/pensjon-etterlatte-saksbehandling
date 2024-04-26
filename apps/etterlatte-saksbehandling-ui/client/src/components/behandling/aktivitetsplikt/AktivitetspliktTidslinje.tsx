import { Buildings2Icon, HatSchoolIcon, PencilIcon, PersonIcon, RulerIcon } from '@navikt/aksel-icons'
import { Alert, Timeline } from '@navikt/ds-react'
import { hentAktiviteter, slettAktivitet } from '~shared/api/aktivitetsplikt'
import { formaterDato, formaterDatoMedTidspunkt } from '~utils/formattering'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { addMonths, addYears } from 'date-fns'
import { useApiCall } from '~shared/hooks/useApiCall'
import React, { useEffect, useState } from 'react'
import { AktivitetspliktType, IAktivitet } from '~shared/types/Aktivitetsplikt'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { NyAktivitet } from '~components/behandling/aktivitetsplikt/NyAktivitet'
import styled from 'styled-components'
import { isPending } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'

export const AktivitetspliktTidslinje = (props: { behandling: IDetaljertBehandling; doedsdato: Date }) => {
  const { behandling, doedsdato } = props
  const [hentet, hent] = useApiCall(hentAktiviteter)
  const [slettet, slett] = useApiCall(slettAktivitet)
  const seksMndEtterDoedsfall = addMonths(doedsdato, 6)
  const tolvMndEtterDoedsfall = addMonths(doedsdato, 12)

  const [aktiviteter, setAktiviteter] = useState<IAktivitet[]>([])
  const [aktivitetsTypeProps, setAktivitetsTypeProps] = useState<AktivitetstypeProps[]>([])

  useEffect(() => {
    hent({ behandlingId: behandling.id }, (aktiviteter) => {
      oppdaterAktiviteter(aktiviteter)
    })
  }, [])

  const oppdaterAktiviteter = (aktiviteter: IAktivitet[]) => {
    setAktivitetsTypeProps([...new Set(aktiviteter.map((a) => a.type))].map(mapAktivitetstypeProps))
    setAktiviteter(aktiviteter)
  }

  const fjernAktivitet = (aktivitetId: string) => {
    slett({ behandlingId: behandling.id, aktivitetId: aktivitetId }, (aktiviteter) => {
      oppdaterAktiviteter(aktiviteter)
    })
  }

  return (
    <TidslinjeWrapper className="min-w-[800px]">
      {aktiviteter.length === 0 ? (
        <Alert variant="info" inline>
          Ingen aktiviteter er registrert.
        </Alert>
      ) : (
        <Timeline startDate={doedsdato} endDate={addYears(doedsdato, 3)}>
          <Timeline.Pin date={doedsdato}>
            <p>Dødsdato: {formaterDato(doedsdato)}</p>
          </Timeline.Pin>
          <Timeline.Pin date={new Date()}>
            <p>Dagens dato: {formaterDato(new Date())}</p>
          </Timeline.Pin>
          <Timeline.Pin date={seksMndEtterDoedsfall}>
            <p>6 måneder etter dødsfall: {formaterDato(seksMndEtterDoedsfall)}</p>
          </Timeline.Pin>
          <Timeline.Pin date={tolvMndEtterDoedsfall}>
            <p>12 måneder etter dødsfall: {formaterDato(tolvMndEtterDoedsfall)}</p>
          </Timeline.Pin>

          {aktivitetsTypeProps.map((props) => (
            <Timeline.Row key={props.type} label={props.beskrivelse}>
              {aktiviteter
                .filter((aktivitet) => aktivitet.type === props.type)
                .map((aktivitet, i) => (
                  <Timeline.Period
                    key={props.type + i}
                    start={new Date(aktivitet.fom)}
                    end={(aktivitet.tom && new Date(aktivitet.tom)) || addYears(doedsdato, 3)}
                    status={props.status}
                    icon={props.ikon}
                    statusLabel={props.beskrivelse}
                  >
                    <p>
                      <b>
                        Fra {formaterDato(new Date(aktivitet.fom))}{' '}
                        {aktivitet.tom && `til ${formaterDato(new Date(aktivitet.tom))}`}
                      </b>
                    </p>
                    <p>{aktivitet.beskrivelse}</p>
                    <p>
                      <i>
                        Lagt til {formaterDatoMedTidspunkt(new Date(aktivitet.opprettet.tidspunkt))} av{' '}
                        {aktivitet.opprettet.ident}
                      </i>
                      <br />
                      <i>
                        Sist endret {formaterDatoMedTidspunkt(new Date(aktivitet.endret.tidspunkt))} av{' '}
                        {aktivitet.endret.ident}
                      </i>
                    </p>
                    {isPending(slettet) ? (
                      <Spinner visible={true} variant="neutral" label="Sletter" margin="1em" />
                    ) : (
                      <SlettWrapper onClick={() => fjernAktivitet(aktivitet.id)}>Slett</SlettWrapper>
                    )}
                    {isFailureHandler({
                      apiResult: slettet,
                      errorMessage: 'En feil har oppstått',
                    })}
                  </Timeline.Period>
                ))}
            </Timeline.Row>
          ))}
        </Timeline>
      )}

      <NyAktivitet behandling={behandling} oppdaterAktiviteter={oppdaterAktiviteter} />

      {isFailureHandler({
        errorMessage: 'En feil oppsto ved henting av aktiviteter',
        apiResult: hentet,
      })}
    </TidslinjeWrapper>
  )
}

interface AktivitetstypeProps {
  type: AktivitetspliktType
  beskrivelse: string
  ikon: JSX.Element
  status: 'success' | 'warning' | 'danger' | 'info' | 'neutral'
}

export const mapAktivitetstypeProps = (type: AktivitetspliktType): AktivitetstypeProps => {
  switch (type) {
    case AktivitetspliktType.ARBEIDSTAKER:
      return {
        type: AktivitetspliktType.ARBEIDSTAKER,
        beskrivelse: 'Arbeidstaker',
        ikon: <PersonIcon aria-hidden />,
        status: 'success',
      }
    case AktivitetspliktType.SELVSTENDIG_NAERINGSDRIVENDE:
      return {
        type: AktivitetspliktType.SELVSTENDIG_NAERINGSDRIVENDE,
        beskrivelse: 'Selvstendig næringsdrivende',
        ikon: <RulerIcon aria-hidden />,
        status: 'info',
      }
    case AktivitetspliktType.ETABLERER_VIRKSOMHET:
      return {
        type: AktivitetspliktType.ETABLERER_VIRKSOMHET,
        beskrivelse: 'Etablerer virksomhet',
        ikon: <Buildings2Icon aria-hidden />,
        status: 'danger',
      }
    case AktivitetspliktType.ARBEIDSSOEKER:
      return {
        type: AktivitetspliktType.ARBEIDSSOEKER,
        beskrivelse: 'Arbeidssøker',
        ikon: <PencilIcon aria-hidden />,
        status: 'warning',
      }
    case AktivitetspliktType.UTDANNING:
      return {
        type: AktivitetspliktType.UTDANNING,
        beskrivelse: 'Utdanning',
        ikon: <HatSchoolIcon aria-hidden />,
        status: 'neutral',
      }
  }
}

const TidslinjeWrapper = styled.div`
  margin-bottom: 50px;
`

const SlettWrapper = styled.div`
  display: inline-flex;
  float: left;
  cursor: pointer;
  color: #0067c5;

  &:hover {
    text-decoration-line: underline;
  }
`
