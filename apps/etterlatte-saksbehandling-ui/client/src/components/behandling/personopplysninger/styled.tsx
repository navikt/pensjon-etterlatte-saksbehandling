import styled from 'styled-components'

export const TableWrapper = styled.div`
  display: flex;
  flex-wrap: wrap;
  max-width: 600px;

  .table {
    max-width: 500px;
    margin-bottom: 1em;

    .text {
      float: right;
    }

    .tableCell {
      max-width: 100px;
    }
  }
  .icon {
    margin-top: 15px;
  }
`

export const ContentHeader = styled.div`
  padding: 5em;
`

export const InfoWrapper = styled.div`
  display: flex;
  justify-content: space-between;
  max-width: 200px;
`

export const DetailWrapper = styled.div`
  margin-bottom: 50px;
`

export const ContentWrapper = styled.div`
  * {
    margin-bottom: 30px;
  }
`

export const TextButtonWrapper = styled.div`  
margin-left: auto;
margin-right: 5em;
margin-bottom: 0;

.textButton{
  margin-bottom: 0;
  display: inline-flex;
  justify-content: space-between;
  text-decoration: underline;
  color: #0067c5;
  :hover {
    cursor: pointer;
  }
  .dropdownIcon {
    margin-bottom: 0;
    margin-left: 0.5em;
    margin-top 0.1em;
 
  }
}
`
