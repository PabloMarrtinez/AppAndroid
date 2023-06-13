import React from "react";
import { connect } from "react-redux";
import { push } from "react-router-redux";
import FrontPage from "./frontPage";
import { Button, Container, Typography } from "@material-ui/core";

import { userManager, userManagerOlympus } from "../utils/userManager";

class MainPage extends React.Component {
  render() {
    const { user } = this.props;
    console.log("start user");
    console.log(user);
    console.log("end user");
    // Fetch user profile and check if user is > 18 years
    var birthdate = user.profile.birthdate;
    var today = new Date();
    console.log(today.getYear());
    today.setYear(today.getYear() + 1900 - 18);
    var over18 = new Date(birthdate) < today;

    // Failed to validate age check
    if (!over18) {
      alert("under 18, sending back to front");
      return <FrontPage />;
    }

    return (
      <Container style={styles.root}>
        <Container style={styles.title}>
          <Typography variant="h4" align="center">
            Welcome, {user ? user.profile.name : "Mister Unknown"}!
          </Typography>
          <Typography variant="subtitle1" align="center" style={styles.subtitle}>You're over 18!</Typography>
          <Typography variant="subtitle1" align="center">A reservation has been made in your name!</Typography>
        </Container>
        <Container style={styles.buttonBox}>
          <Button
            size="medium"
            variant="outlined"
            onClick={(event) => {
              event.preventDefault();
              userManager.removeUser();
              userManagerOlympus.removeUser();
              this.props.dispatch(push("/"));
            }}
          >
            Logout
          </Button>
        </Container>
      </Container>
    );
  }
}

const styles = {
  root: {
    display: "flex",
    flexDirection: "column",
  },
  title: {
    flex: "1 0 auto",
  },
  list: {
    listStyle: "none",
  },
  li: {
    display: "flex",
  },
  buttonBox: {
    paddingTop: "10px",
    marginTop: "auto",
    marginButtom: "auto",
    width: "150px",
  },
  subtitle: {
    paddingTop: "10px",
  },
};

function mapStateToProps(state) {
  return {
    user: state.oidc.user,
  };
}

export default connect(mapStateToProps)(MainPage);
