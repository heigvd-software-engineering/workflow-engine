import { AppBar, Box, Container, Toolbar, lighten, styled, useTheme } from "@mui/material"
import { ReactNode, useEffect } from "react"
import { Link } from "react-router-dom";

type Props = {
  children: ReactNode,
  farEnd?: ReactNode
}

const AlwaysWhiteLink = styled(Link)`
  color: white;
`;
const NavbarLink = styled(AlwaysWhiteLink)`
  text-decoration: none;
  margin-right: 10px;
`
const StyledMainLink = styled(NavbarLink)`
  font-weight: bold;
`;

export default function Layout(props: Props) {
  const theme = useTheme();
  const baseColor = theme.palette.background.paper;
  const thumbColor = lighten(baseColor, 10 * 0.026);
  const scrollbarColor = lighten(baseColor, 2 * 0.026);

  useEffect(() =>{
    document.body.style.setProperty("--scrollbarColor", thumbColor + " " + scrollbarColor);
  }, [thumbColor, scrollbarColor])

  return (
    <Box sx={{padding: 1, display: "flex", flexFlow: "column", height: "100%"}}>
      <AppBar position="static" sx={{flex: "0 1 auto"}}>
        <Container maxWidth={false}>
          <Toolbar disableGutters>
            <StyledMainLink to="/">Workflow engine</StyledMainLink>
            <NavbarLink to="/documentation">Documentation</NavbarLink>
            <Box sx={{flexGrow: 1}}></Box>
            { 
              props.farEnd ? 
                props.farEnd 
                :
                <AlwaysWhiteLink to="/workflows">Workflows</AlwaysWhiteLink>
            }
          </Toolbar>
        </Container>
      </AppBar>
      <Box sx={{flex: "1 1 auto"}}>
        {props.children}
      </Box>
    </Box>
  )
}