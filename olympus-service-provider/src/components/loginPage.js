import { Box, Button, Typography } from "@material-ui/core";
import DirectionsIcon from '@material-ui/icons/Directions';
import React from "react";
import { userManager,userManagerOlympus } from "../utils/userManager";

class LoginPage extends React.Component {

  onLoginClick(event) {
    event.preventDefault();
    userManager.signinRedirect();
  }
  onLoginClickOlympus(event) {
    event.preventDefault();
    userManagerOlympus.signinRedirect();
  }

  render() {
    return (
      <Box style={styles.root}>
      <Typography variant="h5" align="center">
        How do you wish to authorize?
      </Typography>
        <Button
          size="large"
          variant="outlined"
          startIcon={<DirectionsIcon />}
          onClick={this.onLoginClick}
          style={styles.button}
        >
          Log in with KeyCloak
        </Button>
        <Button
          size="large"
          variant="outlined"
          startIcon={<DirectionsIcon />}
          onClick={this.onLoginClickOlympus}
          style={styles.button}
        >
          Log in with Olympus
        </Button>
      </Box>
    );
  }
}

const styles = {
  root: {
    display: "flex",
    flexDirection: "column",
    justifyContent: "space-around",
    alignItems: "center",
    flexShrink: 1,
  },
  button: {
    marginTop: "20px",
  },
};

export default LoginPage;
