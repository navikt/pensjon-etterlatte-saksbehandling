import styled from 'styled-components'

export const Text = styled.div`
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 5px;
`
export const BeslutningWrapper = styled.div`
  .textareaWrapper {
    margin-top: 2em;
    margin-bottom: 1em;
  }
`

export const RadioGroupWrapper = styled.div`
  margin-top: 0.5em;
  margin-bottom: 1em;

  .flex {
    display: flex;
    justify-content: space-between;
  }
`

export const Wrapper = styled.div<{ innvilget: boolean }>`
  margin: 20px 8px 0px 8px;
  padding: 1em;
  border: 1px solid #c7c0c0;
  border-left: 5px solid ${(props) => (props.innvilget ? '#007C2E' : '#881d0c')};
  border-radius: 3px;

  .flex {
    margin-top: 1em;
    display: flex;
    justify-content: space-between;
  }

  .info {
    margin-top: 1em;

    margin-bottom: 1em;
  }
`

export const UnderOverskrift = styled.div<{ innvilget: boolean }>`
  font-style: normal;
  font-weight: 600;
  font-size: 16px;
  color: ${(props) => (props.innvilget ? '#007C2E' : '#881d0c')};
`

export const Overskrift = styled.div`
  font-weight: 600;
  font-size: 20px;
  color: #3e3832;
`

export const Info = styled.div`
  font-weight: 600;
  font-size: 14px;
  width: 115px;
`

export const Tekst = styled.div`
  font-family: 'Source Sans Pro';
  font-style: normal;
  font-weight: 400;
  font-size: 14px;
  line-height: 20px;

  color: #3e3832;
`
