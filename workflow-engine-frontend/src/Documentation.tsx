import { Box, Paper, styled } from "@mui/material";
import { Link, useParams } from "react-router-dom";
import { useEffect, useState } from "react";
import { ClassList, DocResponse, DocumentClass } from "./types/Types";
import Layout from "./Layout";

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
    return <Box>{status}</Box>;
  }

  return (
    <>
      <h2>Documented classes</h2>
      <ul>
        {list?.map(elem => <li key={elem}>
          <CustomLink to={`/documentation/${elem}`}>{elem}</CustomLink>
        </li>)}
      </ul>
    </>
  )
}

function DocumentationClass(props: { type: string }) {
  const [status, setStatus] = useState<string | undefined>("Loading");
  const [classValue, setClassValue] = useState<DocumentClass>();

  useEffect(() => {
    fetch(`/api/documentation/${props.type}`)
      .then(r => r.json())
      .then((data: DocResponse) => {
        if (data.type == "error") {
          setStatus(data.value);
        } else if (data.type == "class") {
          setClassValue(data.value);
          setStatus(undefined);
        }
      });
  }, [props.type]);

  if (status) {
    return <Box>{status}</Box>;
  }

  if (classValue == undefined) {
    return <Box>No data</Box>;
  }

  return (
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