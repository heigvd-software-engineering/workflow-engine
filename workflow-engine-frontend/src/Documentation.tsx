import { Box, Button, Paper, styled } from "@mui/material";
import { Link, useParams } from "react-router-dom";
import { ReactNode, useEffect, useState } from "react";
import { ClassList, DocResponse, DocumentClass } from "./types/Types";
import Layout from "./Layout";
import { ArrowBack } from "@mui/icons-material";

const CustomLink = styled(Link)`
  color: white;
  text-decoration: none;
`;

function DocumentationList() {
  const [status, setStatus] = useState<string | undefined>("Loading");
  const [list, setList] = useState<ClassList>();

  useEffect(() => {
    fetch(`/api/documentation`)
      .then(r => r.json())
      .then((data: DocResponse) => {
        if (data.type == "error") {
          setStatus(data.value);
        } else if (data.type == "list") {
          setList(data.value);
          setStatus(undefined);
        }
      });
  }, []);

  if (status) {
    return <p>{status}</p>;
  }

  if (list == undefined) {
    return <p>No data</p>;
  }

  return (
    <>
      <h2>Documented classes</h2>
      <ul>
        {list.map(elem => <li key={elem}>
          <CustomLink to={`/documentation/${elem}`}>{elem}</CustomLink>
        </li>)}
      </ul>
    </>
  )
}

function DocumentationClass(props: { type: string }) {
  const [status, setStatus] = useState<string | undefined>("Loading");
  const [classValue, setClassValue] = useState<DocumentClass>();

  const returnToClassListElem = (children: ReactNode) => {
    return (
      <Box>
        <Box sx={{marginTop: 1, alignItems: "center"}}>
          <Link to="/documentation">
            <Button>
              <ArrowBack fontSize="small" sx={{marginRight: 0.5}} /> Back
            </Button>
          </Link>
        </Box>
        {children}
      </Box>
    )
  }

  useEffect(() => {
    fetch(`/api/documentation/${props.type}`)
      .then(r => r.json())
      .then((data: DocResponse) => {
        if (data.type == "error") {
          console.log(data);
          setStatus(data.value);
        } else if (data.type == "class") {
          setClassValue(data.value);
          setStatus(undefined);
        }
      });
  }, [props.type]);

  if (status) {
    return returnToClassListElem(<p>{status}</p>);
  }

  if (classValue == undefined) {
    return returnToClassListElem(<p>No data</p>);
  }

  return returnToClassListElem(
    <Paper sx={{marginTop: 1, padding: 1}}>
      <Box fontSize="20px" fontWeight="bold">{classValue.name}</Box>
      <pre>{classValue.comment}</pre>
      {
        classValue.methods.map(method => 
          <Paper elevation={2} sx={{marginTop: 1, padding: 1}} key={method.name}>
            <Box fontSize="18px" fontWeight="bold">{method.type} {method.name}({method.params.map(p => `${p.type} ${p.name}`).join(", ")})</Box>
            <pre>{method.comment}</pre>
          </Paper>
        )
      }
    </Paper>
  )
}

export function Documentation() {
  const { type } = useParams();

  return (
    <Layout>
      {
        type ? 
          <DocumentationClass type={type} />
          :
          <DocumentationList />
      }
    </Layout>
  )
}