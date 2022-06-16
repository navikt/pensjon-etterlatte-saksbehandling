import { format } from 'date-fns'

export const Soeknadsdato = ({ mottattDato }: { mottattDato: string }) => {
  return (
    <div>
      <strong>Søknad mottatt: </strong>
      {format(new Date(mottattDato), 'dd.MM.yyyy')}
    </div>
  )
}
